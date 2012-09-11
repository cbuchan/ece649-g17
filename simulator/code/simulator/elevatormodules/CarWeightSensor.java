package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.elevatormodules.passengers.events.OverweightBuzzerEvent;
import simulator.framework.*;
import simulator.payloads.*;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarWeightAlarmPayload.WriteableCarWeightAlarmPayload;
import simulator.payloads.CarWeightPayload.WriteableCarWeightPayload;

/**
 * Controls the weight alarm inside the car.  If the total weight of all passengers in
 * the car exceeds {@link simulator.framework.Elevator#MaxCarCapacity}, this
 * sensor will activate the alarm.
 *
 * Provides a passenger interface, and an interface for PassngerHandler to create
 * set the current weight of the car.
 *
 * Also reports the weight via network message.
 * 
 * @author Kenny Stauffer
 */
public class CarWeightSensor extends PassengerModule implements TimeSensitive {

    // for sending weight alarms
    private WriteableCarWeightPayload carWeightPhys;
    private WriteableCanMailbox carWeightAlarmNwk;
    private CarWeightAlarmCanPayloadTranslator carWeightAlarmNwkTranslator;
    // for sending weight network messages
    private WriteableCarWeightAlarmPayload carWeightAlarmPhys;
    private WriteableCanMailbox carWeightNwk;
    private CarWeightCanPayloadTranslator carWeightNwkTranslator;
    private final Timer timer = new Timer(this);
    private final static SimTime WEIGHT_PASSENGER_EVENT_PERIOD = new SimTime(10, SimTime.SimTimeUnit.SECOND);
    //private CarWeightPayload carWeightPhys;

    /**
     * Constructs a new <code>CarWeightSensor</code> that sends messages every
     * <code>period</code> microseconds.
     * 
     * @param period
     * The time between <code>CarWeightPayload</code> and
     * <code>CarWeightAlarmPayload</code> messages.
     * 
     * @param verbose
     * Whether to generate log messages about passenger weights and the state of
     * the alarm when {@link #receive(CarWeightPayload)} is called.
     */
    public CarWeightSensor(SimTime period, boolean verbose) {
        super(period, "CarWeightSensor", verbose);

        carWeightNwk = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_WEIGHT_CAN_ID);
        carWeightNwkTranslator = new CarWeightCanPayloadTranslator(carWeightNwk);
        carWeightNwkTranslator.setWeight(0);

        carWeightAlarmNwk = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_WEIGHT_ALARM_CAN_ID);
        carWeightAlarmNwkTranslator = new CarWeightAlarmCanPayloadTranslator(carWeightAlarmNwk);
        carWeightAlarmNwkTranslator.setValue(false);

        carWeightAlarmPhys = CarWeightAlarmPayload.getWriteablePayload();
        carWeightAlarmPhys.set(false);
        physicalConnection.sendOnce(carWeightAlarmPhys);

        carWeightPhys = CarWeightPayload.getWriteablePayload();
        carWeightPhys.set(0);
        physicalConnection.sendOnce(carWeightPhys);
        canNetworkConnection.sendTimeTriggered(carWeightNwk, period);
        canNetworkConnection.sendTimeTriggered(carWeightAlarmNwk, period);
    }

    /**
     * Called by the PassengerHandler to update car weight
     * @param weight
     */
    public void setWeight(int weight) {
        carWeightPhys.set(weight);
        log("car weight is ", carWeightPhys.weight());

        updateAlarm();

        //copy to network
        carWeightNwkTranslator.setWeight(carWeightPhys.weight());
        carWeightAlarmNwkTranslator.setValue(carWeightAlarmPhys.isRinging());
        //send physical update
        physicalConnection.sendOnce(carWeightAlarmPhys);
        physicalConnection.sendOnce(carWeightPhys);
    }

    private void updateAlarm() {
        if (carWeightPhys.weight() > Elevator.MaxCarCapacity) {
            if (carWeightAlarmPhys.isRinging()) {
                log("car is overloaded, turning on alarm");
            }
            carWeightAlarmPhys.set(true);
            //notify the passengers of the overweight event
            firePassengerEvent(new OverweightBuzzerEvent());
            timer.start(WEIGHT_PASSENGER_EVENT_PERIOD);
        } else {
            if (carWeightAlarmPhys.isRinging()) {
                log("car is no longer overloaded, turning off alarm");
            }
            carWeightAlarmPhys.set(false);
        }

    }

    public int getWeight() {
        return carWeightPhys.weight();
    }

    public void timerExpired(Object callbackData) {
        updateAlarm();
    }
}
