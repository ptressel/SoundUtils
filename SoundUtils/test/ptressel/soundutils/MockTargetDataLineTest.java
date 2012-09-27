package ptressel.soundutils;

import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the test helper MockTargetDataLine.
 * 
 * @author Patricia Tressel
 */
public class MockTargetDataLineTest {

  // Formats with one and two byte sample sizes -- other fields don't matter.
  // CAUTION: If we support multiple channels, need to distinguish frame rate
  // and size from sample rate and size.
  private float frequency = 400.0F;
  private float sampleRate = 8 * frequency;
  private boolean signed = true;
  private boolean bigEndian = true;
  private int channels = 1;
  private int sampleBytes1 = 1;
  private AudioFormat format1 =
      new AudioFormat( sampleRate, sampleBytes1 * Byte.SIZE,
                       channels, signed, bigEndian );
  private int sampleBytes2 = 2;
  private AudioFormat format2 =
      new AudioFormat( sampleRate, sampleBytes2 * Byte.SIZE,
                       channels, signed, bigEndian );

  /** LineListener class that records the last event it receives. */
  private class TestLineListener implements LineListener {
    private LineEvent lastEvent = null;
    public void update( LineEvent event ) {
      lastEvent = event;
    }
  }

  /** Helper that hides the open() exceptions. */
  private void openMock( MockTargetDataLine mock, AudioFormat format ) {
    try {
      mock.open( format );
    } catch (LineUnavailableException ex) {
      fail( "Line should be available." );
    } catch (IllegalStateException ex) {
      fail( "Line should not yet be open." );
    }
  }

  /** Helper that gets mock ready to read from. */
  private void readyMock( MockTargetDataLine mock, AudioFormat format ) {
    mock.setLineAvailable( true );
    openMock( mock, format );
    mock.start();
  }

  @Test
  public void testOpenClose() {
    // TODO when state-change events are implemented in MockTargetDataLine,
    // check that OPEN event is received.
    System.out.println( "MockTargetDataLineTest#testOpenClose" );

    byte[] samples = { 0, 1, 2 };
    boolean loop = false;
    MockTargetDataLine mock
        = new MockTargetDataLine( samples, loop, sampleBytes1, format1, null );

    // Check that line is marked open after open().
    assertFalse( mock.isOpen() );
    mock.setLineAvailable( true );
    try {
      mock.open( format1 );
    } catch (LineUnavailableException ex) {
      fail( "Line should be available." );
    } catch (IllegalStateException ex) {
      fail( "Line should not yet be open." );
    }
    assertTrue( mock.isOpen() );

    // Should get exception if we try to open it again.
    boolean gotIllegalStateException = false;
    try {
      mock.open( format1 );
    } catch ( LineUnavailableException ex) {
      fail( "Line should be available." );
    } catch (IllegalStateException ex) {
      gotIllegalStateException = true;
    }
    assertTrue( gotIllegalStateException );

    // close() should make open state go away.
    mock.close();
    assertFalse( mock.isOpen() );
  }

  @Test
  public void testRead() {
    System.out.println( "MockTargetDataLineTest#testRead" );
    byte[] output = new byte[ 10 ];

    // First test obtaining samples from a supplied array with looping off.
    byte[] samples = { 0, 1, 2 };
    boolean loop = false;
    MockTargetDataLine mockWithArray =
        new MockTargetDataLine( samples, loop, sampleBytes1, format1, null );
    mockWithArray.setLineAvailable( true );

    // Before open(), a read should return nothing.
    assertEquals( 0, mockWithArray.read( output, 0, 1 ) );
    openMock( mockWithArray, format1 );
    // Before start(), a read should return nothing.
    assertEquals( 0, mockWithArray.read( output, 0, 1 ) );
    mockWithArray.start();

    // Check that with looping off, we only get the number of bytes
    // in the supplied array.
    Arrays.fill( output, (byte)10 );
    assertEquals(
            samples.length, mockWithArray.read( output, 0, output.length ) );
    byte[] nonloopOutput1 = { 0, 1, 2, 10, 10, 10, 10, 10, 10, 10 };
    assertArrayEquals( nonloopOutput1, output );
    Arrays.fill( output, (byte)10 );
    assertEquals( 0, mockWithArray.read( output, 0, output.length ) );
    byte[] nonloopOutput2 = { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
    assertArrayEquals( nonloopOutput2, output );

    // This time, allow looping.
    loop = true;
    mockWithArray =
        new MockTargetDataLine( samples, loop, sampleBytes1, format1, null );
    mockWithArray.setLineAvailable( true );
    readyMock( mockWithArray, format1 );

    // With looping on, we get as many bytes as we want.
    Arrays.fill( output, (byte)10 );
    assertEquals(
        output.length, mockWithArray.read( output, 0, output.length ) );
    byte[] firstLoopOutput = { 0, 1, 2, 0, 1, 2, 0, 1, 2, 0 };
    assertArrayEquals( firstLoopOutput, output );
    assertEquals(
        output.length, mockWithArray.read( output, 0, output.length ) );
    byte[] secondLoopOutput = { 1, 2, 0, 1, 2, 0, 1, 2, 0, 1 };
    assertArrayEquals( secondLoopOutput, output );

    // Use a nonzero offset.
    Arrays.fill( output, (byte)10 );
    assertEquals( 4, mockWithArray.read( output, 2, 4 ) );
    // After the above reads, the next byte we'll read is samples[2] = 2.
    byte[] offsetOutput = { 10, 10, 2, 0, 1, 2, 10, 10, 10, 10 };
    assertArrayEquals( offsetOutput, output );

    // Attempt to overflow output array.
    boolean gotIllegalArgumentException = false;
    try {
      mockWithArray.read( output, output.length - 1, 2 );
    } catch( IllegalArgumentException e ) {
      gotIllegalArgumentException = true;
    }
    assertTrue( gotIllegalArgumentException );

    // TODO Read from an array with sample size 2.  Use odd-length sample array
    // to show it doesn't matter.

    // Next read from a Wave.  Make one Wave for the MockTargetDataLine, and an
    // identical Wave from which we can get samples to compare against.  Also
    // use two byte samples, so we can test rejection of a bad number of
    // requested bytes.
    Wave waveForMock =
        new PureWave( frequency, 1, 0.0, sampleRate, sampleBytes2,
                      signed, bigEndian );
    Wave referenceWave =
        new PureWave( frequency, 1, 0.0, sampleRate, sampleBytes2,
                      signed, bigEndian );
    MockTargetDataLine mockWithWave =
        new MockTargetDataLine( waveForMock, sampleBytes2, format2, null );
    readyMock( mockWithWave, format2 );

    // First ask to fill the output array.
    byte[] referenceOutput1 = new byte[ output.length ];
    referenceWave.insertNextBytes(
        referenceOutput1, 0, referenceOutput1.length );
    Arrays.fill( output, (byte)10 );
    assertEquals(
        output.length, mockWithWave.read( output, 0, output.length ) );
    assertArrayEquals( referenceOutput1, output );

    // Now use an output offset.
    int off = 2;
    int len = 6;
    byte[] referenceOutput2 = new byte[ output.length ];
    Arrays.fill( referenceOutput2, (byte)10 );
    referenceWave.insertNextBytes( referenceOutput2, off, len );
    Arrays.fill( output, (byte)10 );
    assertEquals( len, mockWithWave.read( output, off, len ) );
    assertArrayEquals( referenceOutput2, output );

    // With a sample size of 2 bytes, we can't ask for an odd number of bytes.
    gotIllegalArgumentException = false;
    try {
      mockWithWave.read( output, 0, 1 );
    } catch( IllegalArgumentException e ) {
      gotIllegalArgumentException = true;
    }
    assertTrue( gotIllegalArgumentException );
  }


  @Test
  public void testPosition() {
    System.out.println( "MockTargetDataLine#*Position" );
    byte[] output = new byte[ 10 ];

    // Read more than once, then check position.  First use a sample array.
    byte[] samples = { 1, 2, 3, 4 };
    MockTargetDataLine mockWithArray =
        new MockTargetDataLine( samples, true, sampleBytes1, format1, null );
    readyMock( mockWithArray, format1 );
    mockWithArray.read( output, 0, 1 );
    mockWithArray.read( output, 0, 8 );
    // # samples read is same as # bytes read for sample size of 1.
    assertEquals( 9, mockWithArray.getLongFramePosition() );
    assertEquals( 9, mockWithArray.getFramePosition() );
    assertEquals( 9 * 1000 / (long) sampleRate,
                  mockWithArray.getMicrosecondPosition() );

    // Same with a Wave.
    Wave waveForMock =
        new PureWave( frequency, 1, 0.0, sampleRate, sampleBytes2,
                      signed, bigEndian );
    MockTargetDataLine mockWithWave =
        new MockTargetDataLine( waveForMock, sampleBytes2, format2, null );
    readyMock( mockWithWave, format2 );
    mockWithWave.read( output, 0, 2 );
    mockWithWave.read( output, 0, 4 );
    int numSamplesRead = 6 / sampleBytes2;
    assertEquals( numSamplesRead, mockWithWave.getLongFramePosition() );
    assertEquals( numSamplesRead, mockWithWave.getFramePosition() );
    assertEquals( numSamplesRead * 1000 / (long) sampleRate,
                  mockWithWave.getMicrosecondPosition() );

    // Test overflow of the int getFramePosition value when the number of
    // frames read goes over Integer.MAX_VALUE+1.  We don't actually read
    // that many -- MockTargetDataLine has a test helper that allows setting
    // the number of frames read.  Set the position to two frames after
    // overflow.
    mockWithWave.setLongFramePosition( (long)Integer.MAX_VALUE + 3 );
    // Modulo Integer.MAX_VALUE+1 samples, what's left should be 2 samples.
    assertEquals( 2, mockWithWave.getFramePosition() );
  }

  @Test
  public void testGetLevel() {
    System.out.println( "MockTargetDataLine#getLevel" );
    byte[] output = new byte[ 10 ];
    


    // Need to read from a MockTargetDataLine before calling getLevel in order
    // for it to have a level to tell us about.  First read from an array-based
    // mock, with 1-byte sound.
    byte[] samples =
        { Byte.MAX_VALUE, Byte.MAX_VALUE/2, Byte.MIN_VALUE };
    MockTargetDataLine mockWithArray =
        new MockTargetDataLine( samples, true, sampleBytes1, format1, null );
    readyMock( mockWithArray, format1 );
    // The float comparison errors between adjacent discrete levels for a 1-byte
    // format are on the order of 1 / Byte.MAX_VALUE.
    float epsilon = 1 / (float) Byte.MAX_VALUE;
    System.out.println( "testGetLevel 1-byte epsilon = " + epsilon );
    // Before any read, mock should report zero.
    assertEquals( 0.0, mockWithArray.getLevel(), epsilon );
    mockWithArray.read( output, 0, 2 );
    // Last sample should be Byte.MAX_VALUE/2 so level should be a little
    // less than half.
    float expectedLevel =
        (float) ( 0.5 * Byte.MAX_VALUE / Math.abs( Byte.MIN_VALUE ) );
    assertEquals( expectedLevel, mockWithArray.getLevel(), epsilon );
    // Read the negative sample.  Level should be positive.
    mockWithArray.read( output, 0, 1 );
    assertEquals( 1.0, mockWithArray.getLevel(), epsilon );

    // Read from a Wave-based mock with 2-byte sound.
    Wave waveForMock =
        new PureWave( frequency, Short.MAX_VALUE, 0.0, sampleRate,
                      sampleBytes2, signed, bigEndian );
    MockTargetDataLine mockWithWave =
        new MockTargetDataLine( waveForMock, sampleBytes2, format2, null );
    readyMock( mockWithWave, format2 );
    mockWithWave.read( output, 0, 6 );
    epsilon = 1 / (float) Short.MAX_VALUE;
    System.out.println( "testGetLevel 2-byte epsilon = " + epsilon );
    expectedLevel = Math.abs( output[5] ) / Math.abs( Short.MIN_VALUE );
    assertEquals( expectedLevel, mockWithWave.getLevel(), epsilon );
  }

  private boolean checkEvent(
      long framePosition, LineEvent.Type type, MockTargetDataLine mock,
      TestLineListener listener1, TestLineListener listener2 ) {
    assertNotNull( listener1.lastEvent );
    assertNotNull( listener2.lastEvent );
    assertEquals( listener1.lastEvent, listener2.lastEvent );
    assertEquals( framePosition, listener1.lastEvent.getFramePosition() );
    assertEquals( mock, listener1.lastEvent.getLine() );
    assertEquals( type, listener1.lastEvent.getType() );
    return true;
  }

  @Test
  public void testEvents() {
    System.out.println( "MockTargetDataLine events" );
    byte[] samples = { 0, 1, 2 };
    boolean loop = true;
    byte[] output = new byte[ 10 ];
    MockTargetDataLine mock
        = new MockTargetDataLine( samples, loop, sampleBytes1, format1, null );

    // Give the mock multiple listeners.
    TestLineListener listener1 = new TestLineListener();
    TestLineListener listener2 = new TestLineListener();
    mock.addLineListener( listener1 );
    mock.addLineListener( listener2 );

    openMock( mock, format1 );
    checkEvent( 0, LineEvent.Type.OPEN, mock, listener1, listener2 );

    mock.start();
    checkEvent( 0, LineEvent.Type.START, mock, listener1, listener2 );

    // Read something to advance the position.
    mock.read( output, 0, 8 );

    mock.stop();
    checkEvent( 8, LineEvent.Type.STOP, mock, listener1, listener2 );

    mock.close();
    checkEvent( 8, LineEvent.Type.CLOSE, mock, listener1, listener2 );
  }

  @Test
  public void testControls() {
    System.out.println( "MockTargetDataLine controls" );
    byte[] samples = { 0, 1, 2 };
    boolean loop = true;
    byte[] output = new byte[ 10 ];

    // Give the mock a control.
    Control[] controls = { new MockTargetDataLine.MuteControl( false ) };
    MockTargetDataLine mock
        = new MockTargetDataLine( samples, loop, sampleBytes1, format1,
                                  controls );

    assertArrayEquals( controls, mock.getControls() );
    assertTrue( mock.isControlSupported( BooleanControl.Type.MUTE ) );
    assertFalse( mock.isControlSupported( BooleanControl.Type.APPLY_REVERB ) );
    assertNotNull( mock.getControl( BooleanControl.Type.MUTE ) );
    assertNull( mock.getControl( BooleanControl.Type.APPLY_REVERB ) );
  }
}
