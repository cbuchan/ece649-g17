package simulator.payloads;

/**
 * A physical state value that is sent between components in the simulator.
 *
 * This class is the superclass for all physical state messages.  It contains
 * event constants for each state type that guarantee that each state type will
 * have a unique ID.
 *
 * In order for reflection to work, all PhysicalPayload descendants MUST implement
 * static factory methods getReadablePayload() and getWriteablePayload().
 *
 * @author Beth Latronico
 * @author Kenny Stauffer
 * @author Justin Ray
 */
public abstract class PhysicalPayload extends Payload {

    public static abstract class PhysicalReadablePayload extends ReadablePayload {
        public PhysicalReadablePayload(PhysicalPayload payload) {
            super(payload);
        }
    }

    public static abstract class PhysicalWriteablePayload extends WriteablePayload {
        public PhysicalWriteablePayload(PhysicalPayload payload) {
            super(payload);
        }
    }


    public static final int idOffset = 8;
    //top bit indicates this is a physical message


    /*
     * Assign unique IDs here.
     * IDs 0--15 are reserved for internal messages.  Network messages should
     * use IDs 16 and above.
     */
    public static final int TimerEvent = 1<<idOffset;
    public static final int NetworkEvent = 5<<idOffset;
    //public static final int FaultEvent = 6<<idOffset;
    public static final int CarPositionEvent = 7<<idOffset;
    public static final int DoorPositionEvent = 8<<idOffset;
    public static final int PersonWeightEvent = 9<<idOffset;
    public static final int BandwidthEvent = 10<<idOffset;
    public static final int NullEvent = 15<<idOffset;
    public static final int DoorMotorEvent = 16<<idOffset;
    public static final int DoorReversalEvent = 17<<idOffset;
    public static final int AtFloorEvent = 18<<idOffset;
    public static final int CarLevelPositionEvent = 19<<idOffset;
    public static final int EmergencyBrakeEvent = 20<<idOffset;
    public static final int DoorClosedEvent = 21<<idOffset;
    public static final int DriveEvent = 22<<idOffset;
    public static final int DriveSpeedEvent = 23<<idOffset;
    public static final int HoistwayLimitEvent = 24<<idOffset;
    public static final int CarWeightEvent = 25<<idOffset;
    public static final int DesiredDwellEvent = 26<<idOffset;
    public static final int DesiredFloorEvent = 27<<idOffset;
    public static final int DoorOpenedEvent = 28<<idOffset;
    public static final int CarCallEvent = 29<<idOffset;
    public static final int HallCallEvent = 30<<idOffset;
    public static final int CarLanternEvent = 31<<idOffset;
    public static final int CarLightEvent = 32<<idOffset;
    public static final int CarPositionIndicatorEvent = 33<<idOffset;
    public static final int CarWeightAlarmEvent = 34<<idOffset;
    public static final int HallLightEvent = 35<<idOffset;
    //fault ids
    public static final int CarLevelFaultEvent = 36<<idOffset;
    public static final int DoorMotorFaultEvent = 37<<idOffset;
    public static final int AtFloorFaultEvent = 38<<idOffset;
    public static final int LevelingEvent = 39<<idOffset;
    public static final int PassengerCountEvent = 40<<idOffset;

    // BL 12/02/02 Added lastDropped -- CNI uses this to determine
    // if the last message of a particular type was dropped
    // Initialized to false

    /**
     * The network message type code of this message.
     */
    private int networkEventID;
    /**
     * Distinguishes this message from several with the same
     * <code>networkEventID</code>.
     */
    private int replicationID;
    
    
    /**
     * Copy constructor
     * @param p the object to copy from
     */
    public PhysicalPayload(PhysicalPayload p) {
        super(p);
        this.networkEventID = p.networkEventID;
        this.replicationID = p.replicationID;
    }

    /**
     * Constructs a <code>Payload</code> with the specified message type and
     * replication identifier.
     * 
     * @param type
     * message type.  Must be one of the static <code>Event</code> values
     * defined in <code>Payload</code>.
     * 
     * @param replicationID
     * Distinguishes this message from others of the same message type.
     * 
     * @throws IllegalArgumentException
     * If <code>replicationID</code> is negative or is equal to or larger than
     * 2<sup><code>idOffset</code></sup>.
     */
    public PhysicalPayload(int type, int replicationID) {
        super(type + replicationID);
        if (replicationID < 0 || replicationID >= (1 << idOffset)) {
            throw new IllegalArgumentException("replicationID: " + replicationID);
        }
        this.networkEventID = type;
        this.replicationID = replicationID;
    }

    public PhysicalPayload(int type) {
        this(type, 0);
    }


    public final int getNetworkEventID() {
        return networkEventID;
    }

    public final int getReplicationID() {
        return replicationID;
    }

    /**
     * PhysicalPayload messages always have 0 size.
     *
     * @return 0
     */
    @Override
    public int getSize() {
        return 0;
    }
    
    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        PhysicalPayload c = (PhysicalPayload)p;
        this.networkEventID = c.networkEventID;
        this.replicationID = c.replicationID;
    }
}
