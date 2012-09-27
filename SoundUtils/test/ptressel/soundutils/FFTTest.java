package ptressel.soundutils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import ptressel.soundutils.SoundUtils.IndexAndValue;

/**
 * @author Patricia Tressel
 */
public class FFTTest {

  public FFTTest() {
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
   * Test of size method, of class FFT.
   */
  @Test
  public void testSize() {
    System.out.println("size");
    int expResult = 2;
    FFT instance = new FFT(expResult, 1.0, 1);
    int result = instance.size();
    assertEquals(expResult, result);
  }

  /**
   * Test of resultsSize method, of class FFT.
   */
  @Test
  public void testResultsSize() {
    System.out.println("resultsSize");
    // With an FFT buffer of size 2, the Nyquist frequency is at index N/2 = 1,
    // so the number of meaningful results is N/2 + 1 = 2.
    FFT instance = new FFT(2, 1.0, 1);
    int expResult = 2;
    int result = instance.resultsSize();
    assertEquals(expResult, result);
  }

  /**
   * Test of nyquist method, of class FFT.
   */
  @Ignore
  @Test
  public void testNyquist() {
    System.out.println("nyquist");
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.nyquist();
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of frequencyAtIndex method, of class FFT.
   */
  @Ignore
  @Test
  public void testFrequencyAtIndex() {
    System.out.println("frequencyAtIndex");
    int n = 0;
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.frequencyAtIndex(n);
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of indexForFrequency method, of class FFT.
   */
  @Ignore
  @Test
  public void testIndexForFrequency() {
    System.out.println("indexForFrequency");
    double f = 0.0;
    FFT instance = null;
    int expResult = 0;
    int result = instance.indexForFrequency(f);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getResult method, of class FFT.
   */
  @Ignore
  @Test
  public void testGetResult() {
    System.out.println("getResult");
    int n = 0;
    FFT instance = null;
    Complex expResult = null;
    Complex result = instance.getResult(n);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getResultsArray method, of class FFT.
   */
  @Ignore
  @Test
  public void testGetResultsArray() {
    System.out.println("getResultsArray");
    FFT instance = null;
    Complex[] expResult = null;
    Complex[] result = instance.getResultsArray();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of copyResultsArray method, of class FFT.
   */
  @Ignore
  @Test
  public void testCopyResultsArray() {
    System.out.println("copyResultsArray");
    FFT instance = null;
    Complex[] expResult = null;
    Complex[] result = instance.copyResultsArray();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of swapResultsArray method, of class FFT.
   */
  @Ignore
  @Test
  public void testSwapResultsArray() {
    System.out.println("swapResultsArray");
    Complex[] newArray = null;
    FFT instance = null;
    Complex[] expResult = null;
    Complex[] result = instance.swapResultsArray(newArray);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getPowerArray method, of class FFT.
   */
  @Ignore
  @Test
  public void testGetPowerArray() {
    System.out.println("getPowerArray");
    FFT instance = null;
    double[] expResult = null;
    double[] result = instance.getPowerArray();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getSmoothedPowerArray method, of class FFT.
   */
  @Ignore
  @Test
  public void testGetSmoothedPowerArray() {
    System.out.println("getSmoothedPowerArray");
    FFT instance = null;
    double[] expResult = null;
    double[] result = instance.getSmoothedPowerArray();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFilterArray method, of class FFT.
   */
  @Ignore
  @Test
  public void testGetFilterArray() {
    System.out.println("getFilterArray");
    FFT instance = null;
    double[] expResult = null;
    double[] result = instance.getFilterArray();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of averagePower method, of class FFT.
   */
  @Ignore
  @Test
  public void testAveragePower() {
    System.out.println("averagePower");
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.averagePower();
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of maximumPower method, of class FFT.
   */
  @Ignore
  @Test
  public void testMaximumPower() {
    System.out.println("maximumPower");
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.maximumPower();
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of maximumSmoothedPower method, of class FFT.
   */
  @Ignore
  @Test
  public void testMaximumSmoothedPower() {
    System.out.println("maximumSmoothedPower");
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.maximumSmoothedPower();
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of powerAtIndex method, of class FFT.
   */
  @Ignore
  @Test
  public void testPowerAtIndex() {
    System.out.println("powerAtIndex");
    int n = 0;
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.powerAtIndex(n);
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of powerAtFrequency method, of class FFT.
   */
  @Ignore
  @Test
  public void testPowerAtFrequency() {
    System.out.println("powerAtFrequency");
    double f = 0.0;
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.powerAtFrequency(f);
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of powerInIndexRange method, of class FFT.
   */
  @Ignore
  @Test
  public void testPowerInIndexRange() {
    System.out.println("powerInIndexRange");
    int n1 = 0;
    int n2 = 0;
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.powerInIndexRange(n1, n2);
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of powerInFreqRange method, of class FFT.
   */
  @Ignore
  @Test
  public void testPowerInFreqRange() {
    System.out.println("powerInFreqRange");
    double f1 = 0.0;
    double f2 = 0.0;
    FFT instance = null;
    double expResult = 0.0;
    double result = instance.powerInFreqRange(f1, f2);
    assertEquals(expResult, result, 0.0);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of powerPeaks method, of class FFT.
   */
  @Ignore
  @Test
  public void testPowerPeaks() {
    System.out.println("powerPeaks");
    double margin = 0.0;
    double threshold = 0.0;
    boolean plateauCenterOnly = false;
    FFT instance = null;
    IndexAndValue[] expResult = null;
    IndexAndValue[] result =
            instance.powerPeaks( margin, threshold, plateauCenterOnly );
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of smoothedPowerPeaks method, of class FFT.
   */
  @Ignore
  @Test
  public void testSmoothedPowerPeaks() {
    System.out.println("smoothedPowerPeaks");
    double margin = 0.0;
    double threshold = 0.0;
    boolean plateauCenterOnly = false;
    FFT instance = null;
    IndexAndValue[] expResult = null;
    IndexAndValue[] result =
            instance.smoothedPowerPeaks( margin, threshold, plateauCenterOnly );
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of fft method, of class FFT.
   */
  @Test
  public void testFft() {
    System.out.println("fft");

    // Try a little bitty input -- N = 2.
    int N1 = 2;
    double[] signal1 = { 1, 2 };
    // With this input, both hand calculation and Matlab's fft get { 3, -1 }.
    Complex[] expResult1 = { new Complex( 3, 0 ), new Complex( -1, 0 ) };
    // (Arbitrary) sample frequency is 1 Hz.
    FFT instance1 = new FFT( N1, 1.0 );
    instance1.fft( signal1 );
    Complex[] result1 = instance1.getResultsArray();
    // Check that each result value is within 2 ulps of the expected value.
    // (Should probably be 2 log 2, i.e. 3ish, to be ~ # operations.)
    for ( int n = 0; n < N1; n++ ) {
      assertTrue( result1[n].closeEnough( expResult1[n], 2 ) );
    }

    // Next, N = 4.
    int N2 = 4;
    double[] signal2 = { 1, 2, 3, 4 };
    // With this input, both hand calculation and Matlab's fft get
    // { 10, -2+2i, -2, -2-2i }.
    Complex[] expResult2 = { new Complex( 10, 0 ), new Complex( -2, 2 ),
                             new Complex( -2, 0 ), new Complex( -2, -2 ) };
    FFT instance2 = new FFT( N2, 1.0 );
    instance2.fft( signal2 );
    Complex[] result2 = instance2.getResultsArray();
    // This time, since there are more double operations, allow a mismatch
    // of 4 ulps.
    for ( int n = 0; n < N2; n++ ) {
      assertTrue( result2[n].closeEnough( expResult2[n], 4 ) );
    }
  }

  /**
   * Test of dft method, of class FFT.
   */
  @Test
  public void testDft() {
    System.out.println("dft");
    // Try a little bitty input -- N = 2.
    double[] signal = { 1, 2 };
    // With this input, both hand calculation and Matlab's fft get { 3, -1 }.
    Complex[] expResult = { new Complex( 3, 0 ), new Complex( -1, 0 ) };
    Complex[] result = FFT.dft(signal);
    assertEquals(expResult, result);

    // Next up -- N = 4.
    double[] signal2 = { 1, 2, 3, 4 };
    // With this input, both hand calculation and Matlab's fft get
    // { 10, -2+2i, -2, -2-2i }.
    Complex[] expResult2 = { new Complex( 10, 0 ), new Complex( -2, 2 ),
                             new Complex( -2, 0 ), new Complex( -2, -2 ) };
    Complex[] result2 = FFT.dft( signal2 );
    assertEquals(expResult2, result2);
  }
}