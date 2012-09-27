package ptressel.soundutils;

/**
 * <p>A holder for a set of audio format values.
 *
 * <p>Intent is that one should create a subclass of this for each set of
 * parameter values that resets these variables in its constructor, with a
 * class name indicatative of the settings.  Then this name can be passed in
 * from the command line, e.g. in SoundCopy or PlayWave, to change settings
 * without recompiling or having a huge long command line.
 *
 * <p>Values here are set to 48000, mono, 1 byte, signed, big endian.
 */
public class AudioFormatParams {

  // Yeah, they're public non-final instance variables.  You want
  // accessors, you write 'em...

  /** Sample rate (Hz) */
  public float sampleRate;
  /** Number of bytes per sample */
  public int sampleBytes;
  /** Is sample value signed or unsigned? */
  public boolean signed;
  /** Is sample value big Endian or small? */
  public boolean bigEndian;
  /** Number of channels (1 is mono, 2 is stereo) */
  public int channels;

  public AudioFormatParams() {
    sampleRate = 48000;
    sampleBytes = 1;
    signed = true;
    bigEndian = true;
    channels = 1;
  }

  @Override
  public String toString() {
    return "Sample rate = " + sampleRate + ", # bytes = " + sampleBytes +
           ", signed = " + signed + ", big endian = " + bigEndian;
  }
}
