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
 * {@link SoundUtils#extractSample(byte[], int, int, boolean, boolean)}
 * so it can be parameterized.
 *
 * @author Patricia Tressel
 */
@RunWith(Parameterized.class)
public class SoundUtilsExtractSampleTest {
  private byte[] input;
  private int index;
  private int nBytes;
  private boolean signed;
  boolean bigEndian;
  // Expected result for the current set of extractSample params.
  private int expResult;

  /**
   * Parameters here are the same as those
   * {@link SoundUtils#extractSample(byte[], int, int, boolean, boolean)},
   * with the exception that the expected test results are the final parameter.
   */
  public SoundUtilsExtractSampleTest(
          byte[] input, int index, int nBytes,
          boolean signed, boolean bigEndian,
          int expResult ) {
    this.input = input;
    this.index = index;
    this.nBytes = nBytes;
    this.signed = signed;
    this.bigEndian = bigEndian;
    this.expResult = expResult;
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    System.out.println("SoundUtils#extractSample");
  }

  @Parameters
  public static Collection parameters() {
    return Arrays.asList( new Object[][] {
      // Begin by testing the format conversion itself.

      // For the one-byte sample tests, include a zero, positive, and negative
      // byte, with all options for signed results and endianness, though
      // endianness is a no-op for one byte.

      // sign-extended, big endian
      { new byte[] {0}, 0, 1, true, true, 0 },
      { new byte[] {1}, 0, 1, true, true, 1 },
      { new byte[] {-2}, 0, 1, true, true, -2 },
      // sign-extended, little endian
      { new byte[] {0}, 0, 1, true, false, 0 },
      { new byte[] {3}, 0, 1, true, false, 3 },
      { new byte[] {-4}, 0, 1, true, false, -4 },
      // not sign-extended, big endian
      { new byte[] {0}, 0, 1, false, true, 0 },
      { new byte[] {5}, 0, 1, false, true, 5 },
      { new byte[] {-6}, 0, 1, false, true, 0xFA },
      // not sign-extended, little endian
      { new byte[] {0}, 0, 1, false, false, 0 },
      { new byte[] {7}, 0, 1, false, false, 7 },
      { new byte[] {-8}, 0, 1, false, false, 0xF8 },

      // For two bytes, include all orders of pairs of positive, negative, and
      // zero.  Here, endianness does matter.  Results are given in hex when
      // that's simpler.
      
      // sign-extended, big endian
      { new byte[] {0, 0}, 0, 2, true, true, 0 },
      { new byte[] {0, 2}, 0, 2, true, true, 2 },
      { new byte[] {0, -2}, 0, 2, true, true, 0x00FE },
      { new byte[] {3, 0}, 0, 2, true, true, 0x0300 },
      { new byte[] {3, 2}, 0, 2, true, true, 0x0302 },
      { new byte[] {3, -2}, 0, 2, true, true, 0x03FE },
      { new byte[] {-3, 0}, 0, 2, true, true, 0xFFFFFD00 },
      { new byte[] {-3, 2}, 0, 2, true, true, 0xFFFFFD02 },
      { new byte[] {-3, -2}, 0, 2, true, true, 0xFFFFFDFE },
      // sign-extended, little endian
      { new byte[] {0, 0}, 0, 2, true, false, 0 },
      { new byte[] {0, 2}, 0, 2, true, false, 0x0200 },
      { new byte[] {0, -2}, 0, 2, true, false, 0xFFFFFE00 },
      { new byte[] {3, 0}, 0, 2, true, false, 3 },
      { new byte[] {3, 2}, 0, 2, true, false, 0x0203 },
      { new byte[] {3, -2}, 0, 2, true, false, 0xFFFFFE03 },
      { new byte[] {-3, 0}, 0, 2, true, false, 0x00FD },
      { new byte[] {-3, 2}, 0, 2, true, false, 0x02FD },
      { new byte[] {-3, -2}, 0, 2, true, false, 0xFFFFFEFD },
      // not sign-extended, big endian
      { new byte[] {0, 0}, 0, 2, false, true, 0 },
      { new byte[] {0, 2}, 0, 2, false, true, 2 },
      { new byte[] {0, -2}, 0, 2, false, true, 0x00FE },
      { new byte[] {3, 0}, 0, 2, false, true, 0x0300 },
      { new byte[] {3, 2}, 0, 2, false, true, 0x0302 },
      { new byte[] {3, -2}, 0, 2, false, true, 0x03FE },
      { new byte[] {-3, 0}, 0, 2, false, true, 0xFD00 },
      { new byte[] {-3, 2}, 0, 2, false, true, 0xFD02 },
      { new byte[] {-3, -2}, 0, 2, false, true, 0xFDFE },
      // not sign-extended, little endian
      { new byte[] {0, 0}, 0, 2, false, false, 0 },
      { new byte[] {0, 2}, 0, 2, false, false, 0x0200 },
      { new byte[] {0, -2}, 0, 2, false, false, 0xFE00 },
      { new byte[] {3, 0}, 0, 2, false, false, 3 },
      { new byte[] {3, 2}, 0, 2, false, false, 0x0203 },
      { new byte[] {3, -2}, 0, 2, false, false, 0xFE03 },
      { new byte[] {-3, 0}, 0, 2, false, false, 0x00FD },
      { new byte[] {-3, 2}, 0, 2, false, false, 0x02FD },
      { new byte[] {-3, -2}, 0, 2, false, false, 0xFEFD },

      // Test indexing with in-bounds indices.  Include indices at the start,
      // middle, and end.  Use various values of endianness and signed.

      { new byte[] {0, 1, -2}, 0, 1, true, true, 0 },
      { new byte[] {0, 1, -2}, 1, 1, true, false, 1 },
      { new byte[] {0, 1, -2}, 2, 1, false, true, 0xFE },
      { new byte[] {0, 1, -2, -3, -4, 5}, 0, 2, true, true, 1 },
      { new byte[] {0, 1, -2, -3, -4, 5}, 2, 2, true, false, 0xFFFFFDFE },
      { new byte[] {0, 1, -2, -3, -4, 5}, 4, 2, false, false, 0x05FC },
    });
  }

  /**
   * Test of extractSample method, of class SoundUtils.
   */
  @Test
  public void testExtractSample() {
    int result = SoundUtils.extractSample(
            input, index, nBytes, signed, bigEndian );
    assertEquals( expResult, result );
  }
}
