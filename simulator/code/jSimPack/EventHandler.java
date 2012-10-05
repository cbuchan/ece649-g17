package jSimPack;

/**
 * Responds to events released by a FutureEventList.  When a FutureEventList
 * releases an event from its queue, it calls the #execute(Object) method of
 * the EventHandler associated with that event.
 */
public interface EventHandler
{
  /**
   * Respond to an event.  The argument is the Object that was passed as the
   * <code>data</cod> parameter to
   * jSimPack.FutureEventList#schedule(EventHandler,long,Object) to register
   * this event.
   */
  public void execute(Object data);
}  
