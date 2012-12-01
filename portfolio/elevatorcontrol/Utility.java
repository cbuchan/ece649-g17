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

import simulator.elevatormodules.AtFloorCanPayloadTranslator;
import simulator.elevatormodules.CarLevelPositionCanPayloadTranslator;
import simulator.elevatormodules.DoorClosedCanPayloadTranslator;
import simulator.elevatormodules.DriveObject;
import simulator.framework.*;
import simulator.payloads.CANNetwork.CanConnection;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.ReadableCanMailbox;

import java.util.HashMap;

/**
 * This class provides some example utility classes that might be useful in more
 * than one spot.  It is okay to create new classes (or modify the ones given
 * below), but you may not use utility classes in such a way that they constitute
 * a communication channel between controllers.
 *
 * @author justinr2, rdsharma
 */
public class Utility {

    /**
     * ************************************************************************
     * DoorClosed
     * ************************************************************************
     */
    public static class DoorClosedArray {

        /* Design decision:  since the Hallway enum contains special cases such 
         * as NONE and BOTH, hard code the class to enum values rather than 
         * looping through them as we do in the other array classes.  This hurts
         * modularity but results in much cleaner and more efficient code.
         */

        private DoorClosedHallwayArray front;
        private DoorClosedHallwayArray back;

        public DoorClosedArray(CanConnection conn) {
            front = new DoorClosedHallwayArray(Hallway.FRONT, conn);
            back = new DoorClosedHallwayArray(Hallway.BACK, conn);
        }

        public boolean getAllClosed() {
            return (front.getAllClosed() && back.getAllClosed());
        }

        public boolean getAllHallwayClosed(Hallway hallway) {
            if (hallway == Hallway.BOTH) {
                return getAllClosed();
            } else if (hallway == Hallway.FRONT) {
                return front.getAllClosed();
            } else if (hallway == Hallway.BACK) {
                return back.getAllClosed();
            }
            return false;
        }

        public boolean getClosed(Hallway hallway, Side side) {
            if (hallway == Hallway.BOTH) {
                return front.getClosed(side) && back.getClosed(side);
            } else if (hallway == Hallway.FRONT) {
                return front.getClosed(side);
            } else if (hallway == Hallway.BACK) {
                return back.getClosed(side);
            }
            return false;
        }
    }

    public static class DoorClosedHallwayArray {

        private DoorClosedCanPayloadTranslator left;
        private DoorClosedCanPayloadTranslator right;
        public final Hallway hallway;

        public DoorClosedHallwayArray(Hallway hallway, CanConnection conn) {
            this.hallway = hallway;

            ReadableCanMailbox m_l = CanMailbox.getReadableCanMailbox(
                    MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID +
                            ReplicationComputer.computeReplicationId(hallway,
                                    Side.LEFT));
            left = new DoorClosedCanPayloadTranslator(m_l, hallway, Side.LEFT);
            conn.registerTimeTriggered(m_l);

            ReadableCanMailbox m_r = CanMailbox.getReadableCanMailbox(
                    MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID +
                            ReplicationComputer.computeReplicationId(hallway,
                                    Side.RIGHT));
            right = new DoorClosedCanPayloadTranslator(m_r, hallway, Side.RIGHT);
            conn.registerTimeTriggered(m_r);
        }

        public boolean getAllClosed() {
            return (left.getValue() && right.getValue());
        }

        public boolean getClosed(Side side) {
            if (side == Side.LEFT) {
                return left.getValue();
            } else if (side == Side.RIGHT) {
                return right.getValue();
            }
            throw new RuntimeException("Invalid side specified");
        }
    }

    /**
     * ************************************************************************
     * CarCall
     * ************************************************************************
     */
    public static class CarCallArray {

        public final int numFloors = Elevator.numFloors;
        public final Hallway hallway;
        public TinyBooleanCanPayloadTranslator[] translatorArrayTiny;

        public CarCallArray(Hallway hallway, CanConnection conn) {
            this.hallway = hallway;
            translatorArrayTiny = new TinyBooleanCanPayloadTranslator[numFloors];
            for (int i = 0; i < numFloors; ++i) {
                ReadableCanMailbox m = CanMailbox.getReadableCanMailbox(
                        MessageDictionary.CAR_CALL_BASE_CAN_ID +
                                ReplicationComputer.computeReplicationId(i + 1, hallway));
                TinyBooleanCanPayloadTranslator t = new TinyBooleanCanPayloadTranslator(m);
                conn.registerTimeTriggered(m);
                translatorArrayTiny[i] = t;
            }
        }

        public boolean getValueForFloor(int floor) {
            if (floor < 1 || floor > numFloors) {
                return false;
            }

            return translatorArrayTiny[floor - 1].getValue();
        }

        public boolean getAllOff() {
            for (int floor = 0; floor < numFloors; ++floor) {
                if (translatorArrayTiny[floor].getValue()) {
                    return false;
                }
            }
            return true;

        }
    }


    /**
     * ************************************************************************
     * HallCall
     * ************************************************************************
     */
    public static class HallCallArray {
        public final int numFloors = Elevator.numFloors;
        public HallCallFloorArray[] translatorArray;

        public HallCallArray(CanConnection conn) {
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
            return translatorArray[floor - 1].getAllOff();
        }

        public boolean getAllFloorHallwayOff(int floor, Hallway hallway) {
            return translatorArray[floor - 1].getAllHallwayOff(hallway);
        }

        public boolean getOff(int floor, Hallway hallway, Direction dir) {
            if (floor < 1 || floor > Elevator.numFloors)
                return false;
            return translatorArray[floor - 1].getOff(hallway, dir);
        }

        public boolean getValue(int floor, Hallway hallway, Direction dir) {
            if (floor < 1 || floor > Elevator.numFloors)
                return false;
            return translatorArray[floor - 1].getValue(hallway, dir);
        }
    }

    public static class HallCallFloorArray {
        private final int floor;
        private HallCallFloorHallwayArray front;
        private HallCallFloorHallwayArray back;


        public HallCallFloorArray(int floor, CanConnection conn) {
            this.floor = floor;
            front = new HallCallFloorHallwayArray(floor, Hallway.FRONT, conn);
            back = new HallCallFloorHallwayArray(floor, Hallway.BACK, conn);
        }

        public boolean getAllOff() {
            boolean f = front.getAllOff();
            boolean b = back.getAllOff();
            return f && b;
        }

        public boolean getAllHallwayOff(Hallway hallway) {
            if (hallway == Hallway.BOTH) {
                return getAllOff();
            } else if (hallway == Hallway.FRONT) {
                return front.getAllOff();
            } else if (hallway == Hallway.BACK) {
                return back.getAllOff();
            }
            throw new RuntimeException("Illegal hallway in HallCallFloorArray.getAllHallwayOff");
        }

        /* NOTE: As of now, do not call getOff for hallway == BOTH, and a specified direction */
        public boolean getOff(Hallway hallway, Direction dir) {
            return !getValue(hallway, dir);
        }

        public boolean getValue(Hallway hallway, Direction dir) {
            if (hallway == Hallway.FRONT && dir == Direction.UP) {
                return (front.up.getValue());
            } else if (hallway == Hallway.FRONT && dir == Direction.DOWN) {
                return (front.down.getValue());
            } else if (hallway == Hallway.BACK && dir == Direction.UP) {
                return (back.up.getValue());
            } else if (hallway == Hallway.BACK && dir == Direction.DOWN) {
                return (back.down.getValue());
            } else if (hallway == Hallway.BOTH && dir == Direction.UP) {
            }
            throw new RuntimeException("Illegal hallway in HallCallFloorArray.getOff");
        }

    }

    public static class HallCallFloorHallwayArray {
        private TinyBooleanCanPayloadTranslator up;
        private TinyBooleanCanPayloadTranslator down;
        public final Hallway hallway;
        public final int floor;

        public HallCallFloorHallwayArray(int floor, Hallway hallway, CanConnection conn) {
            this.hallway = hallway;
            this.floor = floor;

            ReadableCanMailbox m_u = CanMailbox.getReadableCanMailbox(MessageDictionary.HALL_CALL_BASE_CAN_ID +
                    ReplicationComputer.computeReplicationId(floor, hallway, Direction.UP));
            up = new TinyBooleanCanPayloadTranslator(m_u);
            conn.registerTimeTriggered(m_u);

            ReadableCanMailbox m_d = CanMailbox.getReadableCanMailbox(
                    MessageDictionary.HALL_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway, Direction.DOWN));
            down = new TinyBooleanCanPayloadTranslator(m_d);
            conn.registerTimeTriggered(m_d);

        }

        public boolean getAllOff() {
            return (!up.getValue() && !down.getValue());
        }


    }

    /**
     * ************************************************************************
     * AtFloor
     * ************************************************************************
     */
    public static class AtFloorArray {

        public HashMap<Integer, AtFloorCanPayloadTranslator> networkAtFloorsTranslators = new HashMap<Integer, AtFloorCanPayloadTranslator>();
        public final int numFloors = Elevator.numFloors;

        public AtFloorArray(CanConnection conn) {
            for (int i = 0; i < numFloors; i++) {
                int floor = i + 1;
                for (Hallway h : Hallway.replicationValues) {
                    int index = ReplicationComputer.computeReplicationId(floor, h);
                    ReadableCanMailbox m = CanMailbox.getReadableCanMailbox(MessageDictionary.AT_FLOOR_BASE_CAN_ID + index);
                    AtFloorCanPayloadTranslator t = new AtFloorCanPayloadTranslator(m, floor, h);
                    conn.registerTimeTriggered(m);
                    networkAtFloorsTranslators.put(index, t);
                }
            }
        }

        public boolean isAtFloor(int floor, Hallway hallway) {
            return networkAtFloorsTranslators.get(ReplicationComputer.computeReplicationId(floor, hallway)).getValue();
        }

        public int getCurrentFloor() {
            int retval = MessageDictionary.NONE;
            for (int i = 0; i < numFloors; i++) {
                int floor = i + 1;
                for (Hallway h : Hallway.replicationValues) {
                    int index = ReplicationComputer.computeReplicationId(floor, h);
                    AtFloorCanPayloadTranslator t = networkAtFloorsTranslators.get(index);
                    if (t.getValue()) {
                        if (retval == MessageDictionary.NONE) {
                            //this is the first true atFloor
                            retval = floor;
                        } else if (retval != floor) {
                            //found a second floor that is different from the first one
                            throw new RuntimeException("AtFloor is true for more than one floor at " + Harness.getTime());
                        }
                    }
                }
            }
            return retval;
        }
    }

    /**
     * ************************************************************************
     * CommitPoint calculation
     * ************************************************************************
     */
    public static class CommitPointCalculator {

        private CarLevelPositionCanPayloadTranslator mCarLevelPosition;

        public CommitPointCalculator(CanConnection conn) {

            ReadableCanMailbox networkCarLevelPosition =
                    CanMailbox.getReadableCanMailbox(MessageDictionary.CAR_LEVEL_POSITION_CAN_ID);

            mCarLevelPosition =
                    new CarLevelPositionCanPayloadTranslator(networkCarLevelPosition);

            conn.registerTimeTriggered(networkCarLevelPosition);

        }

        //Returns whether commit point has been reached for floor f based on current pos, speed, dir of car in hoistway.
        //Assumes f is towards the direction that car is traveling in.
        public Boolean commitPoint(int f, Direction driveSpeed_d, double driveSpeed_s) {
            int dir = driveSpeed_d == Direction.UP ? 1 : -1;     //sign determined by current direction
            double speed = driveSpeed_s;                             // in m/s
            double pos = mCarLevelPosition.getPosition() / 1000;       // level position in m, *updates at each floor
            double fPos = (f - 1) * Elevator.DISTANCE_BETWEEN_FLOORS;  // DISTANCE_BETWEEN_FLOORS in m
            double commitPt = pos;

            double decel = DriveObject.Deceleration;    // in m/s^2
            double slow = DriveObject.SlowSpeed;        // in m/s
            double level = DriveObject.LevelingSpeed;   // in m/s
            double stop = DriveObject.StopSpeed;        // in m/s

            if ((dir == 1 && fPos <= pos) || (dir == -1 && fPos >= pos)) {
                return true; // reached for f 'less' than curr floor (direction dependent)
            }

            if (speed > slow) {
                commitPt += dir * (1 / decel) *
                        (speed * (speed - slow) + slow * (slow - stop)
                                - 0.5 * ((speed - slow) * (speed - slow) + (slow - stop) * (slow - stop)));
            } else if (speed > stop) {
                commitPt += dir * (1 / decel) *
                        (speed * (speed - stop)
                                - 0.5 * (speed - stop) * (speed - stop));
            }

            /* Some numbers (assume fast speed = 1.0, slow speed = 0.25):
             * stopping distance from fast is 0.5m
             * stopping distance from slow is 0.03125m
             */

            //System.out.println("(levelpos, dir, commitpt, fpos) = ("+pos+", "+dir+", "+commitPt+", "+fPos+")");

            // let's set the error threshold to 10cm (to compensate for the level position sensor updating)
            //double error = 0.1;
            double error = 1.8;
            if (dir == 1) {
                if (commitPt < fPos - error) return false; //not reached
            } else if (dir == -1) {
                if (commitPt > fPos + error) return false; //not reached
            }
            return true; //reached
        }

//        public int nextReachableFloor(Direction driveSpeed_d, double driveSpeed_s) {
//            // Returns the lowest "Not Reached" floor
//            int delta = 5; //mm
//
//            if (driveSpeed_d == Direction.UP) {
//                for (int i = 1; i < Elevator.numFloors; i++) {
//                    if (commitPoint(i, driveSpeed_d, driveSpeed_s) == false) {
//                        return i; //Found the closest "Not Reached"
//                    }
//                }
//                return (int) Math.ceil(((mCarLevelPosition.getPosition() + delta) / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000))) + 1;
//            }
//            // Returns the highest "Not Reached" floor
//            else if (driveSpeed_d == Direction.DOWN) {
//                for (int i = Elevator.numFloors; i >= 1; i--) {
//                    if (commitPoint(i, driveSpeed_d, driveSpeed_s) == false) {
//                        return i; //Found the closest "Not Reached"
//                    }
//                }
//                return (int) Math.floor(((mCarLevelPosition.getPosition() - delta) / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000))) + 1;
//            }
//
//            // Failed to find a floor. Try to return the nearest floor
//            return (int) Math.round(mCarLevelPosition.getPosition() / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000)) + 1;
//        }

        public int nextReachableFloor(Direction driveSpeed_d, double driveSpeed_s) {
            // Returns the lowest "Not Reached" floor
            int delta = 0; //mm
            int nextFloor;

            int upper = Math.min(mCarLevelPosition.getPosition() + delta,
                    (int) Elevator.DISTANCE_BETWEEN_FLOORS * 1000 * (Elevator.numFloors - 1));
            int lower = Math.max(mCarLevelPosition.getPosition() - delta, 0);

            if (driveSpeed_d == Direction.UP) {
                nextFloor =
                        (int) Math.ceil(((upper) / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000))) + 1;
            }
            // Returns the highest "Not Reached" floor
            else if (driveSpeed_d == Direction.DOWN) {
                nextFloor =
                        (int) Math.floor(((lower) / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000))) + 1;
            } else {
                // Failed to find a floor. Try to return the nearest floor
                nextFloor = (int) Math.round(mCarLevelPosition.getPosition() / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000)) + 1;
            }

            return nextFloor;
        }

        public int nextReachableFloorDelta(Direction driveSpeed_d, double driveSpeed_s) {
            // Returns the lowest "Not Reached" floor
            int delta = 101; //mm
            int nextFloor;

            int upper = Math.min(mCarLevelPosition.getPosition() + delta,
                    (int) Elevator.DISTANCE_BETWEEN_FLOORS * 1000 * (Elevator.numFloors - 1));
            int lower = Math.max(mCarLevelPosition.getPosition() - delta, 0);

            if (driveSpeed_d == Direction.UP) {
                nextFloor =
                        (int) Math.ceil(((upper) / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000))) + 1;
            }
            // Returns the highest "Not Reached" floor
            else if (driveSpeed_d == Direction.DOWN) {
                nextFloor =
                        (int) Math.floor(((lower) / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000))) + 1;
            } else {
                // Failed to find a floor. Try to return the nearest floor
                nextFloor = (int) Math.round(mCarLevelPosition.getPosition() / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000)) + 1;
            }

            return nextFloor;
        }

        public int getCommittedFloor(Direction driveSpeed_d, double driveSpeed_s) {
            // Returns the highest "reached" floor
            if (driveSpeed_d == Direction.UP) {
                for (int i = Elevator.numFloors; i >= 1; i--) {
                    if (commitPoint(i, driveSpeed_d, driveSpeed_s) == true) {
                        return i; //Found the highest "reached"
                    }
                }
            }
            // Returns the lowest "reached" floor
            else if (driveSpeed_d == Direction.DOWN) {
                for (int i = 1; i < Elevator.numFloors; i++) {
                    if (commitPoint(i, driveSpeed_d, driveSpeed_s) == true) {
                        return i; //Found the lowest "reached"
                    }
                }
            }
            // Failed to find a floor. Try to return the nearest floor
            return (int) Math.round(mCarLevelPosition.getPosition() / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000)) + 1;
        }

        public int getCommittedFloorDispatcher(Direction driveSpeed_d, double driveSpeed_s) {
            // Returns the highest "reached" floor
            if (driveSpeed_d == Direction.UP) {
                for (int i = Elevator.numFloors; i >= 1; i--) {
                    if (commitPoint(i, driveSpeed_d, driveSpeed_s) == true) {
                        return Math.min(i + 1, Elevator.numFloors); //Found the highest "reached"
                    }
                }
            }
            // Returns the lowest "reached" floor
            else if (driveSpeed_d == Direction.DOWN) {
                for (int i = 1; i < Elevator.numFloors; i++) {
                    if (commitPoint(i, driveSpeed_d, driveSpeed_s) == true) {
                        return Math.max(i - 1, 1); //Found the lowest "reached"
                    }
                }
            }
            // Failed to find a floor. Try to return the nearest floor
            return (int) Math.round(mCarLevelPosition.getPosition() / (Elevator.DISTANCE_BETWEEN_FLOORS * 1000)) + 1;
        }
    }
}
