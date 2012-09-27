package ptressel.soundutils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ptressel.soundutils.SoundUtils.IndexAndValue;

/**
 * @author Patricia Tressel
 */
public class SoundUtilsTest {

  /**
   * JUnit assertArrayEquals(double[] expecteds, double[] actuals, double delta)
   * requires a fixed value for the allowed roundoff error -- it does not (for
   * instance) take Math.ulps of each comparand and use the larger (or half
   * of the larger) as the allowed error.  So here, accept an error equal to
   * the ulps of the largest double we'll be comparing.  Right now, that's
   * (double) (Short.MAX_VALUE + Short.MAX_VALUE/3), used in testGenerateCycle.
   * Be sure to update DOUBLE_EPSILON if the largest double tested changes.
   * Or else add a new assertArrayEquals that does the comparison with the
   * above-suggested ulps-based roundoff error.
   */
  public static final double DOUBLE_EPSILON =
          Math.ulp( (double) (Short.MAX_VALUE + Short.MAX_VALUE/3) );

  public SoundUtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Valid input cases for
   * {@link SoundUtils#extractSample(byte[], int, int, boolean, boolean)}
   * are tested in SoundUtilsExtractSampleTest.
   *
   * Valid input cases for
   * {@link SoundUtils#insertSample(double, byte[], int, int, boolean, boolean)}
   * are tested in SoundUtilsInsertSampleTest.
   */

  /**
   * Test of peaks method, of class SoundUtils.
   */
  @Test
  public void testPeaks() {
    System.out.println("SoundUtils#peaks");

    // With a margin of 2, this sequence has peaks only at the ends.  It also
    // has a run of equal values.  This exercises plateau rejection, margin,
    // and peak at ends cases.  Use margin = 2, threshold = 0;
    double[] array1 = { 100, 90, 80, 82, 80, 78, 80, 70, 80, 90, 90, 90, 100 };
    double margin = 2.0;
    double threshold = 0.0;
    boolean plateauCenterOnly = false;
    IndexAndValue[] expResult1 = {
      new IndexAndValue( 0, 100 ),
      new IndexAndValue( array1.length-1, 100 )  };
    IndexAndValue[] result =
            SoundUtils.peaks( array1, margin, threshold, plateauCenterOnly );
    assertArrayEquals( expResult1, result );

    // With a margin of 2 and threshold of 25, this sequence has no peaks at
    // the ends, two plateau peaks, and a single point peak.  It tests ignoring
    // differences within margin and ignoring values below threshold.
    double[] array2 = {
      80, 90, 92, 90, 80, 10, 12, 20, 10, 90, 88, 90, 80, 83, 80 };
    margin = 2.0;
    threshold = 25.0;
    plateauCenterOnly = false;
    IndexAndValue[] expResult2 = {
      new IndexAndValue( 1, 90 ),
      new IndexAndValue( 2, 92 ),
      new IndexAndValue( 3, 90 ),
      new IndexAndValue( 9, 90 ),
      new IndexAndValue( 10, 88 ),
      new IndexAndValue( 11, 90 ),
      new IndexAndValue( 13, 83 ),
    };
    result = SoundUtils.peaks( array2, margin, threshold, plateauCenterOnly );
    assertArrayEquals( expResult2, result );

    // Using the same array of values, threshold, and margin,, find peaks but
    // ask for only centers of plateaus.
    plateauCenterOnly = true;
    IndexAndValue[] expResult2a = {
      new IndexAndValue( 2, 92 ),
      new IndexAndValue( 10, 90 ),
      new IndexAndValue( 13, 83 ),
    };
    result = SoundUtils.peaks( array2, margin, threshold, plateauCenterOnly );
    assertArrayEquals( expResult2a, result );
  }

  /**
   * Test of topNPeaks method, of class SoundUtils.
   */
  @Test
  public void testTopNPeaks() {
    System.out.println("SoundUtils#topNPeaks");
    double margin = 2.0;
    double threshold = 25.0;
    double[] array = {
      80, 90, 92, 90, 80, 10, 12, 20, 10, 90, 88, 90, 80, 83, 80 };

    // Ask for exactly the number of peaks.  Include peaks of equal size to
    // test stability.
    int N = 7;
    boolean plateauCenterOnly = false;
    IndexAndValue[] expResult = {
      new IndexAndValue( 2, 92 ),
      new IndexAndValue( 1, 90 ),
      new IndexAndValue( 3, 90 ),
      new IndexAndValue( 9, 90 ),
      new IndexAndValue( 11, 90 ),
      new IndexAndValue( 10, 88 ),
      new IndexAndValue( 13, 83 ),
    };
    IndexAndValue[] result = SoundUtils.topNPeaks( N, array, margin, threshold,
                                                   plateauCenterOnly );
    assertEquals(expResult, result);

    // Ask for fewer peaks than all.
    N = 3;
    IndexAndValue[] expResult2 = {
      new IndexAndValue( 2, 92 ),
      new IndexAndValue( 1, 90 ),
      new IndexAndValue( 3, 90 ),
    };
    result = SoundUtils.topNPeaks( N, array, margin, threshold,
                                   plateauCenterOnly );
    assertEquals(expResult2, result);

    // Ask for more peaks than there are (set plateauCenterOnly to
    // reduce the number of actual peaks).
    N = 5;
    plateauCenterOnly = true;
    IndexAndValue[] expResult3 = {
      new IndexAndValue( 2, 92 ),
      new IndexAndValue( 10, 90 ),
      new IndexAndValue( 13, 83 ),
    };
    result = SoundUtils.topNPeaks( N, array, margin, threshold,
                                   plateauCenterOnly );
    assertEquals(expResult3, result);
  }

  /**
   * Test of generateCycle method, of class SoundUtils.
   */
  @Test
  public void testGenerateCycle() {
    System.out.println("SoundUtils#generateCycle");
    System.out.println("EPSILON = " + DOUBLE_EPSILON);

    // Use an amplitude that would get clipped for a two-byte sample width.
    double frequency = 400.0;
    int amplitude = Short.MAX_VALUE + Short.MAX_VALUE/3;
    double sampleRate = 8 * 400.0;  // This will yield 8 samples.
    // Due (presumably) to roundoff error in pi, the phase angles are a bit
    // off, so the angles that would be at zeros of cosine without error are
    // not quite at the zeros...
    double[] expResult =
    { 43689.0, 30892.788163259076, 2.6751797003974358E-12,
      -30892.788163259072, -43689.0, -30892.788163259083,
      -8.025539101192307E-12, 30892.78816325907 };
    double[] result =
            SoundUtils.generateCycle( frequency, amplitude, sampleRate );
    assertArrayEquals( expResult, result, DOUBLE_EPSILON );
  }

  /**
   * Test of generateCycleFormatted method, of class SoundUtils.
   */
  @Test
  public void testGenerateCycleFormatted() {
    System.out.println("SoundUtils#generateCycleFormatted");

    // 1-byte format, amplitude not big enough to get clipped
    double frequency = 400.0;
    int amplitude = Byte.MAX_VALUE / 2;
    double sampleRate = 8 * 400.0;
    int sampleBytes = 1;
    boolean signed = true;
    boolean bigEndian = true;
    int channels = 1;
    byte[] expResult = {
      (byte)0x3f, (byte)0x2d, (byte)0x00, (byte)0xd3,
      (byte)0xc1, (byte)0xd3, (byte)0x00, (byte)0x2d };
    byte[] result = SoundUtils.generateCycleFormatted(
            frequency, amplitude, sampleRate, sampleBytes,
            signed, bigEndian, channels );
    assertArrayEquals( expResult, result );

    // 2-byte, signed, big endian, not clipped
    frequency = 400.0;
    amplitude = Short.MAX_VALUE / 2;
    sampleRate = 8 * 400.0;
    sampleBytes = 2;
    signed = true;
    bigEndian = true;
    channels = 1;
    byte[] expResult2 = {
      (byte)0x3f, (byte)0xff,
      (byte)0x2d, (byte)0x41,
      (byte)0x00, (byte)0x00,
      (byte)0xd2, (byte)0xbf,
      (byte)0xc0, (byte)0x01,
      (byte)0xd2, (byte)0xbf,
      (byte)0x00, (byte)0x00,
      (byte)0x2d, (byte)0x41 };
    result = SoundUtils.generateCycleFormatted(
            frequency, amplitude, sampleRate, sampleBytes,
            signed, bigEndian, channels );
    assertArrayEquals( expResult2, result );

    // 2-byte, signed, little endian, clipped
    frequency = 400.0;
    amplitude = Short.MAX_VALUE + Short.MAX_VALUE / 3;
    sampleRate = 8 * 400.0;
    sampleBytes = 2;
    signed = true;
    bigEndian = false;
    channels = 1;
    byte[] expResult3 = {
      (byte)0xff, (byte)0x7f,
      (byte)0xad, (byte)0x78,
      (byte)0x00, (byte)0x00,
      (byte)0x53, (byte)0x87,
      (byte)0x00, (byte)0x80,
      (byte)0x53, (byte)0x87,
      (byte)0x00, (byte)0x00,
      (byte)0xad, (byte)0x78 };
    result = SoundUtils.generateCycleFormatted(
            frequency, amplitude, sampleRate, sampleBytes,
            signed, bigEndian, channels );
    assertArrayEquals( expResult3, result );
  }

  /**
   * Test of {@link SoundUtils#toHexString(byte)} and
   * {@link SoundUtils#toHexString(short)} methods.  In particular, check the
   * cases where SoundUtils#toHexString differs from Integer#toHexString, which
   * are those where Integer#toHexString would sign-extend its byte or short arg
   * then left-fill with "f"s.
   */
  @Test
  public void testToHexString() {
    System.out.println("SoundUtils#toHexString");

    // negative inputs
    String result = SoundUtils.toHexString( (byte)0xEE );
    assertEquals("ee", result);
    result = SoundUtils.toHexString( (short)0xEEEE );
    assertEquals("eeee", result);
    // positive inputs
    // TODO If toHexString is changed to left-fill with zeros, change results.
    result = SoundUtils.toHexString( (byte)1 );
    assertEquals("1", result);
    result = SoundUtils.toHexString( (short)1 );
    assertEquals("1", result);
  }

  /**
   * Test of getArgForOption method, of class SoundUtils.
   */
  @Test
  public void testGetArgForOption() {
    System.out.println("SoundUtils#getArgForOption");

    String[] args = {
      "optWithNoDash", "itsArg",
      "-optWithADash", "andItsArg",
      "-firstOfTwoOptsInARow", "-secondOfTwoOptsInARow" };

    String option = "optWithNoDash";
    String expResult = "itsArg";
    String result = SoundUtils.getArgForOption(args, option);
    assertEquals(expResult, result);

    option = "-optWithADash";
    expResult = "andItsArg";
    result = SoundUtils.getArgForOption(args, option);
    assertEquals(expResult, result);

    option = "-firstOfTwoOptsInARow";
    expResult = "";
    result = SoundUtils.getArgForOption(args, option);
    assertEquals(expResult, result);

    option = "-secondOfTwoOptsInARow";
    expResult = "";
    result = SoundUtils.getArgForOption(args, option);
    assertEquals(expResult, result);
  }

  /**
   * Test of checkOption method, of class SoundUtils.
   */
  @Test
  public void testCheckOption() {
    System.out.println("SoundUtils#checkOption");

    String[] args = { "-a", "b", "cdef" };
    assertTrue( SoundUtils.checkOption( args, "-a" ) );
    assertTrue( SoundUtils.checkOption( args, "b" ) );
    assertTrue( SoundUtils.checkOption( args, "cdef" ) );
    assertFalse( SoundUtils.checkOption( args, "ghij" ) );
  }
}