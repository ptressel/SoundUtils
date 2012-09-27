package ptressel.soundutils;

/**
 * <p>A holder for a set of buffer parameters for SoundCopy, PlayWave,
 * and their input and output thread classes.  These include parameters
 * influencing the sizes of DataLine buffers and buffers used for read
 * and write.
 *
 * <p>This set worked fine under Windows, with minimal latency and no ticks
 * or pops.
 *
 * @see SoundBufferParams for descriptions of instance variables.
 */
public class SoundBufferParamsWindows extends SoundBufferParams {

  public SoundBufferParamsWindows() {
    minTransferBufferNFrames = 32;
    numInitialTransferBuffers = 50;
    internalToTransferBufferRatio = 4;
    transferBufferNFrames = 256;
    internalBufNFramesIn = 1024;
    internalBufNFramesOut = 1024;
  }
}
