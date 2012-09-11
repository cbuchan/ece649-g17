package simulator.framework;

import jSimPack.SimTime;
import jSimPack.SimTime.SimTimeUnit;
import java.util.*;
import simulator.payloads.*;
import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.CarCallPayload.WriteableCarCallPayload;
import simulator.payloads.CarLanternPayload.ReadableCarLanternPayload;
import simulator.payloads.CarLightPayload.ReadableCarLightPayload;
import simulator.payloads.CarPositionIndicatorPayload.ReadableCarPositionIndicatorPayload;
import simulator.payloads.CarWeightAlarmPayload.ReadableCarWeightAlarmPayload;
import simulator.payloads.DoorPositionPayload.ReadableDoorPositionPayload;
import simulator.payloads.DoorReversalPayload.WriteableDoorReversalPayload;
import simulator.payloads.HallCallPayload.WriteableHallCallPayload;
import simulator.payloads.HallLightPayload.ReadableHallLightPayload;

/**
 * The driver for all the Passengers in the simulations.  All Passenger
 * objects need to execute their statecharts in lockstep to avoid race
 * conditions.  This class oversees the actions of the Passengers so that the
 * execution of all of their statecharts is an atomic action within the
 * simulator.
 *
 * @author
 * Kenny Stauffer (kstauffe)
 */
public class PassengerController extends Networkable {

    enum PassengerState {

        WAITING_IN_HALLWAY,
        ATTEMPTING_ENTRY,
        ABORTING_ENTRY,
        WAITING_IN_CAR,
        ATTEMPTING_EXIT,
        ABORTING_EXIT,
        ATTEMPTING_BACKOUT,
        DELIVERED
    }

    /**
     * A passenger who can press buttons and travel in the elevator car.
     */
    public class Passenger {

        /**
         * Weight of a passenger, in tenths of a pound.
         */
        public static final int weight = 1500;
        /**
         * A unique serial number for this Passenger.
         */
        private final long passengerID;
        /**
         * The time at which this Passenger approaches the start hallway.
         */
        private final SimTime startTime;
        /**
         * The time at which this Passenger exited the car at the desired
         * destination floor.  If this Passenger has not been delivered yet,
         * this value is undefined.
         */
        private SimTime arrivalTime;
        /**
         * The floor this passenger starts at.
         */
        private final int startFloor;
        /**
         * The floor this Passenger wants to go to.
         */
        private final int destinationFloor;
        /**
         * The hallway this Passenger starts at.
         */
        private final Hallway startHallway;
        /**
         * The hallway this Passenger wants to go to.
         */
        private final Hallway destinationHallway;
        /**
         * Specifies the minimum size of the space between the doors that this
         * Passenger can move through.
         */
        private final int minWidth;
        /**
         * Specifies whether this Passenger is traveling up or down.  If
         * <tt>destFloor &gt; endFloor</tt>, this value is
         * <tt>Direction.UP</tt>.  If <tt>destFloor &lt; endFloor</tt>, this
         * value is <tt>Direction.DOWN</tt>.
         */
        private final Direction direction;
        /**
         * Indicates whether the Passenger will log state transitions and other
         * information about its execution.
         */
        private final boolean verbose;
        /**
         * The state (from the state chart diagram) this Passenger is in.
         */
        private PassengerState state;
        /**
         * The time after which this Passenger can move "forward" from the
         * current state to the next state.  Moving forward means getting
         * close to being delivered (for example, going from
         * <tt>ATTEMPTING_ENTRY</tt> to <tt>WAITING_IN_CAR</tt>).
         */
        private SimTime actionTime;
        /**
         * The time at which this Passenger will abort whatever action she was
         * attempting (such as entering or exiting the car).
         */
        private SimTime abortTime;
        /**
         * The maximum number of times this Passenger will press
         * <tt>hallButton</tt> or <tt>carButton</tt> before the corresponding
         * button light illuminates.
         */
        private int pressesRemaining;
        /**
         * The message this Passenger will listen for to know when the elevator
         * has reached the start floor.
         */
        private ReadableAtFloorPayload startAtFloor;
        /**
         * The message this Passenger will listen for to know when the elevator
         * has reached the destination floor.  This message is used for runtime
         * correctness checks.  The passenger will exit when the car position
         * indicator shows the desired floor (and the doors are open).
         */
        private ReadableAtFloorPayload destinationAtFloor;
        /**
         * The hall button this Passenger will look at.
         */
        private HallButton hallButton;
        /**
         * The car button this Passenger will look at.
         */
        private CarButton carButton;
        /**
         * The lantern for the direction this Passenger is not traveling in.
         * The Passenger will only board the elevator if one of the following
         * is true: either both lanterns are off, or the lantern for the
         * Passenger's desired direction is on.  Since at most one lantern can
         * be on at any time, the second condition implies that the lantern
         * for the direction this Passenger <em>does not</em> want is off.
         * So, in both cases, this Passenger will want to enter the car only
         * if the lantern for the direction she doesn't want is off.
         */
        private ReadableCarLanternPayload otherLantern;
        /**
         * The HallQueue this Passenger is waiting in.  This value is
         * <tt>null</tt> if the Passenger is inside the car or has been
         * delivered.
         */
        private List<Passenger> hallQueue;
        /**
         * The CarQueue this Passenger is waiting in.  This value is
         * <tt>null</tt> if the Passenger is outside the car.
         */
        private List<Passenger> startCarQueue;
        private List<Passenger> destinationCarQueue;
        private ReadableDoorPositionPayload[] startDoorPos;
        private WriteableDoorReversalPayload[] startDoorRev;
        private ReadableDoorPositionPayload[] destinationDoorPos;
        private WriteableDoorReversalPayload[] destinationDoorRev;

        public Passenger(SimTime startTime, int startFloor, Hallway startHallway, int destFloor, Hallway destHallway, int minWidth,
                boolean verbose) {
            if (startHallway != Hallway.FRONT && startHallway != Hallway.BACK) {
                throw new IllegalArgumentException(
                        "start hallway must be FRONT or BACK");
            }
            if (destHallway != Hallway.FRONT && destHallway != Hallway.BACK) {
                throw new IllegalArgumentException(
                        "destination hallway must be FRONT or BACK");
            }
            if (startFloor < 0 || startFloor > Elevator.numFloors) {
                throw new IllegalArgumentException(
                        "start floor must be in [0,numFloors]");
            }
            if (destFloor < 1 || destFloor > Elevator.numFloors) {
                throw new IllegalArgumentException(
                        "destination floor must be in [1,numFloors]");
            }
            if (!Elevator.hasLanding(destFloor, destHallway)) {
                throw new IllegalArgumentException(
                        "No landing at destination floor=" + destFloor + ", hall" + destHallway);
            }
            if (startFloor != 0) {
                if (!Elevator.hasLanding(startFloor, startHallway)) {
                    throw new IllegalArgumentException(
                            "No landing at starting floor=" + startFloor + ", hall" + startHallway);
                }
            }


            if (destFloor > startFloor
                    || //cover the case where the passenger gets on and off at the same floor
                    //at the first floor where there is no down button.
                    (destFloor == startFloor && startFloor == 1)) {
                direction = Direction.UP;
            } else {
                direction = Direction.DOWN;
            }

            if (startFloor == 0) {
                state = PassengerState.WAITING_IN_CAR;
            } else {
                state = PassengerState.WAITING_IN_HALLWAY;
            }

            startFloor--;
            destFloor--;

            passengerID = ++passengerCounter;
            this.startTime = startTime;
            this.startFloor = startFloor;
            this.startHallway = startHallway;
            this.destinationFloor = destFloor;
            this.destinationHallway = destHallway;
            this.minWidth = minWidth;
            this.verbose = verbose;
            pressesRemaining = 0;

            if (state == PassengerState.WAITING_IN_HALLWAY) {
                switch (startHallway) {
                    case FRONT:
                        startAtFloor = frontAtFloors[startFloor];
                        if (direction == Direction.UP) {
                            hallButton = frontUpHallButtons[startFloor];
                            otherLantern = downLantern;
                            hallQueue = frontUpHallQueues.get(startFloor);
                        } else {
                            hallButton = frontDownHallButtons[startFloor];
                            otherLantern = upLantern;
                            hallQueue = frontDownHallQueues.get(startFloor);
                        }
                        startCarQueue = frontCarQueues.get(startFloor);
                        startDoorPos = frontDoorPosition;
                        startDoorRev = frontDoorReversal;
                        break;
                    case BACK:
                        startAtFloor = backAtFloors[startFloor];
                        if (direction == Direction.UP) {
                            hallButton = backUpHallButtons[startFloor];
                            otherLantern = downLantern;
                            hallQueue = backUpHallQueues.get(startFloor);
                        } else {
                            hallButton = backDownHallButtons[startFloor];
                            otherLantern = upLantern;
                            hallQueue = backDownHallQueues.get(startFloor);
                        }
                        startCarQueue = backCarQueues.get(startFloor);
                        startDoorPos = backDoorPosition;
                        startDoorRev = backDoorReversal;
                        break;
                }
            }

            switch (destinationHallway) {
                case FRONT:
                    destinationAtFloor = frontAtFloors[destinationFloor];
                    carButton = frontCarButtons[destinationFloor];
                    destinationCarQueue = frontCarQueues.get(destinationFloor);
                    destinationDoorPos = frontDoorPosition;
                    destinationDoorRev = frontDoorReversal;
                    break;
                case BACK:
                    destinationAtFloor = backAtFloors[destinationFloor];
                    carButton = backCarButtons[destinationFloor];
                    destinationCarQueue = backCarQueues.get(destinationFloor);
                    destinationDoorPos = backDoorPosition;
                    destinationDoorRev = backDoorReversal;
                    break;
            }

            log("minWidth=" + minWidth + " instantiated.");
        }

        public void doButton(Button button) {
            if (button == null) {
                throw new NullPointerException(
                        logMessage("button must not be null"));
            }

            //log("doButton(" + button + ")");

            switch (button.state) {
                case READY:
                    if (!button.pressedThisCycle(this) && !button.isLit() && button.isReady()) {
                        log("press");
                        button.state = Button.State.PRESSED;
                        button.start(this);
                        button.press();
                        //delay the button for a random amount of time between
                        //250 and 1000 ms
                        button.delay(randomDelay(250, 1000));
                        /*pressesRemaining = (int) Math.floor(
                        Math.random() * (4 - 1) + 1);*/
                        pressesRemaining = (int) Math.floor(
                                Harness.getRandomSource().getRandom().nextDouble() * (4 - 1) + 1);
                        --pressesRemaining;
                    }
                    break;

                case PRESSED:
                    if (button.isReady() && pressesRemaining > 0) {
                        log("release");
                        button.state = Button.State.WAIT_FOR_NEXT_PRESS;
                        button.release();
                        //delay the button for a random amount of time between
                        //200 and 400 ms
                        button.delay(randomDelay(200, 400));
                    } else if (button.isReady() && pressesRemaining == 0) {
                        log("finish");
                        button.state = Button.State.READY;
                        button.release();
                        button.delay(randomDelay(1000, 2000));
                        button.finish();
                    }
                    break;

                case WAIT_FOR_NEXT_PRESS:
                    if (button.isReady()) {
                        log("press");
                        button.state = Button.State.PRESSED;
                        button.press();
                        button.delay(randomDelay(250, 1000));
                        --pressesRemaining;
                    }
                    break;

                default:
                    throw new IllegalStateException(button.toString());
            }
        }

        public void doState() {
            switch (state) {
                case WAITING_IN_HALLWAY:
                    doWaitingInHallway();
                    break;
                case ATTEMPTING_ENTRY:
                    doAttemptingEntry();
                    break;
                case ABORTING_ENTRY:
                    doAbortingEntry();
                    break;
                case WAITING_IN_CAR:
                    doWaitingInCar();
                    break;
                case ATTEMPTING_EXIT:
                    doAttemptingExit();
                    break;
                case ABORTING_EXIT:
                    doAbortingExit();
                    break;
                case ATTEMPTING_BACKOUT:
                    doAttemptingBackout();
                    break;
                default:
                    throw new RuntimeException(
                            logMessage("unhandled state " + state));
            }
        }

        private void doAbortingEntry() {
            if (carWeightAlarm.isRinging()) {
                // #transition BE 2
                log("BE 2");
                actionTime = SimTime.add(Harness.getTime(), getAlarmDelay());
                return;
            }
            if (Harness.getTime().isGreaterThanOrEqual(actionTime)) {
                // #transition BE 1
                log("BE 1 -> " + PassengerState.WAITING_IN_HALLWAY);
                state = PassengerState.WAITING_IN_HALLWAY;
                return;
            }
        }

        private void doAbortingExit() {
            if (Harness.getTime().isGreaterThanOrEqual(actionTime)) {
                // #transition BX 1
                log("BX 1 -> " + PassengerState.WAITING_IN_CAR);
                state = PassengerState.WAITING_IN_CAR;
                return;
            }
        }

        private void doAttemptingBackout() {
            if (Harness.getTime().isGreaterThanOrEqual(actionTime)) {
                // #transition AB 1
                log("AB 1 -> "
                        + PassengerState.ABORTING_ENTRY);
                state = PassengerState.ABORTING_ENTRY;
                actionTime = SimTime.add(Harness.getTime(), getAlarmDelay());
                carStack.remove(this);
                fi.sendOnce(PersonWeightPayload.getWriteablePayload(-weight));
                destinationCarQueue.remove(this);
                hallQueue.add(0, this);
                startDoorRev[0].set(false);
                startDoorRev[1].set(false);
                return;
            }
            if (Harness.getTime().isGreaterThanOrEqual(abortTime)) {
                // #transition AB 4
                log("AB 4 -> "
                        + PassengerState.ABORTING_EXIT);
                state = PassengerState.ABORTING_EXIT;
                abortTime = SimTime.FOREVER;
                actionTime = SimTime.add(Harness.getTime(), getAbortDelay());
                startDoorRev[0].set(false);
                startDoorRev[1].set(false);
                return;
            }
            if (startDoorPos[0].position() + startDoorPos[1].position() >= minWidth) {
                // #transition AB 2
                log("AB 2");
                startDoorRev[0].set(false);
                startDoorRev[1].set(false);
                abortTime = SimTime.add(Harness.getTime(), getReversalDelay());
                return;
            } else {
                // #transition AB 3
                log("AB 3 (TRIGGER DOOR REVERSAL)");
                startDoorRev[0].set(true);
                startDoorRev[1].set(true);
                return;
            }
        }

        private void doAttemptingEntry() {
            if (carWeightAlarm.isRinging()) {
                // #transition AE 4
                log("AE 4 -> " + PassengerState.ABORTING_ENTRY);
                state = PassengerState.ABORTING_ENTRY;
                actionTime = SimTime.add(Harness.getTime(), getAlarmDelay());
                startDoorRev[0].set(false);
                startDoorRev[1].set(false);
                return;
            }
            if (Harness.getTime().isGreaterThanOrEqual(actionTime)) {
                // #transition AE 1
                log("AE 1 -> " + PassengerState.WAITING_IN_CAR);
                state = PassengerState.WAITING_IN_CAR;
                startDoorRev[0].set(false);
                startDoorRev[1].set(false);
                hallQueue.remove(this);
                destinationCarQueue.add(this);
                if (carStack.contains(this)) {
                    throw new RuntimeException(
                            logMessage("tried to add Passenger to Car twice."));
                }
                carStack.add(0, this);
                fi.sendOnce(PersonWeightPayload.getWriteablePayload(weight));
                return;
            }
            if (Harness.getTime().isGreaterThanOrEqual(abortTime)) {
                // #transition AE 5
                log("AE 5 -> " + PassengerState.ABORTING_ENTRY);
                state = PassengerState.ABORTING_ENTRY;
                actionTime = SimTime.add(Harness.getTime(), getAbortDelay());
                abortTime = SimTime.FOREVER;
                startDoorRev[0].set(false);
                startDoorRev[1].set(false);
                return;
            }
            if (startDoorPos[0].position() + startDoorPos[1].position() < minWidth) {
                // #transition AE 3
                log("AE 3 (TRIGGER DOOR REVERSAL)");
                startDoorRev[0].set(true);
                startDoorRev[1].set(true);
                return;
            } else {
                // #transition AE 2
                log("AE 2");
                abortTime = SimTime.add(Harness.getTime(), getReversalDelay());
                startDoorRev[0].set(false);
                startDoorRev[1].set(false);
                return;
            }
        }

        private void doAttemptingExit() {
            if (Harness.getTime().isGreaterThanOrEqual(actionTime)) {
                // #transition AX 1
                log("AX 1 -> DELIVERED");
                state = PassengerState.DELIVERED;
                destinationCarQueue.remove(this);
                carStack.remove(this);
                fi.sendOnce(PersonWeightPayload.getWriteablePayload(-weight));
                destinationDoorRev[0].set(false);
                destinationDoorRev[1].set(false);
                /* The following will only happen if some components are
                 * malfunctioning.  Don't throw an exception, because this
                 * might be a problem in a student's controller, and they
                 * would probably like to know about it.
                 */
                if (destinationAtFloor.getFloor() != cpiState.floor()) {
                    log("I got off on the wrong floor!");
                    state = null;
                } else {
                    arrivalTime = Harness.getTime();
                }
                return;
            }
            if (Harness.getTime().isGreaterThanOrEqual(abortTime)) {
                // #transition AX 4
                log("AX 4 -> ABORTING_EXIT");
                state = PassengerState.ABORTING_EXIT;
                actionTime = SimTime.add(Harness.getTime(), getAbortDelay());
                abortTime = SimTime.FOREVER;
                destinationDoorRev[0].set(false);
                destinationDoorRev[1].set(false);
                return;
            }
            if (destinationDoorPos[0].position() + destinationDoorPos[1].position() >= minWidth) {
                // #transition AX 2
                log("AX 2");
                abortTime = SimTime.add(Harness.getTime(), getAbortDelay());
                destinationDoorRev[0].set(false);
                        destinationDoorRev[1].set(false);
                return;
            } else {
                // #transition AX 3
                log("AX 3 (TRIGGER DOOR REVERSAL)");
                destinationDoorRev[0].set(false);
                        destinationDoorRev[1].set(false);
            }
        }

        private void doWaitingInCar() {
            doButton(carButton);
            if (destinationAtFloor.getFloor() == cpiState.floor() && destinationDoorPos[0].position() + destinationDoorPos[1].position() >= minWidth && destinationCarQueue.get(0) == this) {
                // #transition WC 1
                log("WC 1 -> ATTEMPTING_EXIT");
                state = PassengerState.ATTEMPTING_EXIT;
                actionTime = SimTime.add(Harness.getTime(), getTransitDelay());
                abortTime = SimTime.FOREVER;
                return;
            }
            if (carWeightAlarm.isRinging() && carStack.get(0) == this && startDoorPos[0].position() + startDoorPos[1].position() >= minWidth) {
                // #transition WC 2
                log("WC 2 -> ATTEMPTING_BACKOUT");
                state = PassengerState.ATTEMPTING_BACKOUT;
                actionTime = SimTime.add(Harness.getTime(), getBackoutDelay());
                return;
            }
        }

        private void doWaitingInHallway() {
            doButton(hallButton);
            if (carWeightAlarm.isRinging()) {
                // #transition WH 2
                log("WH 2 -> ABORTING_ENTRY");
                state = PassengerState.ABORTING_ENTRY;
                actionTime = SimTime.add(Harness.getTime(), getAlarmDelay());
                abortTime = SimTime.FOREVER;
                return;
            }
            if (startAtFloor.value() && (startDoorPos[0].position() + startDoorPos[1].position() >= minWidth) && !otherLantern.lighted() && !carWeightAlarm.isRinging() && hallQueue.get(0) == this && startCarQueue.isEmpty() && carStack.size() < MAX_PASSENGERS_IN_CAR) {
                // #transition WH 1
                log("WH 1 -> ATTEMPTING_ENTRY");
                state = PassengerState.ATTEMPTING_ENTRY;
                actionTime = SimTime.add(Harness.getTime(), getTransitDelay());
                abortTime = SimTime.FOREVER;
                return;
            }
        }

        /**
         * Returns a value in the range [2*<tt>Timer.SECONDS</tt>,
         * 4*<tt>Timer.SECONDS</tt>].
         */
        private SimTime getAbortDelay() {
            return randomDelay(2000, 4000);
        }

        /**
         * Returns 15*<tt>Timer.SECONDS</tt>.
         */
        private SimTime getAlarmDelay() {
            return new SimTime(15, SimTimeUnit.SECOND);
        }

        public SimTime getArrivalTime() {
            return arrivalTime;
        }

        /**
         * Returns a value in the range [2*<tt>Timer.SECONDS</tt>,
         * 4*<tt>Timer.SECONDS</tt>].
         */
        private SimTime getBackoutDelay() {
            return randomDelay(2000, 4000);
        }

        public int getDestinationFloor() {
            return destinationFloor + 1;
        }

        public Hallway getDestinationHallway() {
            return destinationHallway;
        }

        public long getPersonNumber() {
            return passengerID;
        }

        /**
         * Returns the time between the doors closing to the point where the
         * Passenger can no longer fit in the space between them, and the
         * Passenger aborting a transition.
         */
        private SimTime getReversalDelay() {
            return new SimTime(100, SimTimeUnit.MILLISECOND);
        }

        public int getStartFloor() {
            return startFloor + 1;
        }

        public Hallway getStartHallway() {
            return startHallway;
        }

        public SimTime getStartTime() {
            return startTime;
        }

        public SimTime getDeliveryTime() {
            return SimTime.subtract(arrivalTime, startTime);
        }

        /**
         * Returns a value in the range [<tt>Timer.SECOND</tt>,
         * 2*<tt>Timer.SECONDS</tt>] inclusive.
         */
        private SimTime getTransitDelay() {
            // #transition WH1, WC1
            return randomDelay(1000, 2000);
        }

        public boolean isDelivered() {
            return state == PassengerState.DELIVERED;
        }

        public boolean isFinished() {
            return state == PassengerState.DELIVERED || state == null;
        }

        public String logMessage(String s) {
            return name() + ": @TIME " + Harness.getTime() + ": " + s;
        }

        public void log(String s) {
            if (verbose) {
                Harness.log(toString() + "#" + actionTime, s);
            }
        }

        public String name() {
            return "Passenger" + passengerID;
        }

        public SimTime serviceTime() {
            if (state != PassengerState.DELIVERED) {
                throw new IllegalStateException(logMessage(
                        "I was never delivered, why are you asking for my service time?"));
            }

            return SimTime.subtract(arrivalTime, startTime);
        }

        public void start() {
            if (passengers.contains(this)) {
                throw new IllegalStateException(
                        logMessage("passenger already started"));
            }
            passengers.add(this);

            switch (state) {

                case WAITING_IN_HALLWAY:
                    hallQueue.add(this);
                    break;

                case WAITING_IN_CAR:
                    carStack.add(0, this);
                    fi.sendOnce(PersonWeightPayload.getWriteablePayload(weight));
                    destinationCarQueue.add(this);
                    break;

                default:
                    throw new IllegalStateException(logMessage(
                            "start() called when in invalid state " + state));

            }
        }

        @Override
        public String toString() {
            return name() + ":" + state;
        }
    }
    /**
     * The maximum number of passengers that can be in the car at one time.
     * If this many passengers are in the car, other passengers will not
     * attempt to board.
     */
    private static final int MAX_PASSENGERS_IN_CAR = 15;
    /**
     * The time in microseconds between physical framework messages, such as
     * button presses.
     */
    private static final SimTime period = new SimTime(10000,
            SimTimeUnit.MICROSECOND);
    /**
     * The Passengers who are in the Car.  The first Passenger in the list is
     * the most recent one to enter the Car.
     */
    private List<Passenger> carStack;
    /**
     * A unique serial number that will be assigned to the next Passenger that
     * is created.
     */
    private long passengerCounter = 0;
    /**
     * Indicates whether this PassengerController will produce detailed
     * messages about its actions and decisions.
     */
    private boolean verboseMode;
    private final NetworkConnection fi;
    private final Collection<Passenger> passengers;
    private final Button[] buttons;
    private final List<List<Passenger>> frontCarQueues;
    private final List<List<Passenger>> backCarQueues;
    private final List<List<Passenger>> backUpHallQueues;
    private final List<List<Passenger>> backDownHallQueues;
    private final List<List<Passenger>> frontUpHallQueues;
    private final List<List<Passenger>> frontDownHallQueues;

    /* interface between a passenger and the environment */
    private final ReadableCarPositionIndicatorPayload cpiState;
    private final ReadableCarWeightAlarmPayload carWeightAlarm;
    private final ReadableCarLanternPayload upLantern;
    private final ReadableCarLanternPayload downLantern;
    private final ReadableAtFloorPayload[] frontAtFloors;
    private final ReadableAtFloorPayload[] backAtFloors;
    private final ReadableDoorPositionPayload[] frontDoorPosition;
    private final ReadableDoorPositionPayload[] backDoorPosition;
    private final WriteableDoorReversalPayload[] frontDoorReversal;
    private final WriteableDoorReversalPayload[] backDoorReversal;
    private final CarButton[] frontCarButtons;
    private final CarButton[] backCarButtons;
    private final WriteableCarCallPayload[] frontCarCalls;
    private final WriteableCarCallPayload[] backCarCalls;
    private final ReadableCarLightPayload[] frontCarLights;
    private final ReadableCarLightPayload[] backCarLights;
    private final HallButton[] frontUpHallButtons;
    private final HallButton[] frontDownHallButtons;
    private final HallButton[] backUpHallButtons;
    private final HallButton[] backDownHallButtons;
    private final WriteableHallCallPayload[] frontUpHallCalls;
    private final WriteableHallCallPayload[] frontDownHallCalls;
    private final WriteableHallCallPayload[] backUpHallCalls;
    private final WriteableHallCallPayload[] backDownHallCalls;
    private final ReadableHallLightPayload[] frontUpHallLights;
    private final ReadableHallLightPayload[] frontDownHallLights;
    private final ReadableHallLightPayload[] backUpHallLights;
    private final ReadableHallLightPayload[] backDownHallLights;

    public PassengerController(boolean verboseMode) {
        this.verboseMode = verboseMode;
        fi = Harness.getPhysicalNetwork().getFrameworkConnection(this);

        passengers = new ArrayList<Passenger>();

        carStack = new LinkedList<Passenger>();

        frontAtFloors = new ReadableAtFloorPayload[Elevator.numFloors];
        backAtFloors = new ReadableAtFloorPayload[Elevator.numFloors];
        for (int i = 1; i <= Elevator.numFloors; i++) {
            frontAtFloors[i - 1] = AtFloorPayload.getReadablePayload(i, Hallway.FRONT);
            backAtFloors[i - 1] = AtFloorPayload.getReadablePayload(i, Hallway.BACK);
        }

        for (ReadableAtFloorPayload p : frontAtFloors) {
            fi.registerTimeTriggered(p);
        }
        for (ReadableAtFloorPayload p : backAtFloors) {
            fi.registerTimeTriggered(p);
        }

        frontDoorPosition = new ReadableDoorPositionPayload[]{
                    DoorPositionPayload.getReadablePayload(Hallway.FRONT, Side.LEFT),
                    DoorPositionPayload.getReadablePayload(Hallway.FRONT, Side.RIGHT)
                };

        backDoorPosition = new ReadableDoorPositionPayload[]{
                    DoorPositionPayload.getReadablePayload(Hallway.BACK, Side.LEFT),
                    DoorPositionPayload.getReadablePayload(Hallway.BACK, Side.RIGHT),};

        frontDoorReversal = new WriteableDoorReversalPayload[]{
                    DoorReversalPayload.getWriteablePayload(Hallway.FRONT, Side.LEFT),
                    DoorReversalPayload.getWriteablePayload(Hallway.FRONT, Side.RIGHT),};

        backDoorReversal = new WriteableDoorReversalPayload[]{
                    DoorReversalPayload.getWriteablePayload(Hallway.BACK, Side.LEFT),
                    DoorReversalPayload.getWriteablePayload(Hallway.BACK, Side.RIGHT),};

        fi.registerTimeTriggered(frontDoorPosition[0]);
        fi.registerTimeTriggered(frontDoorPosition[1]);
        fi.sendTimeTriggered(frontDoorReversal[0], period);
        fi.sendTimeTriggered(frontDoorReversal[1], period);

        fi.registerTimeTriggered(backDoorPosition[0]);
        fi.registerTimeTriggered(backDoorPosition[1]);
        fi.sendTimeTriggered(backDoorReversal[0], period);
        fi.sendTimeTriggered(backDoorReversal[1], period);

        upLantern = CarLanternPayload.getReadablePayload(Direction.UP);
        downLantern = CarLanternPayload.getReadablePayload(Direction.DOWN);

        fi.registerTimeTriggered(upLantern);
        fi.registerTimeTriggered(downLantern);

        frontUpHallLights = new ReadableHallLightPayload[Elevator.numFloors];
        frontDownHallLights = new ReadableHallLightPayload[Elevator.numFloors];
        backUpHallLights = new ReadableHallLightPayload[Elevator.numFloors];
        backDownHallLights = new ReadableHallLightPayload[Elevator.numFloors];
        frontCarLights = new ReadableCarLightPayload[Elevator.numFloors];
        backCarLights = new ReadableCarLightPayload[Elevator.numFloors];

        frontUpHallCalls = new WriteableHallCallPayload[Elevator.numFloors];
        frontDownHallCalls = new WriteableHallCallPayload[Elevator.numFloors];
        backUpHallCalls = new WriteableHallCallPayload[Elevator.numFloors];
        backDownHallCalls = new WriteableHallCallPayload[Elevator.numFloors];
        frontCarCalls = new WriteableCarCallPayload[Elevator.numFloors];
        backCarCalls = new WriteableCarCallPayload[Elevator.numFloors];

        cpiState = CarPositionIndicatorPayload.getReadablePayload();
        fi.registerTimeTriggered(cpiState);

        carWeightAlarm = CarWeightAlarmPayload.getReadablePayload();
        fi.registerTimeTriggered(carWeightAlarm);

        frontCarQueues = new Vector<List<Passenger>>(Elevator.numFloors);
        backCarQueues = new Vector<List<Passenger>>(Elevator.numFloors);
        frontUpHallQueues = new Vector<List<Passenger>>(Elevator.numFloors);
        frontDownHallQueues = new Vector<List<Passenger>>(Elevator.numFloors);
        backUpHallQueues = new Vector<List<Passenger>>(Elevator.numFloors);
        backDownHallQueues = new Vector<List<Passenger>>(Elevator.numFloors);

        frontCarButtons = new CarButton[Elevator.numFloors];
        backCarButtons = new CarButton[Elevator.numFloors];
        frontUpHallButtons = new HallButton[Elevator.numFloors];
        frontDownHallButtons = new HallButton[Elevator.numFloors];
        backUpHallButtons = new HallButton[Elevator.numFloors];
        backDownHallButtons = new HallButton[Elevator.numFloors];
        buttons = new Button[6 * Elevator.numFloors];

        for (int i = 0; i < Elevator.numFloors; i++) {
            frontUpHallLights[i] = HallLightPayload.getReadablePayload(i + 1, Hallway.FRONT, Direction.UP);
            frontDownHallLights[i] = HallLightPayload.getReadablePayload(i + 1, Hallway.FRONT, Direction.DOWN);
            backUpHallLights[i] = HallLightPayload.getReadablePayload(i + 1, Hallway.BACK, Direction.UP);
            backDownHallLights[i] = HallLightPayload.getReadablePayload(i + 1, Hallway.BACK, Direction.DOWN);

            frontUpHallCalls[i] = HallCallPayload.getWriteablePayload(i + 1, Hallway.FRONT, Direction.UP);
            frontDownHallCalls[i] = HallCallPayload.getWriteablePayload(i + 1, Hallway.FRONT, Direction.DOWN);
            backUpHallCalls[i] = HallCallPayload.getWriteablePayload(i + 1, Hallway.BACK, Direction.UP);
            backDownHallCalls[i] = HallCallPayload.getWriteablePayload(i + 1, Hallway.BACK, Direction.DOWN);

            frontCarLights[i] = CarLightPayload.getReadablePayload(i + 1, Hallway.FRONT);
            backCarLights[i] = CarLightPayload.getReadablePayload(i + 1, Hallway.BACK);

            frontCarCalls[i] = CarCallPayload.getWriteablePayload(i + 1, Hallway.FRONT);
            backCarCalls[i] = CarCallPayload.getWriteablePayload(i + 1, Hallway.BACK);

            fi.registerTimeTriggered(frontUpHallLights[i]);
            fi.registerTimeTriggered(frontDownHallLights[i]);
            fi.registerTimeTriggered(backUpHallLights[i]);
            fi.registerTimeTriggered(backDownHallLights[i]);

            fi.sendTimeTriggered(frontUpHallCalls[i], period);
            fi.sendTimeTriggered(frontDownHallCalls[i], period);
            fi.sendTimeTriggered(backUpHallCalls[i], period);
            fi.sendTimeTriggered(backDownHallCalls[i], period);

            fi.registerTimeTriggered(frontCarLights[i]);
            fi.registerTimeTriggered(backCarLights[i]);

            fi.sendTimeTriggered(frontCarCalls[i], period);
            fi.sendTimeTriggered(backCarCalls[i], period);

            frontCarQueues.add(new LinkedList<Passenger>());
            backCarQueues.add(new LinkedList<Passenger>());
            frontUpHallQueues.add(new LinkedList<Passenger>());
            frontDownHallQueues.add(new LinkedList<Passenger>());
            backUpHallQueues.add(new LinkedList<Passenger>());
            backDownHallQueues.add(new LinkedList<Passenger>());

            frontCarButtons[i] = new CarButton(false,
                    frontCarCalls[i], frontCarLights[i]);
            backCarButtons[i] = new CarButton(false,
                    backCarCalls[i], backCarLights[i]);
            frontUpHallButtons[i] = new HallButton(false,
                    frontUpHallCalls[i], frontUpHallLights[i]);
            frontDownHallButtons[i] = new HallButton(false,
                    frontDownHallCalls[i], frontDownHallLights[i]);
            backUpHallButtons[i] = new HallButton(false,
                    backUpHallCalls[i], backUpHallLights[i]);
            backDownHallButtons[i] = new HallButton(false,
                    backDownHallCalls[i], backDownHallLights[i]);

            buttons[6 * i] = frontCarButtons[i];
            buttons[6 * i + 1] = backCarButtons[i];
            buttons[6 * i + 2] = frontUpHallButtons[i];
            buttons[6 * i + 3] = backUpHallButtons[i];
            buttons[6 * i + 4] = frontDownHallButtons[i];
            buttons[6 * i + 5] = backDownHallButtons[i];
        }
    }

    /**
     * Returns true if and only if there are no Passengers in any CarQueue
     * or HallQueue.
     */
    public boolean isEmpty() {
        return passengers.isEmpty();
    }

    public String logMessage(String s) {
        return "PassengerController: @TIME " + Harness.getTime() + ": " + s;
    }

    public void log(String s) {
        if (verboseMode) {
            Harness.log("PassengerController", s);
        }
    }

    /**
     * Returns a new Passenger.
     *
     * The Passenger will not interact with the environment (and will not
     * be placed in any queue) until setFocus() is called.
     */
    public Passenger makePerson(SimTime startTime, int startFloor,
            Hallway startHallway, int destFloor, Hallway destHallway) {
        //int minWidth = (int) Math.round(Math.random() * (45 - 20 + 1) + 20);
        int minWidth = (int) Math.round(Harness.getRandomSource().getRandom().nextDouble() * (45 - 20 + 1) + 20);
        Passenger p = new Passenger(startTime, startFloor, startHallway,
                destFloor, destHallway, minWidth, verboseMode);
        return p;
    }

    /**
     * Returns a <code>Simtime</code> that represents a random time
     * interval between the specified bounds.
     * @param minMs
     * minimum delay time, in milliseconds
     * @param maxMs
     * maximum delay time, in milliseconds
     * @return
     * a <code>Simtime</code> that represents a random time interval
     * between the specified bounds.
     */
    private SimTime randomDelay(long minMs, long maxMs) {
        return new SimTime(
                Math.round(Math.floor(Harness.getRandomSource().getRandom().nextDouble() * (maxMs - minMs) + minMs)),
                SimTimeUnit.MILLISECOND);
    }

    public void run() {
        if (carWeightAlarm.isRinging()) {
            log(Arrays.toString(frontDoorPosition) + " "
                    + Arrays.toString(backDoorPosition));
            log("car stack = " + carStack);
        }
        for (Iterator<Passenger> pi = passengers.iterator();
                pi.hasNext();) {
            Passenger p = pi.next();
            p.doState();
            if (p.isFinished()) {
                pi.remove();
            }
        }
        for (Button b : buttons) {
            b.doState();
        }
    }

    @Override
    public String toString() {
        return "PassengerController";
    }
}
