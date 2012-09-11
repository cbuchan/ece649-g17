/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

import jSimPack.SimTime;
import java.util.ArrayList;
import simulator.elevatormodules.passengers.PassengerEventReceiver;
import simulator.elevatormodules.passengers.PassengerEventSender;
import simulator.elevatormodules.passengers.events.PassengerEvent;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;

/**
 * subclass of module that sends passenger events
 * @author Justin Ray
 */
public class PassengerModule extends Module implements PassengerEventSender {

    private class PassengerEventCallback implements TimeSensitive {

        private PassengerEvent e;

        public PassengerEventCallback(PassengerEvent e) {
            this.e = e;
            new Timer(this).start(SimTime.ZERO);
        }

        public void timerExpired(Object callbackData) {
            deliver(e);
        }

    }

    public PassengerModule(SimTime period, String name, boolean verbose) {
        super(period, name, verbose);
    }

    private ArrayList<PassengerEventReceiver>receivers = new ArrayList<PassengerEventReceiver>();

    protected void firePassengerEvent(PassengerEvent e) {
        new PassengerEventCallback(e);
    }

    private void deliver(PassengerEvent e) {
        for (PassengerEventReceiver r : receivers) {
            r.passengerEvent(e);
        }
    }

    public void registerReceiver(PassengerEventReceiver receiver) {
        receivers.add(receiver);
    }

    public void unregisterReceiver(PassengerEventReceiver receiver) {
        receivers.remove(receiver);
    }

    public void unregisterReceivers() {
        receivers.clear();
    }




}
