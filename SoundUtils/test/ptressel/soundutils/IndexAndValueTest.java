package ptressel.soundutils;

import org.junit.Test;
import static org.junit.Assert.*;
import ptressel.soundutils.SoundUtils.IndexAndValue;

/**
 * Test the helper class IndexAndValue defined in class SoundUtils.
 * @see SoundUtils.IndexAndValue
 *
 * @author Patricia Tressel
 */
public class IndexAndValueTest {

  // Some comparands that are equal, or differ in index and/or value
  private IndexAndValue iv1 = new IndexAndValue( 10, 100.0 );
  private IndexAndValue iv1a = new IndexAndValue( 10, 100.0 );
  private IndexAndValue iv2 = new IndexAndValue( 9, 100.0 );
  private IndexAndValue iv3 = new IndexAndValue( 11, 100.0 );
  private IndexAndValue iv4 = new IndexAndValue( 10, 101.0 );
  private IndexAndValue iv5 = new IndexAndValue( 11, 101.0 );

  // Expected hashCode result for IndexAndValue( 10, 100.0 )
  private int iv1Hash = 1079574538;

  @Test
  public void testEquals() {
    System.out.println("IndexAndValue#testEquals");
    assertTrue( iv1.equals(iv1a) );
    assertFalse( iv1.equals(iv2) );
    assertFalse( iv1.equals(iv4) );
    assertFalse( iv1.equals(iv5) );
    // This test should use IndexAndValue#equals().
    assertEquals( iv1, iv1a );
  }

  @Test
  public void testHashCode() {
    System.out.println("IndexAndValue#testHashCode");
    assertEquals( iv1Hash, iv1.hashCode() );
  }

  @Test
  public void testCompareTo() {
    System.out.println("IndexAndValue#testCompareTo");
    // Note sort is *decreasing* by value, but *increasing* by index within
    // equal values.
    assertEquals( 0, iv1.compareTo(iv1a) );
    assertEquals( 1, iv1.compareTo(iv2) );
    assertEquals( -1, iv1.compareTo(iv3) );
    assertEquals( 1, iv1.compareTo(iv4) );
    assertEquals( 1, iv1.compareTo(iv5) );
    assertEquals( -1, iv2.compareTo(iv1) );
    assertEquals( 1, iv3.compareTo(iv1) );
    assertEquals( -1, iv4.compareTo(iv1) );
    assertEquals( -1, iv5.compareTo(iv1) );
  }
}
