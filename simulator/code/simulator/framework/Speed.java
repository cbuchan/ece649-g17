package simulator.framework;

/**
 * Speed commands for the drive motor.  The actuals speeds that correspond to
 * these commands can be found in simulator.elevatormodules.DriveObject
 * @author justinr2
 */
public enum Speed
{
  STOP,
  LEVEL,
  SLOW,
  FAST;
  
  public final static boolean isStopOrLevel(Speed s) {
      return (s == STOP || s == LEVEL);
  }
}