package simulator.framework;

/**
 * This is a utility class that provides methods for the consistent generation
 * of offsets for replicated controllers.  The computerReplicationId method
 * returns an integer offset that provides a unique value according to the
 * replicated dimensions passed.  In general, the offsets are not interoperable,
 * i.e. the values returned by floor,hallway are not related in any meaningful
 * way to values returned by floor,hallway,direction.
 *
 * For each computeReplicationId method, a corresponding makeReplicationString
 * method is provided.  It is recommended that you use these when setting the
 * controller name (in a call to the Controller superclass contructor) in order
 * to provide consistent names that can be used for state assertions.
 *
 * @author Justin Ray
 */
public class ReplicationComputer {

    /**
     * The computerReplicationId method returns an integer offset that provides 
     * a unique value according to the replicated dimensions passed.
     */
    public final static int computeReplicationId(int floor, Hallway hallway) {
        checkFloor(floor);
        checkHallway(hallway);
        return (floor - 1) * 2 + hallway.ordinal();
    }

    /**
     * Make a bracketed string from the values passed.  Useful for generating
     * name strings for replicated modules.
     */
    public final static String makeReplicationString(int floor, Hallway hallway) {
        checkFloor(floor);
        checkHallway(hallway);
        return makeBracketedString(floor, hallway);
    }

    /**
     * The computerReplicationId method returns an integer offset that provides
     * a unique value according to the replicated dimensions passed.
     */
    public final static int computeReplicationId(int floor, Hallway hallway, Direction direction) {
        checkFloor(floor);
        checkHallway(hallway);
        checkDirection(direction);
        return (floor - 1) * 4 + hallway.ordinal() * 2 + direction.ordinal();
    }

    /**
     * Make a bracketed string from the values passed.  Useful for generating
     * name strings for replicated modules.
     */
    public final static String makeReplicationString(int floor, Hallway hallway, Direction direction) {
        checkFloor(floor);
        checkHallway(hallway);
        checkDirection(direction);
        return makeBracketedString(floor, hallway, direction);
    }

    /**
     * The computerReplicationId method returns an integer offset that provides
     * a unique value according to the replicated dimensions passed.
     */
    public final static int computeReplicationId(Hallway hallway, Side side) {
        checkHallway(hallway);
        checkSide(side);
        return hallway.ordinal() * 2 + side.ordinal();
    }

    /**
     * Make a bracketed string from the values passed.  Useful for generating
     * name strings for replicated modules.
     */
    public final static String makeReplicationString(Hallway hallway, Side side) {
        checkHallway(hallway);
        checkSide(side);
        return makeBracketedString(hallway, side);
    }

    /**
     * The computerReplicationId method returns an integer offset that provides
     * a unique value according to the replicated dimensions passed.
     */
    public final static int computeReplicationId(Direction direction) {
        checkDirection(direction);
        return direction.ordinal();
    }

    /**
     * Make a bracketed string from the values passed.  Useful for generating
     * name strings for replicated modules.
     */
    public final static String makeReplicationString(Direction direction) {
        checkDirection(direction);
        return makeBracketedString(direction);
    }

    /**
     * The computerReplicationId method returns an integer offset that provides
     * a unique value according to the replicated dimensions passed.
     */
    public final static int computeReplicationId(Hallway hallway) {
        checkHallway(hallway);
        return hallway.ordinal();
    }

    public final static String makeReplicationString(Hallway hallway) {
        checkHallway(hallway);
        return makeBracketedString(hallway);
    }

    /**
     * The computerReplicationId method returns an integer offset that provides
     * a unique value according to the replicated dimensions passed.
     */
    public final static int computeReplicationId(Side side) {
        checkSide(side);
        return side.ordinal();
    }

    /**
     * Make a bracketed string from the values passed.  Useful for generating
     * name strings for replicated modules.
     */
    public final static String makeReplicationString(Side side) {
        checkSide(side);
        return makeBracketedString(side);
    }

    //utility functions
    private final static String makeBracketedString(Object... args) {
        StringBuilder b = new StringBuilder();
        for (Object o : args) {
            b.append("[");
            b.append(o.toString());
            b.append("]");
        }
        return b.toString();
    }
    private final static void checkFloor(int floor) {
        if (floor < 1 || floor > Elevator.numFloors) {
            throw new RuntimeException("Floor out of range");
        }
    }
    private final static void checkHallway(Hallway hallway) {
        if (hallway != Hallway.FRONT && hallway != Hallway.BACK) {
            //throw new RuntimeException("Hallway can only be FRONT or BACK");
        }
    }
    private final static void checkDirection(Direction direction) {
        if (direction != Direction.UP && direction != Direction.DOWN) {
            //throw new RuntimeException("Hallway can only be UP or DOWN");
        }
    }
    private final static void checkSide(Side side) {
        //all possible values of side are acceptable, but this method is provided
        //foe completeness
    }
}
