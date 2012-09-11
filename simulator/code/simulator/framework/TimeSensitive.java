package simulator.framework;

/**
 * Allows simulator components to execute at specified time intervals.
 * Classes that want to act as handlers for {@link Timer} objects must
 * implement this interface.
 *
 * @see AbstractTimer
 * @see Timer
 * @see SystemTimer
 * 
 * @author William Nace
 * @author Kenny Stauffer
 */
public interface TimeSensitive {

  /**
   * Called when a <code>Timer</code> expires.
   *
   * @param callbackData
   * the value that was passed to {@link Timer#start(long,Object)}, or
   * <code>null</code> if {@link Timer#start(long)} was called instead.
   */
  public abstract void timerExpired(Object callbackData);

}
