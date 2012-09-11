package simulator.elevatormodules;

// simulator.framework gives us access to CNI, PFI and Harness 
import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.*;
// simulator.payloads gives us access to all MessagePayload classes 
import simulator.payloads.*;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarPositionPayload.ReadableCarPositionPayload;
import simulator.payloads.DrivePayload.ReadableDrivePayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;

/**
 * Sends CarLevelPositionnetwork messages
 * when the Car passes one of the position sensors on the hoistway rails.
 *
 * @author Charles Shelton 
 */
public class CarLevelPositionSensor extends Module {

    // For verbose output 
    private ReadableCarPositionPayload localCarPosition;
    private WriteableCanMailbox carLevelNwk;
    private CarLevelPositionCanPayloadTranslator carLevelNwkTranslator;
    private ReadableDrivePayload driveOrderedState;
    private ReadableDriveSpeedPayload driveSpeedState;

    /**
     * @param periodicity_
     * time between <code>CarLevelPositionPayload</code> messages
     */
    public CarLevelPositionSensor(SimTime period, boolean verbose) {
        super(period, "CarLevelPositionSensor", verbose);

        localCarPosition = CarPositionPayload.getReadablePayload();
        driveOrderedState = DrivePayload.getReadablePayload();
        driveSpeedState = DriveSpeedPayload.getReadablePayload();
        carLevelNwk = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_LEVEL_POSITION_CAN_ID);
        carLevelNwkTranslator = new CarLevelPositionCanPayloadTranslator(carLevelNwk);
        //initialize network message
        currentRoundedPosition = (int) (localCarPosition.position() / 10.0) / 100;
        carLevelNwkTranslator.setPosition(currentRoundedPosition);
        

        // Register for Drive and DriveSpeed messages from the framework 
        physicalConnection.registerTimeTriggered(driveOrderedState);
        physicalConnection.registerTimeTriggered(driveSpeedState);

        // Receive CarPosition events on the framework and update CarLevelPosition on the network 
        physicalConnection.registerEventTriggered(localCarPosition);
        canNetworkConnection.sendTimeTriggered(carLevelNwk, period);
    }

    private int currentRoundedPosition;

    private void computeRoundedPosition() {
        // Calculate Car Level Postion for closest integer 10 centimeters 

        double tempCarPos = localCarPosition.position() * 10;
        int previousRoundedPosition = currentRoundedPosition;
        long tempCarLevelPos;
        //long prevCarLevelPos = localCarLevelPos.position;

        // If we're going down, take the ceiling, if we're going up take the floor 
        if (driveSpeedState.direction() == Direction.DOWN ||
                (driveOrderedState.direction() == Direction.DOWN &&
                (driveSpeedState.direction() == Direction.DOWN || driveSpeedState.direction() == Direction.STOP))) {

            tempCarLevelPos = (long) Math.ceil(tempCarPos);
            // multiply by 100 to get value in millimeters
            tempCarLevelPos *= 100;

            // make sure we don't output a value above the current position when going down
            if (tempCarLevelPos <= previousRoundedPosition) {
                currentRoundedPosition = (int) tempCarLevelPos;
            }

        } else {

            tempCarLevelPos = (long) Math.floor(tempCarPos);
            // multiply by 100 to get value in millimeters 
            tempCarLevelPos *= 100;

            // make sure we don't out a value below the current position when going up 
            if (tempCarLevelPos >= previousRoundedPosition) {
                currentRoundedPosition = (int) tempCarLevelPos;
            }
        }
    }

    @Override
    public void receive(ReadableCarPositionPayload msg) {
        computeRoundedPosition();
        carLevelNwkTranslator.setPosition(currentRoundedPosition);
        //System.out.println("CarLevelPosSensor:  " + carLevelNwk);
        log(this,
                "CarLevelPosition=",carLevelNwk,
                " ",localCarPosition,
                " ",driveOrderedState,
                " ",driveSpeedState);
    }

    @Override
    public String toString() {
        return "CarLevelPositionSensor";
    }
}
