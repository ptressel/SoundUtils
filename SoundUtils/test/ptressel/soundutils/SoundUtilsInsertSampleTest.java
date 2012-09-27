package ptressel.soundutils;

import java.util.Arrays;
import java.util.Collection;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

/**
 * Split out the test of
 * {@link SoundUtils#insertSample(byte[], int, int, boolean, boolean)}
 * so it can be parameterized.
 *
 * @author Patricia Tressel
 */
@RunWith(Parameterized.class)
public class SoundUtilsInsertSampleTest {
  private double sampleValue;
  private byte[] buffer;
  private int byteOffset;
  private int sampleBytes;
  private boolean signed;
  boolean bigEndian;
  // Expected result for the current set of insertSample params.  This is an
  // array that will be compared to buffer after it is modified by insertSample.
  private byte[] expResult;

/**
   * Parameters here are the same as those
   * {@link SoundUtils#insertSample(byte[], int, int, boolean, boolean)},
   * with the exception that the expected test results are the final parameter.
   */
  public SoundUtilsInsertSampleTest(
          double sampleValue, byte[] buffer, int byteOffset, int sampleBytes,
          boolean signed, boolean bigEndian,
          byte[] expResult ) {
    this.sampleValue = sampleValue;
    this.buffer = buffer;
    this.byteOffset = byteOffset;
    this.sampleBytes = sampleBytes;
    this.signed = signed;
    this.bigEndian = bigEndian;
    this.expResult = expResult;
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    System.out.println("SoundUtils#insertSample");
  }

  @Parameters
  public static Collection parameters() {
    return Arrays.asList( new Object[][] {
      // Begin by testing the format conversion.  Fill the buffer with
      // values that will not appear in the inserted sample.  Include zero,
      // positive, and negative input sample values, and use all options for
      // sign extension and endianness, though sign extension is a no-op when
      // converting a numerical sample into an audio formatted sample.

      // For the one-byte format tests, endianness is a no-op.

      // sign-extended, big endian
      { 0.0, new byte[] {(byte)0x80}, 0, 1, true, true, new byte[] {0} },
      { 1.0, new byte[] {(byte)0x80}, 0, 1, true, true, new byte[] {1} },
      { -2.0, new byte[] {(byte)0x80}, 0, 1, true, true, new byte[] {-2} },
      // sign-extended, little endian
      { 0.0, new byte[] {(byte)0x80}, 0, 1, true, false, new byte[] {0} },
      { 3.0, new byte[] {(byte)0x80}, 0, 1, true, false, new byte[] {3} },
      { -4.0, new byte[] {(byte)0x80}, 0, 1, true, false, new byte[] {-4} },
      // not sign-extended, big endian
      { 0.0, new byte[] {(byte)0x80}, 0, 1, false, true, new byte[] {0} },
      { 5.0, new byte[] {(byte)0x80}, 0, 1, false, true, new byte[] {5} },
      { -6.0, new byte[] {(byte)0x80}, 0, 1, false, true, new byte[] {-6} },
      // not sign-extended, little endian
      { 0.0, new byte[] {(byte)0x80}, 0, 1, false, false, new byte[] {0} },
      { 7.0, new byte[] {(byte)0x80}, 0, 1, false, false, new byte[] {7} },
      { -8.0, new byte[] {(byte)0x80}, 0, 1, false, false, new byte[] {-8} },

      // For two bytes, endianness does matter.

      // sign-extended, big endian
      { 0, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, true, new byte[] {0, 0} },
      { 1, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, true, new byte[] {0, 1} },
      { -2, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, true, new byte[] {-1, -2} },
      // sign-extended, little endian
      { 0, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, false, new byte[] {0, 0} },
      { 3, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, false, new byte[] {3, 0} },
      { -4, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, false, new byte[] {-4, -1} },
      // not sign-extended, big endian
      { 0, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, false, true, new byte[] {0, 0} },
      { 5, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, false, true, new byte[] {0, 5} },
      { -6, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, false, true, new byte[] {-1, -6} },
      // not sign-extended, little endian
      { 0, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, false, new byte[] {0, 0} },
      { 7, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, false, new byte[] {7, 0} },
      { -8, new byte[] {(byte)0x80, (byte)0x80}, 0, 2, true, false, new byte[] {-8, -1} },

      // Test indexing with in-bounds indices.  Include indices at the start,
      // middle, and end.  Use various values of endianness and signed.

      // One byte format
      { 0, new byte[] {(byte)0x80, (byte)0x80, (byte)0x80}, 0, 1, true, true,
        new byte[] {0, (byte)0x80, (byte)0x80} },
      { 1, new byte[] {(byte)0x80, (byte)0x80, (byte)0x80}, 1, 1, true, false,
        new byte[] {(byte)0x80, 1, (byte)0x80} },
      { -2, new byte[] {(byte)0x80, (byte)0x80, (byte)0x80}, 2, 1, false, true,
        new byte[] {(byte)0x80, (byte)0x80, -2} },
      // Two byte format
      { 0,
        new byte[] {(byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80},
        0, 2, true, true,
        new byte[] {0, 0,
                    (byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80} },
      { 1,
        new byte[] {(byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80},
        2, 2, true, false,
        new byte[] {(byte)0x80, (byte)0x80,
                    1, 0,
                    (byte)0x80, (byte)0x80} },
      { -2,
        new byte[] {(byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80},
        4, 2, false, false,
        new byte[] {(byte)0x80, (byte)0x80,
                    (byte)0x80, (byte)0x80,
                    -2, -1} },
    });
  }

  /**
   * Test of SoundUtils#insertSample method.
   */
  @Test
  public void testInsertSample() {
    SoundUtils.insertSample(
            sampleValue, buffer, byteOffset, sampleBytes, signed, bigEndian );
    assertArrayEquals( expResult, buffer );
  }
}