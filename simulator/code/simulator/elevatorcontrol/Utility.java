/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) - Editor
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatorcontrol;

import java.util.HashMap;
import simulator.elevatormodules.AtFloorCanPayloadTranslator;
import simulator.elevatormodules.DoorClosedCanPayloadTranslator;
import simulator.framework.Elevator;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.payloads.CANNetwork.CanConnection;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.translators.BooleanCanPayloadTranslator;

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
        
        private final DoorClosedHallwayArray front;
        private final DoorClosedHallwayArray back;
        
        public DoorClosedArray(CanConnection conn) {
            front = new DoorClosedHallwayArray(Hallway.FRONT, conn);
            back = new DoorClosedHallwayArray(Hallway.BACK, conn);
        }
        
        public boolean getAllClosed() {
            return front.getAllClosed() && back.getAllClosed();
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

        private HashMap<Integer, DoorClosedCanPayloadTranslator> 
                translatorArray;
        public final Hallway hallway;

        public DoorClosedHallwayArray(Hallway hallway, CanConnection conn) {
            this.hallway = hallway;
            
            translatorArray = 
                    new HashMap<Integer, DoorClosedCanPayloadTranslator>(
                    Side.values().length);
            
            for (Side s : Side.values()) {
                int index = ReplicationComputer.computeReplicationId(hallway, 
                        s);
                ReadableCanMailbox m = CanMailbox.getReadableCanMailbox(
                        MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID 
                        + index);
                DoorClosedCanPayloadTranslator t = 
                        new DoorClosedCanPayloadTranslator(m, hallway, s);
                conn.registerTimeTriggered(m);
                translatorArray.put(index, t);
            }
        }

        public boolean getAllClosed() {
            for (DoorClosedCanPayloadTranslator translator 
                    : translatorArray.values()) {
                if (!translator.getValue()) {
                    return false;
                }
            }
            return true;
        }
        
        public boolean getClosed(Side side) {
            return translatorArray.get(
                    ReplicationComputer.computeReplicationId(hallway, side))
                    .getValue();
        }
    }
    
    public static class CarCallArray {

        public final int numFloors = Elevator.numFloors;
        public final Hallway hallway;
        public BooleanCanPayloadTranslator[] translatorArray;

        public CarCallArray(Hallway hallway, CanConnection conn) {
            this.hallway = hallway;
            translatorArray = new BooleanCanPayloadTranslator[numFloors];
            for (int i = 0; i < numFloors; ++i) {
                ReadableCanMailbox m = CanMailbox.getReadableCanMailbox(
                        MessageDictionary.CAR_CALL_BASE_CAN_ID + 
                        ReplicationComputer.computeReplicationId(i+1, hallway));
                BooleanCanPayloadTranslator t = new BooleanCanPayloadTranslator(m);
                conn.registerTimeTriggered(m);
                translatorArray[i] = t;
            }
        }

        public boolean getValueForFloor(int floor) {
            if (floor < 1 || floor > numFloors) {
                return false;
            }
            
            return translatorArray[floor-1].getValue();
        }
    }

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
}
