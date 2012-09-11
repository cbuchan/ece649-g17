/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules.passengers;

import jSimPack.SimTime;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import simulator.elevatormodules.Module;
import simulator.elevatormodules.passengers.DriveMonitor.DriveState;
import simulator.elevatormodules.passengers.events.DoorOpeningEvent;
import simulator.elevatormodules.passengers.events.MotionEvent;
import simulator.elevatormodules.passengers.events.OverweightBuzzerEvent;
import simulator.elevatormodules.passengers.events.PassengerEvent;
import simulator.elevatormodules.passengers.events.PositionIndicatorChangedEvent;
import simulator.framework.Elevator;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.framework.ReplicationComputer;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;

/**
 * This class manages the Passenger objects in the system.  It injects them 
 * at the proper time and maintains queues for the car, the landings, and
 * both sets of doors.  It is also responsible for receiving PassengerEvents from 
 * the system objects and forwarding them to the appropriate objects.  The
 * way this is done (the implementation of the passengerEvent() method) must be 
 * consistent with what the Passenger objects expect.
 * 
 * This class also sets the current weight whenever a passenger is added to or 
 * removed from carQueue.
 *
 * If modifying this class, make sure you keep queueMap consistent with hallQueue and carQueue.
 * I.e., always update queueMap when you move a passenger to a new hall or car queue
 * so that you can use queueMap to find the passenger's queue later on.
 *
 *
 * @author Justin Ray
 */
public class PassengerHandler implements PassengerEventReceiver {

    /**
     * Callback class to inject the passenger into the system at their start time.
     */
    private class PassengerInjector implements TimeSensitive {

        Passenger p;
        Timer t;

        public PassengerInjector(Passenger p) {
            this.p = p;
            t = new Timer(this);
            t.start(p.getInfo().injectionTime);
        }

        public void timerExpired(Object callbackData) {
            injectPassenger(p);
        }
    }
    final boolean verbose;
    //passenger data
    final PassengerControl pc;
    private final DoorQueue[] doorQueue = new DoorQueue[2];
    final PassengerQueue[] hallQueues;
    final PassengerQueue carQueue;
    final Map<Passenger, PassengerQueue> queueMap = new HashMap<Passenger, PassengerQueue>(); //useful for finding the queue a passenger is in
    final List<Passenger> injectedPassengers = new ArrayList<Passenger>();
    final List<Passenger> deliveredPassengers = new ArrayList<Passenger>();
    final int passengerTotal;

    /**
     * Initiate injectors for all passengers and initialize the system queues.  
     * @param pc
     * @param passengers
     * @param verbose
     */
    public PassengerHandler(PassengerControl pc, List<Passenger> passengers, boolean verbose) {
        this.verbose = verbose;
        this.pc = pc;
        pc.registerReceiver(this);
        //create door queues
        for (Hallway h : Hallway.replicationValues) {
            doorQueue[ReplicationComputer.computeReplicationId(h)] = new DoorQueue(h);
        }
        //create the queue arrays
        hallQueues = new PassengerQueue[Elevator.numFloors * 2];
        for (int floor = 1; floor <= Elevator.numFloors; floor++) {
            for (Hallway hall : Hallway.replicationValues) {
                if (Elevator.hasLanding(floor, hall)) {
                    hallQueues[ReplicationComputer.computeReplicationId(floor, hall)] = new PassengerQueue();
                }
            }
        }
        carQueue = new PassengerQueue();
        //create injector for each passenger
        for (Passenger p : passengers) {
            new PassengerInjector(p);
        }
        passengerTotal = passengers.size();
        updateCarWeight();
    }

    /**
     * Respond to passenger events by forwarding them to the appropriate Passenger
     * objects.
     * 
     * Responds to:
     * MotionEvent - forward to all passengers in the car queue
     * DoorOpeningEvent - foward to all passengers in the car queue and all
     * passengers at the hall queue on the floor and side where the door is opening.
     * OverweightBuzzerEvent - forward to the last passenger in the car queue and all
     * passengers in the hall landings where the doors are open.
     * PositionIndicatorChangedEvent - forward to passengers in the carQueue.
     * 
     * 
     * @param e
     */
    public void passengerEvent(PassengerEvent e) {
        if (e instanceof MotionEvent) {
            //clear the door queues whenever we start moving
            doorQueue[0].clear();
            doorQueue[1].clear();
            //forward to the passengers in the car
            //log("Forward motion event to car passengers");
            carQueue.passengerEvent(e);
        } else if (e instanceof DoorOpeningEvent) {
            DoorOpeningEvent de = (DoorOpeningEvent) e;
            //double check state
            if (pc.driveMonitor.getDriveState() == DriveState.STOPPED) {
                //forward to the passengers at the landing and in the car
                //log("Forward door opening event to car and hall passengers");
                int currentFloor = pc.driveMonitor.getCurrentFloor();
                hallQueues[ReplicationComputer.computeReplicationId(currentFloor, de.getHallway())].passengerEvent(e);
                carQueue.passengerEvent(e);
            } //else do nothing because there will probably be an emergency brake
        } else if (e instanceof OverweightBuzzerEvent) {
            //forward to the last passenger in the car to make them get out
            //forward to hall passengers to make them back off
            //log("Forward overweight event to hall passengers and last passenger in the car");
            int currentFloor = pc.driveMonitor.getCurrentFloor();
            for (Hallway h : Hallway.replicationValues) {
                //only forward to hall if doors are open
                if (pc.doors[ReplicationComputer.computeReplicationId(h)].isNotClosed()) {
                    hallQueues[ReplicationComputer.computeReplicationId(currentFloor, h)].passengerEvent(e);
                }
            }
            carQueue.peekLast().passengerEvent(e);
        } else if (e instanceof PositionIndicatorChangedEvent) {
            //forward position indicator events to all passengers in the car
            carQueue.passengerEvent(e);
        } else {
            log("Ignored event " + e);
        }
    }

    /**
     * Add the passenger to the appropriate queue
     * @param p
     */
    private void injectPassenger(Passenger p) {
        log("Injecting passenger", p);
        PassengerQueue target;
        int floor = p.getInfo().startFloor;
        Hallway hallway = p.getInfo().startHallway;
        if (floor == 0) {
            target = carQueue;
            if (!Harness.getTime().equals(SimTime.ZERO)) {
                throw new RuntimeException("This exception caused by tring to inject a passenger into the car queue at a time other than 0 (simulation start).");
            }
        } else {
            target = hallQueues[ReplicationComputer.computeReplicationId(floor, hallway)];
        }
        target.addLast(p);
        queueMap.put(p, target);
        injectedPassengers.add(p);
        p.setPassengerHandler(this);
        p.start();
        updateCarWeight();
    }

    /***************************************************************************
     * Interface methods for passenger
     **************************************************************************/

    /**
     * Remove the passenger from their current queue and put them into the car queue
     * @param p
     */
    void joinCarQueue(Passenger p) {
        log(p, " joining car queue");
        //remove the passenger from a hall queue
        PassengerQueue queue = queueMap.get(p);
        if (queue == null) {
            throw new RuntimeException("Passenger " + p + " not in a queue.");
        }
        queue.remove(p);
        queueMap.remove(p);
        //put in the car queue
        carQueue.addLast(p);
        queueMap.put(p, carQueue);
        updateCarWeight();
    }

    /**
     * remove the passenger from their current queue and put them in a hall queue
     * @param p
     */
    void joinHallQueue(Passenger p, int floor, Hallway hallway) {
        log(p, " joining hall queue ", floor, ",", hallway);
        //do some sanity checking to see if the hall queue we are trying to join
        //is reachable from the car.
        if (pc.driveMonitor.getCurrentFloor() != floor) {
            throw new RuntimeException("Attempted to join hall queue at " + floor + "," + hallway + " but the car is at floor " + floor);
        }
        if (pc.doors[ReplicationComputer.computeReplicationId(hallway)].isClosed()) {
            throw new RuntimeException("Attempted to join hall queue at " + floor + "," + hallway + " but the doors on that side are not open");
        }
        PassengerQueue queue = queueMap.get(p);
        if (queue == null) {
            throw new RuntimeException("Passenger " + p + " not in a queue.");
        }
        queue.remove(p);
        queueMap.remove(p);
        //put in the car queue
        PassengerQueue hallQueue = hallQueues[ReplicationComputer.computeReplicationId(floor, hallway)];
        hallQueue.addLast(p);
        queueMap.put(p, hallQueue);
        updateCarWeight();
    }

    /**
     * Put the passenger at the end of the queue they were already in.
     * @param p
     */
    void requeue(Passenger p) {
        log(p, " moved to the end of queue");
        PassengerQueue queue = queueMap.get(p);
        queue.remove(p);
        queue.addLast(p);
        updateCarWeight();
    }

    /**
     * Utility method for computing the weight and forwarding it to the 
     * weight sensor object.
     */
    private void updateCarWeight() {
        int oldWeight = pc.carWeightSensor.getWeight();
        int weight = 0;
        for (Passenger p : carQueue.passengers) {
            weight += p.getWeight();
        }
        if (oldWeight != weight) {
            log("Car weight=", weight);
            pc.carWeightSensor.setWeight(weight);
        }
    }

    /**
     * Called by the passenger object whenever they have arrived at their destination.
     * @param p
     */
    void passengerFinished(Passenger p) {
        //save the passenger in a separate queue
        log(p, " finished.");
        PassengerQueue q = queueMap.get(p);
        q.remove(p);
        queueMap.remove(p);
        updateCarWeight();
        injectedPassengers.remove(p);
        deliveredPassengers.add(p);
        if (deliveredPassengers.size() == passengerTotal) {
            //all passengers delivered, so stop the simulator
            Harness.endSim();
        }
    }

    /**
     * @param hallway
     * @return a reference to the door queue for Hallway hallway
     */
    public DoorQueue getDoorQueue(Hallway hallway) {
        return doorQueue[ReplicationComputer.computeReplicationId(hallway)];
    }

    /***************************************************************************
     * Interface methods for gui
     **************************************************************************/

    public int getCarPassengerCount() {
        return carQueue.size();
    }

    public int getHallPassengerCount(int floor, Hallway hall) {
        return hallQueues[ReplicationComputer.computeReplicationId(floor, hall)].size();
    }

    public String getCarPassengerInfo() {
        return carQueue.getToolTipString();
    }

    public String getHallPassengerInfo(int floor, Hallway hall) {
        return hallQueues[ReplicationComputer.computeReplicationId(floor, hall)].getToolTipString();
    }



    /***************************************************************************
     * Interface methods for stats reporting (simulator.framework.Elevator)
     **************************************************************************/

    /**
     * Get full stats for all passengers
     * @return a multiline string with all passenger stats
     */
    public String getStats() {
        return getStatStr(false);
    }

    /**
     * Get summary stats for all passengers
     * @return a multiline string with summary passenger stats
     */
    public String getSummaryStats() {
        return getStatStr(true);
    }

    /**
     * Utility method for stats since the summary and full stats need to compute
     * the same metrics.
     * @param isSummary
     * @return the appropriate stats string based on isSummary
     */
    private String getStatStr(boolean isSummary) {
        StringWriter output = new StringWriter();
        PrintWriter stats = new PrintWriter(output);


        stats.println("STATISTICS FOR ELEVATOR SIMULATION PASSENGER DELIVERY");
        stats.println("-----------------------------------------------------");
        stats.println();

        long temp_maxDeliveryTime = 0;
        long temp_totalDeliveryTime = 0;

        int delivered = 0;
        int stranded = 0;
        int notInjected = 0;


        //count up passengers first
        /* count passengers still in the system */
        for (Passenger p : injectedPassengers) {
            if (p.getState() == Passenger.State.INIT) {
                throw new RuntimeException("Passenger in INIT state means the simulator did not run past the last injected time.");
            } else if (p.getState() == Passenger.State.DONE) {
                throw new RuntimeException("Finished passenger in the injection queue means there is a missing call to finish() somewhere in the passenger");
            } else {
                ++stranded;
            }
        }
        delivered = deliveredPassengers.size();

        if (!isSummary) {
            //print warnings
            if (stranded > 0) {
                stats.println("The following passengers were stranded:");
                for (Passenger p : injectedPassengers) {
                    stats.println(p.getStatusLine());
                }
            }
            stats.println();
            stats.println("These passengers were delivered:");
        }


        SimTime totalDeliveryTime = SimTime.ZERO;
        SimTime maxDeliveryTime = SimTime.ZERO;
        double totalSatisfactionScore = 0;
        double minSatisfactionScore = Double.MAX_VALUE;

        for (Passenger p : deliveredPassengers) {
            totalDeliveryTime = SimTime.add(totalDeliveryTime, p.getDeliveryTime());
            if (p.getDeliveryTime().isGreaterThan(maxDeliveryTime)) {
                maxDeliveryTime = p.getDeliveryTime();
            }
            totalSatisfactionScore += p.getSatisfactionScore();
            if (p.getSatisfactionScore() < minSatisfactionScore) {
                minSatisfactionScore = p.getSatisfactionScore();
            }
            if (!isSummary) {
                stats.println(p.getStatusLine());
                stats.print(p.getSatisfactionStats());
            }
        }

        double averageDeliveryTime = totalDeliveryTime.getFracSeconds() / (delivered);
        double averageSatisfaction = totalSatisfactionScore / delivered;


        stats.println();
        stats.println("Passenger Delivery Summary");
        stats.println("Delivered: " + delivered);
        stats.println("Stranded: " + stranded);
        stats.println("Total: " + passengerTotal);

        if (notInjected == 0 && stranded == 0) {
            stats.println();
            stats.println("Deliver Stats (lower is better):");
            stats.format("Average_delivery_time: %.3f\n", averageDeliveryTime);
            stats.format("Maximum_delivery_time:  %.3f\n", maxDeliveryTime.getFracSeconds());
            stats.format("Delivery_performance_score:  %.3f\n", (4 * averageDeliveryTime + (maxDeliveryTime.getFracSeconds())));
            stats.println();
            stats.println("Satisfaction Stats (higher is better):");
            stats.format("Average_satisfaction_score: %.3f\n", averageSatisfaction);
            stats.format("Min_satisfaction_score:  %.3f\n", minSatisfactionScore);
            stats.format("Satisfaction_performance_score:  %.3f\n", (4 * averageSatisfaction + minSatisfactionScore));
            stats.println();
        } else {
            stats.println();
            stats.println("Deliver Stats (lower is better):");
            stats.println("Average_delivery_time: n/a");
            stats.println("Maximum_delivery_time:  n/a");
            stats.format("Delivery_performance_score:  %d\n", Integer.MAX_VALUE);
            stats.println();
            stats.println("Satisfaction Stats (higher is better):");
            stats.println("Average_satisfaction_score: n/a");
            stats.println("Min_satisfaction_score:  n/a");
            stats.format("Satisfaction_performance_score:  %.3f\n", 0.0);
        }

        stats.close();
        return output.toString();
    }

    private void log(Object... msg) {
        if (verbose) {
            Harness.log("PassengerHandler", msg);
        }
    }
}
