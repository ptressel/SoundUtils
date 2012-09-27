package ptressel.soundutils;

/**
 * <p>A holder for a set of audio format values.
 *
 * <p>This one is 8000 Hz, 2 bytes, mono, signed, big endian.
 */
public class AudioFormatParams8000TwoBytesMonoSignedBig
    extends AudioFormatParams {

  public AudioFormatParams8000TwoBytesMonoSignedBig() {
    sampleRate = 8000;
    sampleBytes = 2;
    signed = true;
    bigEndian = true;
    channels = 1;
  }
}
