/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers;

import java.util.ArrayList;
import simulator.elevatormodules.CarButtonLight;
import simulator.elevatormodules.CarLantern;
import simulator.elevatormodules.CarPositionIndicator;
import simulator.elevatormodules.CarWeightAlarm;
import simulator.elevatormodules.CarWeightSensor;
import simulator.elevatormodules.Door;
import simulator.elevatormodules.HallButtonLight;

/**
 * A compact data class that holds references to all the system objects the passengers
 * interact with.
 *
 * This class implements PassengerEventSender and forwards receiver registration
 * to all PassengerEventSender objects.  If you add new passenger events to the
 * system, make sure that the sender is put into the senders list.
 * 
 * @author justinr2
 */
public class PassengerControl implements PassengerEventSender{
    public final HallButtonLight[] hallCalls;
    public final CarButtonLight[] carCalls;
    public final CarLantern carLanterns;
    public final Door[] doors;
    public final CarPositionIndicator carPositionIndicator;
    public final CarWeightAlarm carWeightAlarm;
    public final CarWeightSensor carWeightSensor;
    final DriveMonitor driveMonitor;

    public PassengerControl(HallButtonLight[] hallCalls, 
            CarButtonLight[] carCalls,
            CarLantern carLanterns,
            Door[] doors,
            CarPositionIndicator carPositionIndicator,
            CarWeightAlarm carWeightAlarm,
            CarWeightSensor carWeightSensor) {
        this.hallCalls = hallCalls;
        this.carCalls = carCalls;
        this.carLanterns = carLanterns;
        this.doors = doors;
        this.carPositionIndicator = carPositionIndicator;
        this.carWeightAlarm = carWeightAlarm;
        this.carWeightSensor = carWeightSensor;
        driveMonitor = DriveMonitor.getInstance();
        //add senders to list
        senders.add(driveMonitor);
        for (Door d : doors) {
            senders.add(d);
        }
        senders.add(carWeightSensor);
        senders.add(carPositionIndicator);
    }

    private ArrayList<PassengerEventSender> senders = new ArrayList<PassengerEventSender>();

    public void registerReceiver(PassengerEventReceiver receiver) {
        for (PassengerEventSender s : senders) {
            s.registerReceiver(receiver);
        }
    }

    public void unregisterReceiver(PassengerEventReceiver receiver) {
        for (PassengerEventSender s : senders) {
            s.unregisterReceiver(receiver);
        }
    }

    public void unregisterReceivers() {
        for (PassengerEventSender s : senders) {
            s.unregisterReceivers();
        }
    }



}
