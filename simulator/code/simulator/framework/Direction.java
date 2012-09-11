package simulator.framework;

public enum Direction
{
  /* these must be declared in this order so .ordinal() works as replication
   * ID
   */
  UP,
  DOWN,
  STOP;

  /**
   * Array with just UP and DOWN that can be iterated for dealing with replicated
   * controllers and the like.  No controller will ever be instantiated with a Direction
   * of STOP.
   */
  public final static Direction[] replicationValues = {
      Direction.UP,
      Direction.DOWN
  };
}
