package ptressel.soundutils;

import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
//import javax.sound.sampled.Line.Info;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * <p>MockTargetDataLine allows providing a known sequence of sound samples, for
 * testing sound consumers.  It can provide samples either from a supplied array
 * of sample buffers, or samples extracted from a supplied
 * {@link SoundUtils#Wave}.
 *
 * <p>This implementation does not block on read -- it does not emulate the
 * sampling rate delay (since that would slow down testing).  It allows
 * providing a set of Controls, but does not alter the sound samples based on
 * control settings.
 * 
 * @author Patricia Tressel
 */
// TODO perhaps:
// Allow setting whether various methods, such as open() and close(), should
// throw exceptions, such as SecurityException or LineUnvailableException.
// Allow pausing before returning from read(), to emulate sample rate, in case
// thread interaction is being exercised.  Alternatively allow signalling
// MockTargetDataLine when it should return from read(), for thread safety
// testing.
// Allow reading the samples from a clip.
// Add methods that allow simulating a gap in the input data (e.g. to mimic
// loss of samples due to resource limitations).  Note this should send STOP
// and START events.
// Find out what controls are supported by the capture TargetDataLine returned
// by AudioSystem and make mocks for those.  Only mute is provided currently.
//
// CAUTION:  If multiple channels are supported, will need to distinguish frame
// size and sample size.  Non-PCM formats might have frame rate != sample rate.
public class MockTargetDataLine implements TargetDataLine {

  /**
   * Mute control:  Currently mute does nothing.
   */
  // TODO If this control is installed in a MockTargetDataLine and its value
  // is set to true, return zero for all samples.
  public static class MuteControl extends BooleanControl {
    public MuteControl( boolean initialValue ) {
      super( BooleanControl.Type.MUTE, initialValue );
    }
  }

  /**
   * Can be set true via setLineAvailable( boolean isLineAvailable ) for
   * testing calls to open when unavailable.  This has nothing to do with
   * the available() method that returns the amount of data available.
   */
  private boolean isLineAvailable = true;

  /**
   * Value (representing number of bytes of data currently available to read)
   * that calls to available() should return.
   */
  private int bytesAvailable = 0;

  /** Set true if open has been called. */
  private boolean isOpen = false;

  /**
   * Set true when start() is called, set false on stop().  When stopped,
   * reads will return zero bytes until start() is called again.
   */
  private boolean isStarted = false;

  /**
   * Set when drain() is called to indicate no more data should be returned
   * by calls to read().
   */
  private boolean isDrained = false;

  private AudioFormat format = null;

  /**
   * Default AudioFormat, if not supplied via open().  Currently only the
   * sample size is used.  If this mock will read from a Wave, you can use
   * the Wave's AudioFormat.  Else can use something simple like
   * AudioFormat( 1.0F, sampleSize, 1, true, true).
   */
  private AudioFormat defaultFormat = null;

  /** Value to be returned by getBufferSize; otherwise not used. */
  private int bufferSize = 1;

  /**
   * Default value for bufferSize, to be used if not supplied via open().
   * Set via constructor.  Note if this default isn't a multiple of the sample
   * size, and caller does not supply a bufferSize, open() will throw an
   * exception.
   */
  private int defaultBufferSize = 1;

  /**
   * Number of samples returned by read() since open() was called.
   */
  private long framePosition = 0;

  /** A Wave from which to draw samples. */
  private Wave sourceWave = null;

  /**
   * An array containing samples to return.  One may loop over the array,
   * repeatedly providing the same sequence of samples, by setting loop to
   * true in the constructor that takes an array.
   */
  private byte[] sourceArray = null;

  /**
   * When reading from supplied buffers, allow specifying whether read should
   * loop when it reaches the end of the array of buffers.
   */
  private boolean loop = true;

  /** Index of next byte in the array of samples to return. */
  private int nextToCopy = 0;

  /** Last sample returned. */
  private int lastSample = 0;

  /** Listeners. */
  private Vector<LineListener> listeners = new Vector<LineListener>();

  /** Controls. */
  private Control[] controls = null;

  /**
   * Set up to read samples from the supplied Wave.  (No check is made that the
   * Wave's AudioFormat is compatible with that specified in open(), as the
   * test code is presumably setting what is intended, and the intent may be
   * that it does not match open().)
   * 
   * @param sourceWave A Wave from which to read samples.
   * @param defaultBufferSize Value for bufferSize if not supplied via open().
   * @param defaultFormat A format to use if not supplied via open().
   * Suggest using the same format is sourceWave.
   */
  public MockTargetDataLine(
          Wave sourceWave, int defaultBufferSize, AudioFormat defaultFormat,
          Control[] controls ) {
    if ( sourceWave == null ) {
      throw new IllegalArgumentException( "Supply non-null Wave." );
    }
    this.sourceWave = sourceWave;
    commonInit( defaultBufferSize, defaultFormat, controls );
  }

  /**
   * Set up to return samples from a supplied array.
   *
   * @param samples An array containing sample bytes.
   * @param loop If true, when read reaches the end of the supplied
   * array, it will start over from the beginning.
   * @param defaultBufferSize Value for bufferSize if not supplied via open().
   * @param defaultFormat A format to use if not supplied via open().
   * Suggest using something simple, such as
   * AudioFormat( 1.0F, sampleSize, 1, true, true ).
   *
   */
  // TODO perhaps -- Add a Wave subclass that reads from an array, then
  // remove this.  And do we really need loop?  A TargetDataLine (unlike a
  // Clip) blocks until enough data comes in to satisfy the requested len.
  // Only current purpose of loop is to allow testing a read that returns
  // less than len due to the line being closed, flushed, or drained during
  // a read.
  public MockTargetDataLine(
          byte[] sourceArray, boolean loop,
          int defaultBufferSize, AudioFormat defaultFormat,
          Control[] controls ) {
    if ( sourceArray == null ) {
      throw new IllegalArgumentException( "Supply non-null sample data." );
    }
    this.sourceArray = sourceArray;
    this.loop = loop;
    nextToCopy = 0;
    commonInit( defaultBufferSize, defaultFormat, controls );
  }

  private void commonInit(
          int defaultBufferSize, AudioFormat defaultFormat,
          Control[] controls ) {
    this.defaultBufferSize = defaultBufferSize;
    this.defaultFormat = defaultFormat;
    this.controls = controls;
  }

  // Helper methods for tests -- these allow setting or examining the state of
  // the mock TargetDataLine.

  /**
   * Set whether the line is available or not.  This controls whether an
   * attempt to open the line gets LineUnavailableException.
   * @param isLineAvailable whether open() should treat line as available.
   */
  public void setLineAvailable( boolean isLineAvailable ) {
    this.isLineAvailable = isLineAvailable;
  }

  /**
   * Set the number of bytes that following calls to available() should return.
   * @param bytesAvailable the value that subsequent calls to available() will
   * return.
   */
  public void setBytesAvailable( int bytesAvailable ) {
    this.bytesAvailable = bytesAvailable;
  }

  /** Get the listeners, if any. */
  public Vector<LineListener> getLineListeners() {
    return listeners;
  }

  // TargetDataLine interface methods.

  /**
   * Set state of the line to open.
   *
   * @param format The intended AudioFormat.
   * @param bufferSize In a real TargetDataLine, this would be the size of
   * the buffer to allocate.  Here, it is only checked to make sure it's a
   * multiple of the sample size in the AudioFormat.
   * @throws LineUnavailableException if line has been marked unavailable
   * via setLineUnavailable.
   * @throws IllegalStateException if line has already been opened.
   * @throws IllegalArgumentException if bufferSize isn't a multiple of the
   * format's sample size.
   */
  public void open( AudioFormat format, int bufferSize )
          throws LineUnavailableException, IllegalStateException {
    // Validate the arguments first, before checking is the line is already
    // open, as the interface spec only says this "may" throw an exception
    // if already open.
    if ( format == null ) {
      throw new IllegalArgumentException( "No AudioFormat supplied." );
    }
    // TODO validate the AudioFormat.
    if ( ( bufferSize <= 0 ) || ( bufferSize % format.getFrameSize() ) != 0 ) {
      throw new IllegalArgumentException( "Bad buffer size." );
    }
    if ( !isLineAvailable ) {
      throw new LineUnavailableException( "Line not available." );
    }
    if ( isOpen ) {
      throw new IllegalStateException( "Already open." );
    }
    this.format = format;
    this.bufferSize = bufferSize;
    framePosition = 0;
    isOpen = true;
    updateLineListeners(
        new LineEvent( this, LineEvent.Type.OPEN, getLongFramePosition() ) );
  }

  /**
   * Set state of the line to open.
   *
   * @param format The AudioFormat for the data to be returned -- only the
   * sample size is used by the mock.
   * @throws LineUnavailableException if line has been marked unavailable
   * via setLineUnavailable.
   * @throws IllegalStateException if line has already been opened.
   */
  public void open( AudioFormat format )
          throws LineUnavailableException, IllegalStateException {
    // Use the sample size from the format for the buffer size -- that won't
    // cause an exception with any sample size.
    open( format, ( format != null ) ? format.getFrameSize() : 0 );
  }

  /**
   * Set state of the line to open.  Use a simple AudioFormat with a sample
   * size of 1, which won't cause an exception with any buffer size.
   *
   * @throws LineUnavailableException if line has been marked unavailable
   * via setLineUnavailable.
   * @throws IllegalStateException if line has already been opened.
   */
  public void open() throws LineUnavailableException, IllegalStateException {
    open( new AudioFormat( 1.0F, 1, 1, true, true ) );
  }

  /**
   * Read bytes from the source specified during construction.
   * 
   * If there is a mute control, and it's set to true, then all samples
   * returned will be zero.  Mute is only checked at the beginning of the read.
   *
   * @param b Array to write the bytes into.
   * @param off Offset of first byte to write in b.
   * @param len Number of bytes to write into b.
   */
  // TODO perhaps:  Allow specifying whether and for how long this should block.
  // E.g. support blocking if len > bytesAvailable.  Allow specifying whether
  // a close, flush, or drain happens "during" the read.  Note a read of less
  // than len bytes can currently be produced by using a byte array as the
  // source of data, and not looping, but that won't mark the line as closed or
  // drained.
  // TODO If there is a mute control, and it's set to true, then return zero for
  // all samples.  Only check mute at the beginning of the read.
  public int read( byte[] b, int off, int len ) {
    if ( !isOpen || !isStarted || isDrained ) {
      return 0;
    }
    if ( ( len < 0 ) || ( off < 0 ) || ( off + len > b.length ) ) {
      throw new IllegalArgumentException(
              "Bad value for offset or length." );
    }
    if ( ( len % format.getFrameSize() ) != 0 ) {
      throw new IllegalArgumentException(
              "Requested number of bytes is not a multiple of frame size." );
    }

    int numRead = 0;
    if ( sourceArray != null ) {
      // Here, return the next set of samples from the supplied array.
      // We may have to wrap around in our source array to get enough samples.
      int numCopied = 0;
      int copyTo = off;
      while ( numCopied < len && ( loop || nextToCopy < sourceArray.length ) ) {
        // Copy over the number of bytes wanted, or the entire tail of the
        // source array, whichever is smaller.
        int numToCopy = Math.min( len - numCopied,
                                  sourceArray.length - nextToCopy );
        System.arraycopy( sourceArray, nextToCopy, b, copyTo, numToCopy );
        // Store the last sample we just copied as the current sample, in case
        // we're exiting the loop.
        lastSample = b[ copyTo + numToCopy - 1 ];
        // Bump indices and counters.
        numCopied += numToCopy;
        copyTo += numToCopy;
        nextToCopy += numToCopy;
        if ( loop && ( nextToCopy >= sourceArray.length ) ) nextToCopy = 0;
      }
      numRead = numCopied;
    } else if ( sourceWave != null ) {
      // Here, read samples from the Wave.
      sourceWave.insertNextBytes( b, off, len );
      // Say we returned the requested number even if the audio format's
      // sample size caused the number of bytes to be rounded down to the
      // nearest whole sample.  The format mismatch is either deliberate or
      // a bug in the test which will likely cause the test to fail.
      numRead = len;
    } else {
      // Add more options here...
      throw new RuntimeException(
              "Bad setup -- no source of samples specified." );
    }

    framePosition += numRead / format.getFrameSize();
    return numRead;
  }

  /**
   * Mark that no more data should be returned.  Subsequent reads will return
   * zero bytes.  (Does not simulate blocking forever, as would happen on a
   * source that could not be drained.)
   */
  public void drain() {
    isDrained = true;
  }

  /**
   * Currently this is a no-op -- no data is skipped following a call to flush.
   */
  public void flush() {
  }

  public void start() {
    isDrained = false;
    isStarted = true;
    // Send START immediately.
    // TODO Do we need to provide a way to delay sending START, e.g. if we're
    // also providing a way to delay a return from read(), and it's the first
    // read() call?
    updateLineListeners(
        new LineEvent( this, LineEvent.Type.START, getLongFramePosition() ) );
  }

  public void stop() {
    isStarted = false;
    updateLineListeners(
        new LineEvent( this, LineEvent.Type.STOP, getLongFramePosition() ) );
  }

  public boolean isRunning() {
    return isStarted;
  }

  public boolean isActive() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public AudioFormat getFormat() {
    return format;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public int available() {
    return bytesAvailable;
  }

  public int getFramePosition() {
    return (int) ( framePosition % ( (long)Integer.MAX_VALUE + 1 ) );
  }

  public long getLongFramePosition() {
    return framePosition;
  }

  /**
   * This is a helper for testing MockTargetDataLine#getFramePosition.
   * It allows framePosition to be set to a number near
   * (long)Integer.MAX_VALUE + 1, so only a short read is needed to cause
   * overflow in an int.
   * @param newFramePosition Value to set framePosition to.
   */
  public void setLongFramePosition( long newFramePosition ) {
    framePosition = newFramePosition;
  }

  // CAUTION:  Currently only mono, PCM are supported, so sample rate and size
  // are the same as frame rate and size.
  public long getMicrosecondPosition() {
    return framePosition * 1000 / (long) format.getFrameRate();
  }

  // Level is the non-negative amplitude of the sound as a fraction of the
  // maximum possible amplitude (e.g. for a byte sample width, the maximum is
  // Byte.MAX_VALUE.  Regard the current amplitude as that of the last sample
  // that was returned.  Given a short sample interval, that is reasonable.
  // (Use the current, not next, value because Wave does not support reading
  // ahead, then pushing back the next value.)
  // (The doc for {@link DataLine#getLevel()}, says the range is from 0.0 to
  // 1.0, but says amplitude, not power, so assume we're intended to take the
  // absolute value, not the square.)
  // TODO Verify that this is a reasonable mock for getLevel.  E.g. the level
  // returned by a real TargetDataLine might continue to track the actual
  // external sound even if the TargetDataLine isn't being read at the moment.
  public float getLevel() {
    return Math.abs( lastSample ) /
           SoundUtils.maxAmplitudePerBitDepth( format.getSampleSizeInBits() );
  }

  public Info getLineInfo() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void close() {
    isOpen = false;
    updateLineListeners(
        new LineEvent( this, LineEvent.Type.CLOSE, getLongFramePosition() ) );
  }

  public boolean isOpen() {
    return isOpen;
  }

  public Control[] getControls() {
    return controls;
  }

  public boolean isControlSupported( Type type ) {
    for ( Control control : controls ) {
      if ( control.getType().equals( type ) ) {
        return true;
      }
    }
    return false;
  }

  public Control getControl( Type type ) {
    for ( Control control : controls ) {
      if ( control.getType().equals( type ) ) {
        return control;
      }
    }
    return null;
  }

  public void addLineListener(LineListener listener) {
    listeners.add( listener );
  }

  public void removeLineListener(LineListener listener) {
    listeners.remove( listener );
  }

  private void updateLineListeners( LineEvent event ) {
    for ( LineListener listener : listeners ) {
      listener.update( event );
    }
  }
}
