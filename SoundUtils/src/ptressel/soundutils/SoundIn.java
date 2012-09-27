package ptressel.soundutils;

import javax.sound.sampled.*;
import java.util.concurrent.*;

/**
 * Copy sound from the capture line and queue it up.
 *
 * @see SoundCopy for more info.
 */

public class SoundIn extends Thread {

  // The current system's newline string ("\n" for Unix, or "\r\n" for Windows).
  private final static String NEWLINE = System.getProperty( "line.separator" );

  // Instance data

  /** Flag that tells us when to quit. */
  private boolean quitFlag = false;

  /** Capture line */
  private final TargetDataLine capture;

  /** Transfer queue */
  private final LinkedBlockingQueue<byte[]> transferQueue;

  /** Buffer pool */
  private final LinkedBlockingQueue<byte[]> bufferPool;

  /** Buffer size */
  private final int bufferSize;

  // Debugging info

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

  /** Moving average of buffer pool length. */
  private double statAvgPoolLen = 0;

  // Constructor

  /**
   * Make a SoundIn thread.
   *
   * @param capture the input line from which to acquire sound
   * @param transferQueue the queue into which full buffers will be placed
   * @param bufferPool a source of empty buffers
   * @param bufferSize size that buffers should be (in case more are needed)
   */
  public SoundIn( TargetDataLine capture,
                  LinkedBlockingQueue<byte[]> transferQueue,
                  LinkedBlockingQueue<byte[]> bufferPool,
                  int bufferSize ) {

    this.capture = capture;
    this.transferQueue = transferQueue;
    this.bufferPool = bufferPool;
    this.bufferSize = bufferSize;

    debug = false;
    debugBlocking = false;
  }

  /**
   * Make a SoundIn thread.
   *
   * @param capture the input line from which to acquire sound
   * @param transferQueue the queue into which full buffers will be placed
   * @param bufferPool a source of empty buffers
   * @param bufferSize size that buffers should be (in case more are needed)
   */
  public SoundIn( TargetDataLine capture,
                  LinkedBlockingQueue<byte[]> transferQueue,
                  LinkedBlockingQueue<byte[]> bufferPool,
                  int bufferSize,
                  boolean debug, boolean debugBlocking, int statLoopInterval,
                  double statForgettingRate, int sampleBytes,
                  boolean signed, boolean bigEndian ) {

    // Standard setup.
    this( capture, transferQueue, bufferPool, bufferSize );

    // Now set the debug options.
    this.debug = debug;
    this.debugBlocking = debugBlocking;
    this.statLoopInterval = statLoopInterval;
    this.statForgettingRate = statForgettingRate;
    this.sampleBytes = sampleBytes;
    this.signed = signed;
    this.bigEndian = bigEndian;

    // Start the buffer pool stats off with the right value.
    if ( debug ) statAvgPoolLen = bufferPool.size();
  }

  // Thread methods

  /**
   * The run loop:  Get a buffer from the buffer pool, if there are any, else
   * make a new buffer.  Fill it from the capture line.  Queue it up on the
   * transfer queue.  Repeat until our quit flag goes high.
   */
  @Override
  public void run() {

    // The buffer we're moving from capture to transfer queue.
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

      // Get a buffer from the buffer pool, if possible, else make a new one.
      // The poll method doesn't block -- it returns null if nothing is
      // available.
      buffer = bufferPool.poll();
      if ( buffer == null ) {
        // Tried recycling -- now be greedy.
        buffer = new byte[ bufferSize ];
      }

      // Read something from the capture line.  Nothing fancy is done about
      // partial buffers.  Might be good to zero the buffer before reading,
      // or wrap the byte array in something that stores the actual number
      // of samples read.
      int numRead = capture.read( buffer, 0, bufferSize );
      if ( debugBlocking ) {
            System.out.println( "SoundIn: # read = " + numRead );
      }

      // Put it in the transfer queue
      putLoop:
      while(true) {
        try {
          if ( debugBlocking ) {
            System.out.println( "SoundIn: about to put to transfer queue." );
          }
          transferQueue.put( buffer );
          if ( debugBlocking ) {
            System.out.println( "SoundIn: put finished." );
          }
          break putLoop;
        }
        catch( InterruptedException e ) {
          // Check for quit.  
          if ( quitFlag ) break runLoop;
        }
      }

      // Debugging statistics:  Get average of samples in this buffer, and
      // update the moving average.  If we're at the stats printing interval,
      // do it and reset the counter.  Note we use the sample bytes,
      // endianness, and signed-or-not given to us -- we do not
      // extract those from our line's AudioFormat.
      if ( debug ) {
        // Get average of this buffer.
        int avg = 0;
        for ( int i = 0; i < buffer.length; i += sampleBytes ) {
          avg += SoundUtils.extractSample( buffer, i, sampleBytes,
                   signed, bigEndian );
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
        statAvgPoolLen
          = statForgettingRate * statAvgPoolLen +
            ( 1 - statForgettingRate ) * bufferPool.size();
        // Want to print them now?
        if ( statCtr >= statLoopInterval ) {
          // System.out.println does a good job of not dumping one thread's
          // line right in the middle of another's.
          System.out.println(
                  "SoundIn: # passes = " + statCtr + NEWLINE +
                  "SoundIn: current sound value = " + avg + NEWLINE +
                  "SoundIn: avg sound value = " + statAvgValue + NEWLINE +
                  "SoundIn: avg queue len = " + statAvgQueueLen + NEWLINE +
                  "SoundIn: avg pool len = " + statAvgPoolLen );
          statCtr = 0;
        } else {
          statCtr++;
        }
      }

      // Forget we had this buffer.
      buffer = null;

      // Give the poor output thread a break.
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
   * Start the capture line.
   */
  public void on() {
    capture.start();
  }

  /**
   * Close the capture line.
   */
  public void off() {
    // We aren't going to copy over any more info, so we flush anything the
    // line has in its internal buffers.
    capture.stop();
    capture.flush();
    capture.close();
  }
}
