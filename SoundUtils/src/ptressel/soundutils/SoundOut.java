package ptressel.soundutils;

import javax.sound.sampled.*;
import java.util.concurrent.*;

/**
 * Get data from the transfer queue and write it to the output.
 *
 * @see SoundCopy for more info.
 */

public class SoundOut extends Thread {

  // Instance data

  /** Flag that tells us when to quit. */
  private boolean quitFlag = false;

  /** Output line */
  private final SourceDataLine output;

  /** Transfer queue */
  private final LinkedBlockingQueue<byte[]> transferQueue;

  /** Buffer pool */
  private final LinkedBlockingQueue<byte[]> bufferPool;

  // Debugging info -- the audio format info is used in collecting
  // data shown in debug messages

  /** Want debugging statistics printed? */
  private boolean debug;

  /** Want msgs every time the output thread might block? */
  private boolean debugBlocking;

  /** Print debug stats after this many loop passes */
  private int statLoopInterval;

  /** Forgetting rate for moving averages */
  private double statForgettingRate;

  /** Audio format: Bytes per sample */
  private int sampleBytes;

  /** Audio format: True if signed */
  private boolean signed;

  /** Audio format: True if big endian */
  private boolean bigEndian;

  /** Counter to tell when to print debugging statistics. */
  private int statCtr = 0;

  /** Moving average value of bytes received. */
  private double statAvgValue = 0;

  /** Moving average of transfer queue length. */
  private double statAvgQueueLen = 0;

  // Constructor

  /**
   * Make a SoundOut thread.
   *
   * @param output the output line to which to write data
   * @param transferQueue the queue from which full buffers will be obtained
   * @param bufferPool queue into which empty buffers will be placed
   */
  public SoundOut( SourceDataLine output,
                   LinkedBlockingQueue<byte[]> transferQueue,
                   LinkedBlockingQueue<byte[]> bufferPool ) {

    this.output = output;
    this.transferQueue = transferQueue;
    this.bufferPool = bufferPool;

    this.debug = false;
    this.debugBlocking = false;
  }

  /**
   * Make a SoundOut thread with the given debug options, including audio
   * format info.
   *
   * @param debug want debug messages?
   * @param debugBlocking want grossly excessive messages around operations
   * that might block?
   * @param statLoopInterval print debug stats after this many loop passes
   * @param statForgettingRate fraction of old "average" to keep in moving
   * averages
   * @param sampleBytes audio format: bytes per sample
   * @param signed audio format: true if signed
   * @param bigEndian audio format: true if big endian
   *
   * @param output the output line to which to write data
   * @param transferQueue the queue from which full buffers will be obtained
   * @param bufferPool queue into which empty buffers will be placed
   */
  public SoundOut( SourceDataLine output,
                   LinkedBlockingQueue<byte[]> transferQueue,
                   LinkedBlockingQueue<byte[]> bufferPool,
                   boolean debug, boolean debugBlocking, int statLoopInterval,
                   double statForgettingRate, int sampleBytes,
                   boolean signed, boolean bigEndian ) {

    // Standard setup.
    this( output, transferQueue, bufferPool );

    // Now set the debug options.
    this.debug = debug;
    this.debugBlocking = debugBlocking;
    this.statLoopInterval = statLoopInterval;
    this.statForgettingRate = statForgettingRate;
    this.sampleBytes = sampleBytes;
    this.signed = signed;
    this.bigEndian = bigEndian;
  }

  // Thread methods

  /**
   * The run loop:  Get a buffer from the transfer queue -- block til one is
   * available.  Write it to the output.  Put the (spent) buffer in the buffer
   * pool.  Repeat until our quit flag goes high.
   */
  @Override
  public void run() {

    // The buffer we're moving from transfer queue to output.
    byte[] buffer = null;

    // Turn the line on.  If we can't, we quit.
    try {
      on();
    }
    catch( Exception e ) {
      quitFlag = true;
    }

    runLoop:
    while(true) {
      // First check if we've been told to quit.
      if ( quitFlag ) break runLoop;

      // Get a buffer from the transfer queue.  The take method blocks until
      // an item is available.
      takeLoop:
      while(true) {
        if ( debugBlocking ) {
          buffer = null;
          System.out.println( "SoundOut: about to take from transfer queue." );
        }
        try {
          buffer = transferQueue.take();
          if ( debugBlocking ) {
            System.out.println( "SoundOut: got "
              + ( buffer == null ? "null" : "something" ) );
          }
          break takeLoop;
        }
        catch( InterruptedException e ) {
          if ( debugBlocking ) {
            System.out.println( "SoundOut: interrupted in takeLoop" );
          }
          // Check for quit.
          if ( quitFlag ) break runLoop;
        }
      }

      // Write to the output.
      if ( debugBlocking ) {
        System.out.println( "SoundOut: about to write to SourceDataLine." );
      }
      int numWritten = output.write( buffer, 0, buffer.length );
      if ( debugBlocking ) {
        System.out.println( "SoundOut: # written = " + numWritten );
      }

      // Put buffer back in the buffer pool.  Note the SourceDataLine write
      // had better not return til it's copied out the buffer, 'cause we're
      // about to reuse it...
      if ( debugBlocking ) {
        System.out.println( "SoundOut: about to put to buffer pool." );
      }
      putLoop:
      while(true) {
        try {
          bufferPool.put( buffer );
          break putLoop;
        }
        catch( InterruptedException e ) {
          if ( debugBlocking ) {
            System.out.println( "SoundOut: interrupted in putLoop" );
          }
          // Check for quit.
          if ( quitFlag ) break runLoop;
        }
      }

      // Debugging statistics:  Get average of samples in this buffer, and
      // update the moving average.  If we're at the stats printing interval,
      // do it and reset the counter.  Note we do this in the output thread
      // really only to show it's running.  The actual values shown should
      // be ~same as what the capture thread is showing.  We use the sample
      // bytes, endianness, and signed-or-not given to us -- we do not
      // extract those from our line's AudioFormat.
      if ( debug ) {
        // Get average of this buffer.
        int avg = 0;
        for ( int i = 0; i < buffer.length; i += sampleBytes ) {
          avg += SoundUtils.extractSample( buffer, i, sampleBytes, signed, bigEndian);
        }
        avg /= ( buffer.length / sampleBytes );
        // Update moving average of sound values.
        statAvgValue
          = statForgettingRate * statAvgValue +
            ( 1 - statForgettingRate ) * avg;
        // Update moving average of queue lengths.
        statAvgQueueLen
          = statForgettingRate * statAvgQueueLen +
            ( 1 - statForgettingRate ) * transferQueue.size();
        // Want to print them now?
        if ( statCtr >= statLoopInterval ) {
          // System.out.println does a good job of not dumping one thread's
          // line right in the middle of another.
          System.out.println( "SoundOut: # passes = " + statCtr );
          System.out.println( "SoundOut: current sound value = " + avg );
          System.out.println( "SoundOut: avg sound value = " + statAvgValue );
          System.out.println( "SoundOut: avg queue len = " + statAvgQueueLen);
          statCtr = 0;
        } else {
          statCtr++;
        }
      }

      // Forget we had this buffer.
      buffer = null;

      // The input thread does a yield for us, so in fairness, we should do
      // one for it.  But we're the ones that can't keep up (under Linux
      // with Sun's Java).  So consider being unfair.  On the other hand,
      // it's the input side that we want to not miss anything.  This side
      // we could always turn off when we don't want to hear it.
      yield();
    }

    // We've been told to quit.  Shut down the line and fall out of the loop.
    off();
  }

  // Other methods

  /**
   * Turn on the quit flag.  This is the officially sanctioned method for
   * stopping this thread.  Do not call stop() -- you have been warned.
   */
  public void quit() {
    quitFlag = true;
  }

  /**
   * Start the output line.
   */
  public void on() {
    output.start();
  }

  /**
   * Close the output line.
   */
  public void off() {
    // Tell the output to drain its internal buffers, then close it.
    output.drain();
    output.close();
  }
}
