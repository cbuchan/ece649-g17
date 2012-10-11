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

import java.util.HashMap;

import simulator.elevatormodules.AtFloorCanPayloadTranslator;
import simulator.elevatormodules.DoorClosedCanPayloadTranslator;
import simulator.elevatormodules.HallCallCanPayloadTranslator;
import simulator.framework.Elevator;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.framework.ReplicationComputer;
import simulator.framework.Direction;
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
                                ReplicationComputer.computeReplicationId(i + 1, hallway));
                BooleanCanPayloadTranslator t = new BooleanCanPayloadTranslator(m);
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
		public boolean getAllOff(){
			for (int floor = 0; floor < numFloors; ++floor) {
                if ( translatorArray[floor].getValue() ) {
					return false;
				}
            }
            return true;
			
		}
    }

    
    
	
	
	
	
	
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
                if ( !translatorArray[floor].getAllOff() ) {
					return false;
				}
            }
            return true;
        }
		
        public boolean getAllFloorOff(int floor) {
            return translatorArray[floor].getAllOff();
        }
		
        public boolean getAllFloorHallwayOff(int floor, Hallway hallway) {
			return translatorArray[floor].getAllHallwayOff(hallway);
        }
		public boolean getOff(int floor, Hallway hallway, Direction dir){
			return translatorArray[floor].getOff(hallway,dir);
		}
	}

	public static class HallCallFloorArray {
		private final int floor;
		private HallCallFloorHallwayArray front;
		private HallCallFloorHallwayArray back;
		
		
		public HallCallFloorArray(int floor, CanConnection conn){
			this.floor = floor;
			front = new HallCallFloorHallwayArray(floor, Hallway.FRONT, conn);
			back = new HallCallFloorHallwayArray(floor, Hallway.BACK, conn);
			
		}
		
		public boolean getAllOff() {
			return (front.getAllOff() && back.getAllOff());
		}
		public boolean getAllHallwayOff(Hallway hallway){
			if (hallway == Hallway.BOTH) {
                return getAllOff();
            } else if (hallway == Hallway.FRONT) {
                return front.getAllOff();
            } else if (hallway == Hallway.BACK) {
                return back.getAllOff();
            }
			return false;
		}
		/* NOTE: As of now, do not call getOff for hallway == BOTH, and a specified direction */
		public boolean getOff(Hallway hallway, Direction dir){
			if (hallway == Hallway.FRONT && dir == Direction.UP) {
				return (front.up.getValue());
			}else if (hallway == Hallway.FRONT && dir == Direction.DOWN) {
				return (front.down.getValue());
			}else if (hallway == Hallway.BACK && dir == Direction.UP) {
				return (back.up.getValue());
			}else if (hallway == Hallway.BACK && dir == Direction.DOWN) {
				return (back.down.getValue());
			}else if (hallway == Hallway.BOTH && dir == Direction.UP) {
			}
			return false; 
		}
		
	}
	
	public static class HallCallFloorHallwayArray {
		private HallCallCanPayloadTranslator up;
		private HallCallCanPayloadTranslator down;
		public final Hallway hallway;
		public final int floor;
		
		public HallCallFloorHallwayArray(int floor, Hallway hallway, CanConnection conn){
			this.hallway = hallway;
			this.floor = floor;
			
			ReadableCanMailbox m_u = CanMailbox.getReadableCanMailbox(
					MessageDictionary.HALL_CALL_BASE_CAN_ID + 
					ReplicationComputer.computeReplicationId(floor, hallway, 
					Direction.UP));
			up = new HallCallCanPayloadTranslator(m_u, floor, hallway, Direction.UP);
			conn.registerTimeTriggered(m_u);
			
			ReadableCanMailbox m_d = CanMailbox.getReadableCanMailbox(
					MessageDictionary.HALL_CALL_BASE_CAN_ID +
					ReplicationComputer.computeReplicationId(floor, hallway, 
					Direction.DOWN));
            down = new HallCallCanPayloadTranslator(m_d, floor, hallway, Direction.DOWN);
            conn.registerTimeTriggered(m_d);
			
		}
		public boolean getAllOff(){
			return (up.getValue() && down.getValue());
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
