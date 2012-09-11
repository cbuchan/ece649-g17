package simulator.framework;

import jSimPack.SimTime;
import jSimPack.SimTime.SimTimeUnit;
import simulator.framework.PassengerController.Passenger;
import java.util.*;

abstract class Button {

    enum State {

        READY,
        PRESSED,
        WAIT_FOR_NEXT_PRESS
    }
    State state;
    private SimTime actionTime = SimTime.ZERO;
    private SimTime ONE_SECOND = new SimTime(1, SimTimeUnit.SECOND);
    private Set<Passenger> startedPushing;
    private boolean verbose;

    public Button(boolean verbose) {
        this.verbose = verbose;
        state = State.READY;
        startedPushing = new HashSet<Passenger>();
    }

    abstract void press();

    abstract void release();

    abstract boolean isLit();

    void doState() {
        switch (state) {
            case READY:
                if (isLit() || Harness.getTime().isGreaterThanOrEqual(SimTime.add(actionTime, ONE_SECOND))) {
                    startedPushing.clear();
                    log("reset");
                }
                break;

            case PRESSED:
                if (Harness.getTime().isGreaterThanOrEqual(actionTime)) {
                    log("release");
                    state = State.WAIT_FOR_NEXT_PRESS;
                    release();
                }
                break;

            case WAIT_FOR_NEXT_PRESS:
                if (Harness.getTime().isGreaterThanOrEqual(SimTime.add(actionTime, ONE_SECOND))) {
                    log("timeout");
                    state = State.READY;
                }
                break;
        }
    }

    void delay(SimTime time) {
        actionTime = SimTime.add(Harness.getTime(),time);
    }

    boolean isReady() {
        return Harness.getTime().isGreaterThanOrEqual(actionTime);
    }

    void start(Passenger p) {
        log("start ",p);
        startedPushing.add(p);
    }

    /**
     * Returns true if and only if the specified Passenger has started
     * a push/release cycle since the light last transitioned from on to off.
     */
    boolean pressedThisCycle(Passenger p) {
        return startedPushing.contains(p);
    }

    void finish() {
    }

    void log(Object... o) {
        if (verbose) {
            Harness.log(toString(), o);
        }
    }
}
