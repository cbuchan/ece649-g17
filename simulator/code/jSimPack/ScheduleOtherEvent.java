/*
 * ScheduleOtherEvent
 *   An Event that schedules other events
 *
 * RCS Identity
 * $Id$
 *
 * RCS Log
 * $Log$
 *
 */

package jSimPack;

public class ScheduleOtherEvent implements FutureEventListener {

  private final FutureEventListener event;
  private final SimTime interval;
  private final FutureEventList fel;

  public ScheduleOtherEvent(FutureEventListener event, SimTime interval, 
                     FutureEventList fel){
		this.event = event;
		this.interval = interval; 
		this.fel = fel;
  }

  public void eventReleased(Object data){
    fel.schedule(event, interval, null);
  }

}  
