package ptressel.soundutils;

/**
 * <p>A holder for a set of audio format values.
 *
 * <p>This one is 16000 Hz, 1 byte, mono, signed, big endian.
 */
public class AudioFormatParams16000OneByteMonoSignedBig
    extends AudioFormatParams {

  public AudioFormatParams16000OneByteMonoSignedBig() {
    sampleRate = 16000;
    sampleBytes = 1;
    signed = true;
    bigEndian = true;
    channels = 1;
  }

  /** Be fanatic and check that we get what we expect. */
  public static void main( String[] args ) {
    AudioFormatParams16000OneByteMonoSignedBig selfRef
      = new AudioFormatParams16000OneByteMonoSignedBig();
    AudioFormatParams parentRef
      = new AudioFormatParams16000OneByteMonoSignedBig();
    AudioFormatParams parentClass = new AudioFormatParams();
    System.out.println( "With instance assigned to own reference type:" );
    System.out.println( "sampleRate = " + selfRef.sampleRate );
    System.out.println( "With instance assigned to parent reference type:" );
    System.out.println( "sampleRate = " + parentRef.sampleRate );
    System.out.println( "For contrast, parent instance & parent ref:" );
    System.out.println( "sampleRate = " + parentClass.sampleRate );
  }
}
