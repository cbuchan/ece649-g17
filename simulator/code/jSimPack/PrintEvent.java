/*
 * PrintEvent
 *   An Event that prints a string
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
 * Utility class that prints a string when the scheduled event occurs.
 */
public class PrintEvent implements FutureEventListener {

  private String printString;

  public PrintEvent(String printString){
    this.printString = printString;
  }

  public void eventReleased(Object data){
    System.out.println(printString);
  }

}