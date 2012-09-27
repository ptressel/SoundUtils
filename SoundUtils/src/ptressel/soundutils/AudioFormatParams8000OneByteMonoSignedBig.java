package ptressel.soundutils;

/**
 * <p>A holder for a set of audio format values.
 *
 * <p>This one is 8000 Hz, 1 byte, mono, signed, big endian.
 */
public class AudioFormatParams8000OneByteMonoSignedBig
    extends AudioFormatParams {

  public AudioFormatParams8000OneByteMonoSignedBig() {
    sampleRate = 8000;
    sampleBytes = 1;
    signed = true;
    bigEndian = true;
    channels = 1;
  }
}
