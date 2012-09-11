/*
 * PrintDataEvent
 *   An Event that prints the data 
 *
 * RCS Identity
 * $Id$
 *
 * RCS Log
 * $Log$
 *
 */

package jSimPack;

/**
 * A utility class that prints the callback object to stdout when the event occurs.
 */
public class PrintDataEvent implements FutureEventListener {

  public void eventReleased(Object data){
    System.out.println(data);
  }

}  
