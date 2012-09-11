package simulator.framework;

/**
 * Hallway constants correspond to the front and back doors on the elevator and
 * elevator shaft/landings.
 * @author justinr2
 */
public enum Hallway
{
  /* these must be declared in this order so .ordinal() can be used to make
   * reliable replication IDs in Payloads.
   */
  FRONT,
  BACK,
  NONE,
  BOTH;
  
  /**
   * Array for iterating over replicated physical objects and the like.  No
   * controller object should ever be instantiated with a direction of NONE or BOTH.
   */
  public final static Hallway[] replicationValues = {
      Hallway.FRONT,
      Hallway.BACK
  };

  /**
   * Utility method for getting the opposite hallway.
   * @param h  FRONT or BACK
   * @return returns the opposite hallway.
   * @throws RuntimeException if something other than FRONT or BACK is passed.
   */
  public final static Hallway oppositeHallway(Hallway h) {
      if (h == Hallway.FRONT) return Hallway.BACK;
      if (h == Hallway.BACK) return Hallway.FRONT;
      throw new RuntimeException("No opposite value for hallway " + h);
  }
  
}