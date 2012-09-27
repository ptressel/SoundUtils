package ptressel.soundutils;

import javax.sound.sampled.*;

/**
 * <p>A holder for a set of buffer parameters for SoundCopy, PlayWave,
 * and their input and output thread classes.  These include parameters
 * influencing the sizes of DataLine buffers and buffers used for read
 * and write.
 *
 * <p>This set is for Linux with ALSA, Java 1.5.  Give up and let the
 * poor thing have its maximal output buffer.  This has a large latency,
 * but helps avoid the ticks and pops due to draining the output buffer.
 *
 * @see SoundBufferParams for descriptions of instance variables.
 */
public class SoundBufferParamsLinuxDefaultOutput extends SoundBufferParams {

  public SoundBufferParamsLinuxDefaultOutput() {
    minTransferBufferNFrames = 32;
    numInitialTransferBuffers = 50;
    internalToTransferBufferRatio = 4;
    transferBufferNFrames = 4096;
    internalBufNFramesIn = 16384;
    internalBufNFramesOut = AudioSystem.NOT_SPECIFIED;
  }
}
