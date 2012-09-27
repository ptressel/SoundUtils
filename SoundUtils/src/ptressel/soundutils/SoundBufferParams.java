package ptressel.soundutils;

/**
 * <p>A holder for a set of buffer parameters for SoundCopy, PlayWave,
 * and their input and output thread classes.  These include parameters
 * influencing the sizes of DataLine buffers and buffers used for read
 * and write.
 *
 * <p>Intent is that one should create a subclass of this for each set of
 * parameter values that resets these variables in its constructor, with a
 * class name indicatative of the settings.  Then this name can be passed in
 * from the command line, e.g. in SoundCopy or PlayWave, to change settings
 * without recompiling or having a huge long command line.
 *
 * <p>Default values set here are a lowest-common-denominator that will
 * likely get some recognizable sound out.
 */
public class SoundBufferParams {

  // Yeah, they're public non-final instance variables.  You want accessors,
  // you write 'em...

  /**
   * The minimum transfer buffer size, in frames.
   */
  public int minTransferBufferNFrames = 32;

  /**
   * Initial number of buffers in the buffer pool.  (So long as they're
   * small, get a lot.)
   */
  public int numInitialTransferBuffers = 50;

  /**
   * <p>Ratio of the low-level sound system buffer size to the read or write
   * buffer size, in samples.  The read/write buffers are the transfer queue
   * buffers.  E.g., to use transfer buffers one fourth as long as the internal
   * buffer, set this to 4.
   *
   * <p>The recommendation given in the Java Sound Programmer's Guide
   * (java.sun.com/j2se/1.5.0/docs/guide/sound/programmer_guide/chapter5.html)
   * is to use read/write buffers that are "some fraction of" the internal
   * buffer size, and their example uses 1/5.  Not wanting to have transfer
   * buffers that can't hold an integral number of samples, we will do our
   * calculation in terms of samples, not bytes.
   *
   * <p>Note the transfer buffer length should also be a submultiple of any
   * FFT or other DSP length, so choice of all these values may take some
   * tuning per platform.
   */
  public int internalToTransferBufferRatio = 4;

  /**
   * <p>Suggested size of each transfer buffer in samples (not bytes).  These
   * are the buffers that we pass to our DataLines either to read samples
   * into or write out samples from.  This is not the size of the underlying
   * sound system's internal buffers.
   *
   * <p>If the buffer size is too large, it may take so long to fill that the
   * output side will starve and there will be gaps or delays in the sound.
   * If it's too small, there'll be more overhead than necessary.  There is
   * probably some happy medium, but gaps are worse than overhead so long as
   * the system can handle the overhead, so set the buffer size to the
   * smallest that doesn't lead to excessive overhead.
   *
   * <p>Another advantage of using small buffers is that a higher level
   * sitting on top of a raw byte copier could decide, if they want to send a
   * message that doesn't come out an even multiple of the buffer size, just
   * to fill the trailing bytes with silence and ignore the wastage.
   *
   * <p>Yet one more argument in favor of small buffers is given here:
   * java.sun.com/j2se/1.5.0/docs/guide/sound/programmer_guide/chapter5.html
   * This warns that the read and write buffers must not be the same size
   * as the internal buffers, but rather, should be some fraction of the
   * size, else the mixer may need to fill/empty a read/write buffer while
   * we're busy processing it.  With small buffers, we can get our little
   * buffer handed off to a processing thread, and get another read/write
   * queued up, before the mixer drops too much.
   *
   * <p>One drawback of small read/write buffers is that they may be too
   * small to directly use for in any signal processing (e.g. FFT)
   * operations.  So a batch of read/write buffers may need to be copied
   * into an FFT input array.  This is not necessarily a waste -- it will
   * likely be necessary to marshall the sample bytes in each frame into
   * ints or doubles anyway.
   *
   * <p>Note if the size specified here is too large, it will be reduced
   * to the greater of minTransferBufferNFrames or the input internal buffer
   * size (in frames) / internalToTransferBufferRatio.
   */
  public int transferBufferNFrames = 1024;

  /**
   * <p>Suggested internal buffer size for input, specified as the number of
   * frames (for mono PCM, just the number of samples), not bytes.  This will
   * be multiplied by the frame size, then requested in the open to the
   * TargetDataLine.  If this value is set to AudioSystem.NOT_SPECIFIED then
   * no buffer size will be set in the open.
   *
   * <p>The value specified here will be constrained to the allowed range
   * of buffer sizes that the line says it can handle (gotten from its
   * DataLine.Info, which is gotten via getLineInfo).
   *
   * <p>The size of the internal buffers directly controls the delay in
   * receiving or playing out sound.  This would imply we should make them
   * as small as possible.  However, that can lead to too much overhead.
   * The value chosen here was selected purely based on not hearing much
   * delay.
   *
   * <p>Thanks to Raphael Hoffmann for uncovering the source of the delay in
   * propagating sound from input to output, namely, that the size of buffers
   * we pass in to a DataLine's read or write has nothing to do with the size
   * of the internal buffers it's using -- we can only affect those via the
   * size we request in the open.  Raphael's info is from:
   * http://www.jsresources.org/faq_performance.html
   */
  public int internalBufNFramesIn = 4096;

  /**
   * <p>Suggested internal buffer size for output, specified as the number of
   * frames (for mono PCM, just the number of samples), not bytes.  This will
   * be multiplied by the frame size, then requested in the open to the
   * SourceDataLine.  If this value is set to AudioSystem.NOT_SPECIFIED then
   * no buffer size will be set in the open.
   *
   * <p>The value specified here will be constrained to the allowed range
   * of buffer sizes that the line says it can handle (gotten from its
   * DataLine.Info, which is gotten via getLineInfo).
   *
   * <p>The size of the internal buffers directly controls the delay in
   * receiving or playing out sound.  This would imply we should make them
   * as small as possible.  However, that can lead to too much overhead.
   * The value chosen here was selected purely based on not hearing much
   * delay.
   *
   * <p>Thanks to Raphael Hoffmann for uncovering the source of the delay in
   * propagating sound from input to output, namely, that the size of buffers
   * we pass in to a DataLine's read or write has nothing to do with the size
   * of the internal buffers it's using -- we can only affect those via the
   * size we request in the open.  Raphael's info is from:
   * http://www.jsresources.org/faq_performance.html
   *
   * <p>Under Linux (kernel 2.6.12-2smp.1, JDK jdk1.5.0_04), if the output
   * buffer is too small and the sample rate too high, the output thread
   * won't be able to keep up with real time, and will gradually lag further
   * and further behind (yielding odd-sounding output!).  A sample rate of
   * 16KHz and output buffer size of 4096 samples was found to allow the
   * output to keep up.  Under Windows (precise JDK version unknown, but 1.5)
   * there is no problem with smaller buffers and higher sample rate.
   */
  public int internalBufNFramesOut = 4096;

  public SoundBufferParams() {
    // minTransferBufferNFrames = 32;
    // numInitialTransferBuffers = 50;
    // internalToTransferBufferRatio = 4;
    // transferBufferNFrames = 1024;
    // internalBufNFramesIn = 4096;
    // internalBufNFramesOut = 4096;
  }

  public String toString() {
    return "min # xfer buf frames = " + minTransferBufferNFrames +
           ", # init xfer bufs = " + numInitialTransferBuffers +
           ", ratio of int to xfer buf sizes = "
           + internalToTransferBufferRatio +
           ", suggested # xfer buf frames = " + transferBufferNFrames +
           ", suggested # int output buf frames = " + internalBufNFramesIn +
           ", suggested # int input buf frames = " + internalBufNFramesOut;
  }
}
