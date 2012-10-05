package jSimPack;

/**
 * Receives notification when events are released from a FutureEventList.
 */
public interface FutureEventListener
{
  /**
   * Invoked when an event is released from a <code>FutureEventList</code>.
   * The argument is whatever was passed as the
   * <code>data</cod> parameter when
   * {@link jSimPack.FutureEventList#schedule(FutureEventListener, long, Object)}
   * was called to register this event.
   */
  public void eventReleased(Object data);
}