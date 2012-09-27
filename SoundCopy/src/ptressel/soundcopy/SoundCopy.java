package ptressel.soundcopy;

import java.io.*;
import javax.sound.sampled.*;
import java.util.concurrent.*;
import ptressel.soundutils.*;

/**
 * <p>Copy sound from the capture source (e.g. mic) to whatever playback
 * devices are turned on (e.g. the speakers).
 *
 * <p>SoundCopy provides a synchronized queue to a producer thread that
 * reads from the capture source, and a consumer thread that sends to the
 * output(s).
 *
 * <p>This code does not attempt to unmute devices, control the volume
 * or change the capture device.  So you may need to use your system's mixer
 * to un-mute the microphone, set it as the capture source, and turn up its
 * volume.  Caution:  Be sure to turn off (i.e. mute) playback for at least
 * the mic and CD before you make the mic the capture device.  Otherwise, you
 * may fry your sound card!
 *
 * <p>Usage:<br>
 *   java SoundCopy
 *
 * <p>Thanks to Ben Lerner for the warning about not turning on mic *capture*
 * while mic or CD *playback* are on!
 *
 * @author Patricia Tressel
 */

public class SoundCopy {

  // Instance data

  // Debugging info -- set or computed here and passed on to the input
  // and output threads

  /** Want debugging statistics printed? */
  private boolean debug = true;

  /** Want msgs every time the output thread might block? */
  private boolean debugBlocking = false;

  /** Print statistics every statTimeInterval seconds. */
  private double statTimeInterval = 2;

  /**
   * Print statistics every statLoopInterval passes through the input and
   * output thread's run loops.  This will be set based on statTimeInterval,
   * the sample frequency specified for the audio format, and the transfer
   * buffer size.
   */
  private int statLoopInterval;

  /**
   * Forgetting rate for statistics.  I.e. for moving averages,
   * forgetting rate * old average is kept, and ( 1 - forgetting rate ) *
   * new buffer's average is included.
   * This absolves us from maintaining a real average, for which we'd need
   * the total count of bytes seen or loop passes -- that could overflow.
   * The forgetting rate per buffer is computed from the frequency, etc.
   * to provide the desired forgetting rate per statistics display
   * interval, statIntervalForgettingRate.
   */
  private double statForgettingRate;

  /**
   * The net forgetting rate over one statTimeInterval.  Each loop pass
   * contributes a factor of statForgettingRate, so have:
   * statIntervalForgettingRate = statForgettingRate ^ statLoopInterval
   */
  private double statIntervalForgettingRate = .25;

  // Values set by user at compile time, and not modified at instance
  // construction time.

  /**
   * The minimum transfer buffer size, in frames.
   */
  private int minTransferBufferNFrames = 32;

  /**
   * Initial number of buffers in the buffer pool.  (So long as they're
   * small, get a lot.)
   */
  private int numInitialTransferBuffers = 50;

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
  private int internalToTransferBufferRatio = 4;

  // Default audio format choices:
  //
  // Currently no distinction is made between capture and playback settings --
  // same used for both.  Note that although we select an endianness here, we
  // make no use of it as we're just going to pass it through unmodified.  If
  // the input and output endianness are made different, some conversion may
  // be needed between input and output.  If extensive conversion or other
  // processing must be done, it should be done outside of the input and
  // output threads, to avoid dropping input data, or starving the output.
  //
  // Besides the values shown here or set via the constructor, we are going to
  // choose PCM so that we do not need to mess with any fancy decoding.  A PCM
  // frame does not have any "header" info -- it just contains the sample(s).
  // And if we stick with mono, we'll only have one sample per frame.  The
  // words "sample" and "frame" are thus used somewhat interchangeably here.

  /** Sample rate (Hz) */
  private float sampleRate = 16000;
  /** Number of bytes per sample */
  private int sampleBytes = 2;
  /** Is sample value signed or unsigned? */
  private boolean signed = true;
  /** Is sample value big Endian or small? */
  private boolean bigEndian = true;
  /** Number of channels (1 is mono, 2 is stereo) */
  private int channels = 1;

  // Info to be set by user at compile time, that may be modified at
  // construction time.  (Note these should be passed in via the command
  // line.)

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
  private int transferBufferNFrames = 1024;

  /**
   * Actual transfer buffer size in bytes, after all adjustments due to
   * internal input buffer size, sample size, etc.
   */
  private int transferBufferSize;

  /**
   * <p>Internal buffer size for input, specified as the number of frames
   * (for mono PCM, just the number of samples), not bytes.  This will be
   * multiplied by the frame size, then requested in the open to the
   * TargetDataLine.
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
  private int internalBufNFramesIn = 4096;

  /**
   * <p>Internal buffer size for output, specified as the number of frames
   * (for mono PCM, just the number of samples), not bytes.  This will be
   * multiplied by the frame size, then requested in the open to the
   * SourceDataLine.
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
  private int internalBufNFramesOut = 4096;

  // Internal state

  /**
   * Audio format for the user's capture settings, with the default encoding
   * (PCM).
   */
  private AudioFormat formatIn;

  /**
   * Audio format for the user's output settings, with the default encoding
   * (PCM).
   */
  private AudioFormat formatOut;

  // The buffer and frame sizes are not currently used outside of the
  // constructor, so could be made local variables there.  They are here
  // in case someone wants to add accessors.

  // Frame size is gotten from the AudioFormats via getFrameSize.
  // It is fixed due to our choice of PCM and mono.

  /** Frame size for input, in bytes. */
  private int frameSizeIn;

  /** Frame size for output, in bytes. */
  private int frameSizeOut;

  // Max and min internal buffer sizes are gotten from our DataLines'
  // DataLine.Infos.

  /** Max internal buffer size for input, in bytes. */
  private int maxInternalBufferSizeIn;

  /** Min internal buffer size for input, in bytes. */
  private int minInternalBufferSizeIn;

  /** Max internal buffer size for output, in bytes. */
  private int maxInternalBufferSizeOut;

  /** Min internal buffer size for output, in bytes. */
  private int minInternalBufferSizeOut;

  // Note "target" and "source" in Java Sound jargon are from the point of
  // view of the mixer, especially, the "mixers" in the sound card.  E.g.
  // the capture mixer reads from the capture source (an actual device) and
  // writes to the capture target, which is what we read from.

  /** Capture -- presumably the mic. */
  private TargetDataLine input;

  /** Playback -- presumably line out. */
  private SourceDataLine output;

  /** Capture thread */
  private SoundIn soundIn;

  /** Playback thread */
  private SoundOut soundOut;

  /**
   * Buffer pool:  Input side gets buffers to fill from here unless there are
   * no more, in which case it allocates one.  Output side puts spent buffers
   * back in.  If all goes well, the length of this queue should not grow
   * indefinitely.  The intent of the buffer pool is to reuse buffers rather
   * than allocating new ones for each input, then throwing them away.
   * Avoiding object creation and destruction is usually the most significant
   * optimization in this sort of application.  The buffer pool is prefilled
   * with a fair number of buffers.
   */
  private LinkedBlockingQueue<byte[]> transferBufferPool
    = new LinkedBlockingQueue<byte[]>();

  /**
   * Transfer queue:  Input side puts filled buffers on one end.  Output
   * side dequeues them from the other.
   */
  private LinkedBlockingQueue<byte[]> transferQueue
    = new LinkedBlockingQueue<byte[]>();

  /** Share one instance of a reader on System.in with our main. */
  BufferedReader in;

  // Constructor -- note really should be more specific about what
  // exceptions this throws.

  /**
   * Create a SoundCopy:  Get the input and output lines with the desired
   * format.  Note we do (almost) all the audio setup here so we know right
   * away if it's not going to work.  Make the producer and consumer threads
   * but don't start them.
   *
   * @param in a reader for System.in that we share with main
   */
  public SoundCopy( BufferedReader in ) throws Exception {

    // No debugging.
    debug = false;
    debugBlocking = false;

    // Call init.
    init( in );
  }

  /**
   * Create a SoundCopy:  Get the input and output lines with the desired
   * format.  Note we do (almost) all the audio setup here so we know right
   * away if it's not going to work.  Make the producer and consumer threads
   * but don't start them.
   *
   * @param in a reader for System.in that we share with main
   * @param formatParams the desired audio format
   */
  public SoundCopy( BufferedReader in,
                    AudioFormatParams formatParams,
                    SoundBufferParams bufferParams,
                    boolean debug, boolean debugBlocking ) throws Exception {

    // Unpack the values from our AudioFormatParams.
    sampleRate = formatParams.sampleRate;
    sampleBytes = formatParams.sampleBytes;
    signed = formatParams.signed;
    bigEndian = formatParams.bigEndian;
    channels = formatParams.channels;

    // Unpack the buffer params.
    minTransferBufferNFrames = bufferParams.minTransferBufferNFrames;
    numInitialTransferBuffers = bufferParams.numInitialTransferBuffers;
    internalToTransferBufferRatio
      = bufferParams.internalToTransferBufferRatio;
    transferBufferNFrames = bufferParams.transferBufferNFrames;
    internalBufNFramesIn = bufferParams.internalBufNFramesIn;
    internalBufNFramesOut = bufferParams.internalBufNFramesOut;

    // Debug options.
    this.debug = debug;
    this.debugBlocking = debugBlocking;

    // Call init.
    init( in );
  }

  /**
   * Initialize a SoundCopy:  Get the input and output lines with the desired
   * format.  Note we do (almost) all the audio setup here so we know right
   * away if it's not going to work.  Make the producer and consumer threads
   * but don't start them.
   *
   * @param in a reader for System.in that we share with main
   */
  private void init( BufferedReader in ) throws Exception {

    // Save the reader that someone else has already opened.  (Note it has
    // in the past been a problem for one process to open several readers
    // on System.in.)
    this.in = in;

    // Now that we have the audio format settings, make the AudioFormats.
    formatIn = new AudioFormat( sampleRate, sampleBytes * Byte.SIZE, channels,
                                signed, bigEndian );
    formatOut = new AudioFormat( sampleRate, sampleBytes * Byte.SIZE, channels,
                                 signed, bigEndian );
    frameSizeIn = formatIn.getFrameSize();
    frameSizeOut = formatOut.getFrameSize();

    // Get the input line, using the specified audio format.  We don't try
    // to select the capture source -- we depend on whatever the user has
    // set up.  Open the line.  Keep fingers crossed...
    try {
      input = AudioSystem.getTargetDataLine( formatIn );
    }
    catch( Exception e ) {
      System.out.println( "SoundCopy: getTargetDataLine threw exception "
        + e.getClass().getName() );
      System.out.println( "SoundCopy: message: " + e.getMessage() );
      throw e;
    }

    // Get the limits on the internal buffer size, and constrain our
    // requested size to that.  Requested size is in frames, so need frame
    // size for conversion.
    DataLine.Info infoIn;
    try {
      infoIn = (DataLine.Info) input.getLineInfo();
    }
    catch( Exception e ) {
      System.out.println( "SoundCopy: getLineInfo threw exception "
        + e.getClass().getName() );
      System.out.println( "SoundCopy: message: " + e.getMessage() );
      throw e;
    }
    if ( debug ) {
      System.out.println( "Info for input line is:" );
      System.out.println( infoIn );
    }

    // Get max & min buffer sizes, in bytes.
    maxInternalBufferSizeIn = infoIn.getMaxBufferSize();
    minInternalBufferSizeIn = infoIn.getMinBufferSize();
    if ( debug ) {
      System.out.println(
        "Limits, in bytes, of internal input buffers are " +
        minInternalBufferSizeIn + " to " + maxInternalBufferSizeIn + "." );
      System.out.println(
        "Frame size in bytes is " + frameSizeIn );
      System.out.println(
        "User requested internal input buffers of " + internalBufNFramesIn +
        " frames." );
    }

    // Convert to samples.  There had *better* be an even number of frames
    // in the buffer sizes that the system gives us (unless the value is
    // AudioSystem.NOT_SPECIFIED)!
    int maxInternalBufNFramesIn = maxInternalBufferSizeIn / frameSizeIn;
    int minInternalBufNFramesIn = minInternalBufferSizeIn / frameSizeIn;

    // If the user says they don't want to specify the input buffer size,
    // leave it alone.  Otherwise, make sure the requested buffer size is
    // within the limits.  Ignore limits that are AudioSystem.NOT_SPECIFIED
    // or zero.  Note we compare in number of samples/frames not bytes in
    // case the frames have non-sample info.
    int internalBufNBytesIn = AudioSystem.NOT_SPECIFIED;
    if ( internalBufNFramesIn != AudioSystem.NOT_SPECIFIED ) {
      if ( maxInternalBufferSizeIn != AudioSystem.NOT_SPECIFIED
           && maxInternalBufferSizeIn != 0
           && internalBufNFramesIn > maxInternalBufNFramesIn ) {
        internalBufNFramesIn = maxInternalBufNFramesIn;
        System.out.println(
          "Reducing size of input internal buffers requested to maximum.");
      } else if ( minInternalBufferSizeIn != AudioSystem.NOT_SPECIFIED
           && minInternalBufferSizeIn != 0
           && internalBufNFramesIn < minInternalBufNFramesIn ) {
        internalBufNFramesIn = minInternalBufNFramesIn;
        System.out.println(
          "Increasing size of input internal buffers requested to minimum.");
      }
      internalBufNBytesIn = internalBufNFramesIn * frameSizeIn;
    }

    // We must specify the internal buffer size here, else the underlying
    // sound system will use whatever its pleases, which (as was uncovered
    // by Raphael Hoffmann) may be huge.  This is independent of the size
    // of the buffers we give to the DataLine's read method -- the buffer
    // parameter in the open applies to low-level internal buffers.  However,
    // if the user says NOT_SPECIFIED, we don't specify it.  The docs for
    // TargetDataLine's open( format, bufferSize ) say nothing about letting
    // us send in AudioSystem.NOT_SPECIFIED for the buffer size, so we use
    // the other form of open in that case.
    try {
      if ( internalBufNBytesIn != AudioSystem.NOT_SPECIFIED )
        input.open( formatIn, internalBufNBytesIn );
      else
        input.open( formatIn );
      if ( debug ) {
        System.out.println( "Requested input buffer size " +
          internalBufNBytesIn + " bytes, got " + input.getBufferSize() );
      }
    }
    catch( Exception e ) {
      System.out.println( "SoundCopy: open of input line threw exception "
        + e.getClass().getName() );
      System.out.println( "SoundCopy: message: " + e.getMessage() );
      throw e;
    }

    // Do the same for the output.  Here, we depend even more on the user's
    // manipulation of the system mixer settings for the sound card.  We
    // make no attempt to alter the settings.  User has been warned about
    // not having mic and CD playback on when mic capture is on...
    try {
      output = AudioSystem.getSourceDataLine( formatOut );
    }
    catch( Exception e ) {
      System.out.println( "SoundCopy: getSourceDataLine threw exception "
        + e.getClass().getName() );
      System.out.println( "SoundCopy: message: " + e.getMessage() );
      throw e;
    }

    // Get the limits on the internal buffer size, and constrain our
    // requested size to that.  Requested size is in frames, so need frame
    // size for conversion.
    DataLine.Info infoOut;
    try {
      infoOut = (DataLine.Info) output.getLineInfo();
    }
    catch( Exception e ) {
      System.out.println( "SoundCopy: getLineInfo threw exception "
        + e.getClass().getName() );
      System.out.println( "SoundCopy: message: " + e.getMessage() );
      throw e;
    }
    if ( debug ) {
      System.out.println( "Info for output line is:" );
      System.out.println( infoOut );
    }

    // Get max & min buffer sizes, in bytes.
    maxInternalBufferSizeOut = infoOut.getMaxBufferSize();
    minInternalBufferSizeOut = infoOut.getMinBufferSize();
    if ( debug ) {
      System.out.println(
        "Limits, in bytes, of internal output buffers are " +
        minInternalBufferSizeOut + " to " + maxInternalBufferSizeOut + "." );
      System.out.println(
        "Frame size in bytes is " + frameSizeOut );
      System.out.println(
        "User requested internal output buffers of " + internalBufNFramesOut +
        " frames." );
    }

    // Convert to samples.  There had *better* be an even number of frames
    // in the buffer sizes that the system gives us (unless the value is
    // AudioSystem.NOT_SPECIFIED)!
    int maxInternalBufNFramesOut = maxInternalBufferSizeOut / frameSizeOut;
    int minInternalBufNFramesOut = minInternalBufferSizeOut / frameSizeOut;

    // If the user says they don't want to specify the output buffer size,
    // leave it alone.  Otherwise, make sure the requested buffer size is
    // within the limits.  Ignore limits that are AudioSystem.NOT_SPECIFIED
    // or zero.  Note we compare in number of samples/frames not bytes in
    // case the frames have non-sample info.
    int internalBufNBytesOut = AudioSystem.NOT_SPECIFIED;
    if ( internalBufNFramesOut != AudioSystem.NOT_SPECIFIED ) {
      if ( maxInternalBufferSizeOut != AudioSystem.NOT_SPECIFIED
           && maxInternalBufferSizeOut != 0
           && internalBufNFramesOut > maxInternalBufNFramesOut ) {
        internalBufNFramesOut = maxInternalBufNFramesOut;
        System.out.println(
          "Reducing size of output internal buffers requested to maximum.");
      } else if ( minInternalBufferSizeOut != AudioSystem.NOT_SPECIFIED
           && minInternalBufferSizeOut != 0
           && internalBufNFramesOut < minInternalBufNFramesOut ) {
        internalBufNFramesOut = minInternalBufNFramesOut;
        System.out.println(
          "Increasing size of output internal buffers requested to minimum.");
      }
      internalBufNBytesOut = internalBufNFramesOut * frameSizeOut;
    }

    // If it's been left open somehow, we won't be able to open it.
    // This actually happened...  One possible cause:  A JVM process was
    // observed to be running under the same username as had been used to
    // run SoundCopy.  That JVM was still up after SoundCopy exited
    // abnormally, without doing a close.  It was servicing mozilla, as
    // mozilla died when the JVM was killed.  If that JVM was also used
    // for the SoundCopy run, then it may not yet have garbage-collected
    // the old SoundCopy when the new one was started up.  At that time,
    // the close was in SoundCopy's finalize.  The JVM is under no
    // obligation to run the garbage collector (even if we do System.gc()),
    // and thus our objects' finalize methods, until it's forced to it by
    // running out of memory or exiting.  This means finalize is no good
    // as a means of shutting down.  In another case, no JVM was observed,
    // and shutting mozilla down didn't free up the line.  In that case,
    // the system had to be rebooted.
    if ( output.isOpen() ) {
      System.out.println( "SoundCopy: Found output line already open." );
    }

    // If the user says NOT_SPECIFIED, we don't specify it.  The docs for
    // SourceDataLine's open( format, bufferSize ) say nothing about letting
    // us send in AudioSystem.NOT_SPECIFIED for the buffer size, so we use
    // the other form of open in that case.
    try {
      if ( internalBufNBytesOut != AudioSystem.NOT_SPECIFIED )
        output.open( formatOut, internalBufNBytesOut );
      else
        output.open( formatOut );
      if ( debug ) {
        System.out.println( "Requested output buffer size " +
          internalBufNBytesOut + " bytes, got " + output.getBufferSize() );
      }
    }
    catch( Exception e ) {
      System.out.println( "SoundCopy: open of output line threw exception "
        + e.getClass().getName() );
      System.out.println( "SoundCopy: message: " + e.getMessage() );
      throw e;
    }

    // If we get here, we should have a input and output lines -- whew!
    if ( debug ) {
      System.out.println( "Input line is:" );
      System.out.println( input );
      System.out.println( "Output line is:" );
      System.out.println( output );
    }

    // The transfer queue and buffer pool are already created.  Put some
    // buffers in the pool.  First need to figure out an appropriate size
    // range that will play well with the internal buffer sizes.
    // The recommendation given here:
    // java.sun.com/j2se/1.5.0/docs/guide/sound/programmer_guide/chapter5.html
    // is to use read buffers that are "some fraction of" the internal input
    // buffer size, and their example uses 1/5.  Not wanting to have transfer
    // buffers that can't hold an integral number of examples, we do our
    // calculation in terms of samples, not bytes.  The programmer's guide
    // implies that it's more important to keep the input side happy, so we
    // only check the input buffer size ratio here.

    // Start with the number of samples based on the requested ratio to the
    // internal input buffer.
    if ( debug ) {
      System.out.print( "Requested transfer buffers of "
        + ( transferBufferNFrames * sampleBytes ) + "bytes " );
    }
    int provisionalTransferNFrames
      = internalBufNFramesIn / internalToTransferBufferRatio;
    // If that's smaller than the requested transfer buffer size, use it
    // instead.
    if ( provisionalTransferNFrames < transferBufferNFrames ) {
      transferBufferNFrames = provisionalTransferNFrames;
    }
    // But don't go below the requested minimum.
    if ( transferBufferNFrames < minTransferBufferNFrames ) {
      transferBufferNFrames = minTransferBufferNFrames;
    }
    // Record the number of bytes we're going to use.
    transferBufferSize = transferBufferNFrames * sampleBytes;
    // Confused yet?
    if ( debug ) {
      System.out.println( ", got " + transferBufferSize );
    }

    for ( int i = 0; i < numInitialTransferBuffers; i++ ) {
      byte[] buffer = new byte[ transferBufferSize ];
      transferBufferPool.put( buffer );
    }

    // Now that we know the real transfer buffer size, fill in the debugging
    // criteria (used by the input and output threads).
    // Calculate the number of loop passes at the sampling rate to give
    // the right time interval.  Both input and output process one transfer
    // queue buffer per loop pass.
    statLoopInterval
      = (int) Math.ceil( sampleRate * statTimeInterval / transferBufferNFrames);
    // Set a forgetting rate that will retain a reasonable memory of a good
    // fraction of the given time interval.  We're going to multiply by the
    // forgetting rate on every loop pass, so the net rate over one time
    // interval is statForgettingRate ^ statLoopInterval.  Make that come out
    // to, oh, say, a quarter.
    statForgettingRate
      = Math.exp( Math.log( statIntervalForgettingRate ) / statLoopInterval );
    if ( debug ) {
      System.out.println(
        "SoundCopy: forgetting rate per pass = " + statForgettingRate );
    }

    // Make our in and out threads.  We pass both of them the transfer queue
    // and buffer pool, along with their separate lines, debug info, etc.
    soundIn = new SoundIn( input, transferQueue, transferBufferPool,
                           transferBufferSize, debug, debugBlocking,
                           statLoopInterval, statForgettingRate,
                           sampleBytes, signed, bigEndian );
    soundOut = new SoundOut( output, transferQueue, transferBufferPool,
                             debug, debugBlocking,
                             statLoopInterval, statForgettingRate,
                             sampleBytes, signed, bigEndian );
  }

  // Methods

  /** Tell the in and out threads to start, then wait for user to stop us. */
  public void copy() {
    // Start the out side first so it doesn't run out of data and have gaps
    // in the output.  The input side seems to get ahead anyway -- it manages
    // to get hundreds of buffers in the queue right away.  This causes a
    // delay of about half a second between the input and output sounds, but
    // there is no apparent loss.  Giving the output side extra time to get
    // started makes no difference.
    soundOut.start();
    // Give the output thread time to get ready.
    try {
      Thread.sleep(1000);
    }
    catch( InterruptedException e ) {
      // Don't care.
    }
    soundIn.start();

    // Ask user to tell us when to stop.  This is the poor man's wait-until-
    // interrupted.  Replace this with something nicer (e.g. a gui with a
    // quit button, or maybe just sleep til ^C).
    System.out.print( "Type return to quit: " );
    // Block til user types return.
    try {
      String reply = in.readLine();
    }
    catch( IOException e ) {
      // Excuse me???
      System.out.println( "SoundCopy: readLine threw an IOException." );
      // Fall out and quit anyway.
    }

    // Shut down the input side first, so it doesn't run out of buffers and
    // start uselessly allocating them.
    if ( soundIn != null ) soundIn.quit();
    if ( soundOut != null ) soundOut.quit();
  }

  /** Create a SoundCopy and start it. */
  public static void main( String[] args ) {

    // Read args.
    if ( SoundUtils.checkOption( args, "-h" ) ||
         SoundUtils.checkOption( args, "-help" ) ) {
      // User just wants usage info.
      System.out.println( "SoundCopy usage:" );
      System.out.println( "  java SoundCopy <options>" );
      System.out.println( "Options:" );
      System.out.println( "  -h or -help, print this message" );
      System.out.println( "  -d, turn on debugging messages" );
      System.out.println(
        "  -dblock, turn on detailed debugging messages re. blocking" );
      System.out.println(
        "  -af <subclass of AudioFormatParams>, to specifiy audio format" );
      System.out.println(
        "  -af <subclass of SoundBufferParams>, to control buffer sizes" );
      System.exit(1);
    }
    String audioFormatParamsName = SoundUtils.getArgForOption( args, "-af" );
    String bufferParamsName = SoundUtils.getArgForOption( args, "-buf" );
    boolean debug = SoundUtils.checkOption( args, "-d" );
    boolean debugBlocking = SoundUtils.checkOption( args, "-dblock" );

    // Find the audio format params, if any.
    Class formatParamsClass = null;
    AudioFormatParams formatParams = null;
    if ( audioFormatParamsName != null ) {
      try {
        formatParamsClass = Class.forName( audioFormatParamsName );
        formatParams = (AudioFormatParams) formatParamsClass.newInstance();
      }
      catch( Exception e ) {
        System.out.println(
          "Couldn't find or create the specified AudioFormatParams: "
          + audioFormatParamsName );
        System.out.println( "Got exception: " + e );
        System.out.println( "Message: " + e.getMessage() );
        System.exit(0);
      }
    }

    // Use a default if we didn't get any audio params from the
    // command line.
    if ( formatParams == null ) {
      formatParams = new AudioFormatParams8000TwoBytesMonoSignedBig();
    }
    if ( debug ) {
      System.out.println( "Audio format params:" );
      System.out.println( formatParams );
    }

    //double sampleRate = formatParams.sampleRate;
    //int sampleBytes = formatParams.sampleBytes;
    //boolean signed = formatParams.signed;
    //boolean bigEndian = formatParams.bigEndian;
    //int channels = formatParams.channels;

    // Find the buffer params.
    Class bufferParamsClass = null;
    SoundBufferParams bufferParams = null;
    if ( bufferParamsName != null ) {
      try {
        bufferParamsClass = Class.forName( bufferParamsName );
        bufferParams = (SoundBufferParams) bufferParamsClass.newInstance();
      }
      catch( Exception e ) {
        System.out.println(
          "Couldn't find or create the specified SoundBufferParams: "
          + bufferParamsName );
        System.out.println( "Got exception: " + e );
        System.out.println( "Message: " + e.getMessage() );
        System.exit(0);
      }
    }

    // Use an os-specifid default if we didn't get any buffer params
    // from the command line.
    String osName = null;
    if ( bufferParams == null ) {
      // What OS is this?
      osName = System.getProperty( "os.name" );
      if ( debug ) System.out.println( "OS name is: " + osName );
      if ( osName.toLowerCase().contains( "linux" ) )
        bufferParams = new SoundBufferParamsLinuxDefaultOutput();
      else if ( osName.toLowerCase().contains( "windows" ) )
        bufferParams = new SoundBufferParamsWindows();
      else
        bufferParams = new SoundBufferParams();  // default
    }
    if ( debug ) {
      System.out.println( "Buffer params:" );
      System.out.println( bufferParams );
    }

    // Set up to get a reply from the user.
    BufferedReader in
      = new BufferedReader( new InputStreamReader( System.in ) );

    // Beware!  Must turn off playback for mic, CD *before* turning on mic
    // capture!!
    System.out.println(
     "Before proceeding, please:" );
    System.out.println(
     "Turn off playback for mic and CD.");
    System.out.println(
     "(Do this before setting the microphone as the capture device.)" );
    System.out.println(
     "For playback, unmute speakers, master volume, wave/PCM," );
    System.out.println(
     "and turn up all their volumes." );
    System.out.println(
     "Set the microphone as the capture device and turn up the capture level.");

    System.out.print( "Type return when ready: " );
    // Block til user types return.
    try {
      String reply = in.readLine();
    }
    catch( IOException e ) {
      // Excuse me???
      System.out.println( "SoundCopy: readLine threw an IOException." );
      System.exit(0);
    }

    // Make an instance.  Give it the reader for System.in that we already
    // have.  This is a temporary kluge until we have a nicer way for the
    // user to interrupt the copier.
    SoundCopy copier = null;
    try {
      copier = new SoundCopy( in, formatParams, bufferParams,
                              debug, debugBlocking );
    }
    catch( Exception e ) {
      System.out.println(
        "SoundCopy: Setup failed with exception " + e.getClass().getName() );
      System.out.println( "SoundCopy: message: " + e.getMessage() );
      System.exit(0);
    }

    // Start the in and out run loops.  Wait til user interrupts.
    copier.copy();
  }
}
