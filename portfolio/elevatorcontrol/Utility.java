/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal) - Editor
 * Rajeev Sharma (rdsharma) - Editor
 * Collin Buchan (cbuchan)  - Editor
 * Jessica Tiu   (jtiu)     
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatorcontrol;

/**
 * This class provides some example utility classes that might be useful in more
 * than one spot.  It is okay to create new classes (or modify the ones given
 * below), but you may not use utility classes in such a way that they constitute
 * a communication channel between controllers.
 *
 * @author justinr2, rdsharma
 */
public class Utility {

    public static class DoorClosedArray {

        /* Design decision:  since the Hallway enum contains special cases such 
         * as NONE and BOTH, hard code the class to enum values rather than 
         * looping through them as we do in the other array classes.  This hurts
         * modularity but results in much cleaner and more efficient code.
         */

        private DoorClosedHallwayArray front;
        private DoorClosedHallwayArray back;

        public DoorClosedArray(simulator.payloads.CANNetwork.CanConnection conn) {
            front = new DoorClosedHallwayArray(simulator.framework.Hallway.FRONT, conn);
            back = new DoorClosedHallwayArray(simulator.framework.Hallway.BACK, conn);
        }

        public boolean getAllClosed() {
            return (front.getAllClosed() && back.getAllClosed());
        }

        public boolean getAllHallwayClosed(simulator.framework.Hallway hallway) {
            if (hallway == simulator.framework.Hallway.BOTH) {
                return getAllClosed();
            } else if (hallway == simulator.framework.Hallway.FRONT) {
                return front.getAllClosed();
            } else if (hallway == simulator.framework.Hallway.BACK) {
                return back.getAllClosed();
            }
            return false;
        }

        public boolean getClosed(simulator.framework.Hallway hallway, simulator.framework.Side side) {
            if (hallway == simulator.framework.Hallway.BOTH) {
                return front.getClosed(side) && back.getClosed(side);
            } else if (hallway == simulator.framework.Hallway.FRONT) {
                return front.getClosed(side);
            } else if (hallway == simulator.framework.Hallway.BACK) {
                return back.getClosed(side);
            }
            return false;
        }
    }

    public static class DoorClosedHallwayArray {

        private simulator.elevatormodules.DoorClosedCanPayloadTranslator left;
        private simulator.elevatormodules.DoorClosedCanPayloadTranslator right;
        public final simulator.framework.Hallway hallway;

        public DoorClosedHallwayArray(simulator.framework.Hallway hallway, simulator.payloads.CANNetwork.CanConnection conn) {
            this.hallway = hallway;

            simulator.payloads.CanMailbox.ReadableCanMailbox m_l = simulator.payloads.CanMailbox.getReadableCanMailbox(
                    MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID +
                            simulator.framework.ReplicationComputer.computeReplicationId(hallway,
                                    simulator.framework.Side.LEFT));
            left = new simulator.elevatormodules.DoorClosedCanPayloadTranslator(m_l, hallway, simulator.framework.Side.LEFT);
            conn.registerTimeTriggered(m_l);

            simulator.payloads.CanMailbox.ReadableCanMailbox m_r = simulator.payloads.CanMailbox.getReadableCanMailbox(
                    MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID +
                            simulator.framework.ReplicationComputer.computeReplicationId(hallway,
                                    simulator.framework.Side.RIGHT));
            right = new simulator.elevatormodules.DoorClosedCanPayloadTranslator(m_r, hallway, simulator.framework.Side.RIGHT);
            conn.registerTimeTriggered(m_r);
        }

        public boolean getAllClosed() {
            return (left.getValue() && right.getValue());
        }

        public boolean getClosed(simulator.framework.Side side) {
            if (side == simulator.framework.Side.LEFT) {
                return left.getValue();
            } else if (side == simulator.framework.Side.RIGHT) {
                return right.getValue();
            }
            throw new RuntimeException("Invalid side specified");
        }
    }

    public static class CarCallArray {

        public final int numFloors = simulator.framework.Elevator.numFloors;
        public final simulator.framework.Hallway hallway;
        public simulator.payloads.translators.BooleanCanPayloadTranslator[] translatorArray;

        public CarCallArray(simulator.framework.Hallway hallway, simulator.payloads.CANNetwork.CanConnection conn) {
            this.hallway = hallway;
            translatorArray = new simulator.payloads.translators.BooleanCanPayloadTranslator[numFloors];
            for (int i = 0; i < numFloors; ++i) {
                simulator.payloads.CanMailbox.ReadableCanMailbox m = simulator.payloads.CanMailbox.getReadableCanMailbox(
                        MessageDictionary.CAR_CALL_BASE_CAN_ID +
                                simulator.framework.ReplicationComputer.computeReplicationId(i + 1, hallway));
                simulator.payloads.translators.BooleanCanPayloadTranslator t = new simulator.payloads.translators.BooleanCanPayloadTranslator(m);
                conn.registerTimeTriggered(m);
                translatorArray[i] = t;
            }
        }

        public boolean getValueForFloor(int floor) {
            if (floor < 1 || floor > numFloors) {
                return false;
            }

            return translatorArray[floor - 1].getValue();
        }

        public boolean getAllOff() {
            for (int floor = 0; floor < numFloors; ++floor) {
                if (translatorArray[floor].getValue()) {
                    return false;
                }
            }
            return true;

        }
    }


    public static class HallCallArray {
        public final int numFloors = simulator.framework.Elevator.numFloors;
        public HallCallFloorArray[] translatorArray;

        public HallCallArray(simulator.payloads.CANNetwork.CanConnection conn) {
            translatorArray = new HallCallFloorArray[numFloors];

            for (int floor = 0; floor < numFloors; ++floor) {
                HallCallFloorArray hcfa = new HallCallFloorArray(floor + 1, conn);
                translatorArray[floor] = hcfa;
            }
        }

        public boolean getAllOff() {
            for (int floor = 0; floor < numFloors; ++floor) {
                if (!translatorArray[floor].getAllOff()) {
                    return false;
                }
            }
            return true;
        }

        public boolean getAllFloorOff(int floor) {
            return translatorArray[floor].getAllOff();
        }

        public boolean getAllFloorHallwayOff(int floor, simulator.framework.Hallway hallway) {
            return translatorArray[floor].getAllHallwayOff(hallway);
        }

        public boolean getOff(int floor, simulator.framework.Hallway hallway, simulator.framework.Direction dir) {
            return translatorArray[floor].getOff(hallway, dir);
        }
    }

    public static class HallCallFloorArray {
        private final int floor;
        private HallCallFloorHallwayArray front;
        private HallCallFloorHallwayArray back;


        public HallCallFloorArray(int floor, simulator.payloads.CANNetwork.CanConnection conn) {
            this.floor = floor;
            front = new HallCallFloorHallwayArray(floor, simulator.framework.Hallway.FRONT, conn);
            back = new HallCallFloorHallwayArray(floor, simulator.framework.Hallway.BACK, conn);
        }

        public boolean getAllOff() {
            boolean f = front.getAllOff();
            boolean b = back.getAllOff();
            return f && b;
        }

        public boolean getAllHallwayOff(simulator.framework.Hallway hallway) {
            if (hallway == simulator.framework.Hallway.BOTH) {
                return getAllOff();
            } else if (hallway == simulator.framework.Hallway.FRONT) {
                return front.getAllOff();
            } else if (hallway == simulator.framework.Hallway.BACK) {
                return back.getAllOff();
            }
            throw new RuntimeException("Illegal hallway in HallCallFloorArray.getAllHallwayOff");
        }

        /* NOTE: As of now, do not call getOff for hallway == BOTH, and a specified direction */
        public boolean getOff(simulator.framework.Hallway hallway, simulator.framework.Direction dir) {
            if (hallway == simulator.framework.Hallway.FRONT && dir == simulator.framework.Direction.UP) {
                return (front.up.getValue());
            } else if (hallway == simulator.framework.Hallway.FRONT && dir == simulator.framework.Direction.DOWN) {
                return (front.down.getValue());
            } else if (hallway == simulator.framework.Hallway.BACK && dir == simulator.framework.Direction.UP) {
                return (back.up.getValue());
            } else if (hallway == simulator.framework.Hallway.BACK && dir == simulator.framework.Direction.DOWN) {
                return (back.down.getValue());
            } else if (hallway == simulator.framework.Hallway.BOTH && dir == simulator.framework.Direction.UP) {
            }
            throw new RuntimeException("Illegal hallway in HallCallFloorArray.getAllHallwayOff");
        }

    }

    public static class HallCallFloorHallwayArray {
        private simulator.payloads.translators.BooleanCanPayloadTranslator up;
        private simulator.payloads.translators.BooleanCanPayloadTranslator down;
        public final simulator.framework.Hallway hallway;
        public final int floor;

        public HallCallFloorHallwayArray(int floor, simulator.framework.Hallway hallway, simulator.payloads.CANNetwork.CanConnection conn) {
            this.hallway = hallway;
            this.floor = floor;

            simulator.payloads.CanMailbox.ReadableCanMailbox m_u = simulator.payloads.CanMailbox.getReadableCanMailbox(MessageDictionary.HALL_CALL_BASE_CAN_ID +
                    simulator.framework.ReplicationComputer.computeReplicationId(floor, hallway, simulator.framework.Direction.UP));
            up = new simulator.payloads.translators.BooleanCanPayloadTranslator(m_u);
            conn.registerTimeTriggered(m_u);

            simulator.payloads.CanMailbox.ReadableCanMailbox m_d = simulator.payloads.CanMailbox.getReadableCanMailbox(MessageDictionary.HALL_CALL_BASE_CAN_ID +
                    simulator.framework.ReplicationComputer.computeReplicationId(floor, hallway, simulator.framework.Direction.DOWN));
            down = new simulator.payloads.translators.BooleanCanPayloadTranslator(m_d);
            conn.registerTimeTriggered(m_d);

        }

        public boolean getAllOff() {
            return (!up.getValue() && !down.getValue());
        }


    }


    public static class AtFloorArray {

        public java.util.HashMap<Integer, simulator.elevatormodules.AtFloorCanPayloadTranslator> networkAtFloorsTranslators = new java.util.HashMap<Integer, simulator.elevatormodules.AtFloorCanPayloadTranslator>();
        public final int numFloors = simulator.framework.Elevator.numFloors;

        public AtFloorArray(simulator.payloads.CANNetwork.CanConnection conn) {
            for (int i = 0; i < numFloors; i++) {
                int floor = i + 1;
                for (simulator.framework.Hallway h : simulator.framework.Hallway.replicationValues) {
                    int index = simulator.framework.ReplicationComputer.computeReplicationId(floor, h);
                    simulator.payloads.CanMailbox.ReadableCanMailbox m = simulator.payloads.CanMailbox.getReadableCanMailbox(
                            MessageDictionary.AT_FLOOR_BASE_CAN_ID + index);
                    simulator.elevatormodules.AtFloorCanPayloadTranslator t = new simulator.elevatormodules.AtFloorCanPayloadTranslator(m, floor, h);
                    conn.registerTimeTriggered(m);
                    networkAtFloorsTranslators.put(index, t);
                }
            }
        }

        public boolean isAtFloor(int floor, simulator.framework.Hallway hallway) {
            return networkAtFloorsTranslators.get(simulator.framework.ReplicationComputer.computeReplicationId(floor, hallway)).getValue();
        }

        public int getCurrentFloor() {
            int retval = MessageDictionary.NONE;
            for (int i = 0; i < numFloors; i++) {
                int floor = i + 1;
                for (simulator.framework.Hallway h : simulator.framework.Hallway.replicationValues) {
                    int index = simulator.framework.ReplicationComputer.computeReplicationId(floor, h);
                    simulator.elevatormodules.AtFloorCanPayloadTranslator t = networkAtFloorsTranslators.get(index);
                    if (t.getValue()) {
                        if (retval == MessageDictionary.NONE) {
                            //this is the first true atFloor
                            retval = floor;
                        } else if (retval != floor) {
                            //found a second floor that is different from the first one
                            throw new RuntimeException("AtFloor is true for more than one floor at " + simulator.framework.Harness.getTime());
                        }
                    }
                }
            }
            return retval;
        }
    }
}
