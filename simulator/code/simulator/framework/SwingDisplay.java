/**
 * @brief
 * Displays the status of the elevator, using Java Swing.
 *
 * @author
 * Kenny Stauffer (kstauffer@cmu.edu)
 */
package simulator.framework;

import jSimPack.BreakpointPrinter;
import jSimPack.BreakpointListener;
import jSimPack.FutureEvent;
import jSimPack.FutureEventListener;
import jSimPack.SimTime;
import jSimPack.SimTime.SimTimeUnit;
import simulator.payloads.*;
import simulator.elevatormodules.DriveObject;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import simulator.elevatorcontrol.DesiredFloorCanPayloadTranslator;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.elevatormodules.passengers.PassengerHandler;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CarCallPayload.ReadableCarCallPayload;
import simulator.payloads.CarLanternPayload.ReadableCarLanternPayload;
import simulator.payloads.CarLightPayload.ReadableCarLightPayload;
import simulator.payloads.CarPositionIndicatorPayload.ReadableCarPositionIndicatorPayload;
import simulator.payloads.CarPositionPayload.ReadableCarPositionPayload;
import simulator.payloads.CarWeightAlarmPayload.ReadableCarWeightAlarmPayload;
import simulator.payloads.CarWeightPayload.ReadableCarWeightPayload;
import simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;
import simulator.payloads.DoorOpenPayload.ReadableDoorOpenPayload;
import simulator.payloads.DoorPositionPayload.ReadableDoorPositionPayload;
import simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;
import simulator.payloads.HallCallPayload.ReadableHallCallPayload;
import simulator.payloads.HallLightPayload.ReadableHallLightPayload;

/**
 * a GUI display that shows the elevator state during the simulation and allows
 * the user to adjust the speed of the simulation.
 * @author justinr2
 */
public class SwingDisplay extends javax.swing.JFrame implements BreakpointListener {

    private final static Color LEVEL_OFF_COLOR = new Color(0.8f,0.8f,0.8f);
    private final static Color LEVEL_ON_COLOR = Color.WHITE;
    private final static Color UP_LANTERN_COLOR = Color.RED;
    private final static Color DOWN_LANTERN_COLOR = Color.GREEN;
    private final static Color OFF_LANTERN_COLOR = Color.getHSBColor(0.0f,0.0f,0.93f);
    private final static String CAR_PASSENGER_COUNT_STR = "# Pass. in Car: ";


    private static final long serialVersionUID = 0;
    private final Collection<Widget> widgets;
    private final NetworkConnection canNetworkConnection;
    private final NetworkConnection physicalConnection;
    private final PassengerHandler passengerHandler;
    private final boolean verbose;
    private final ReadableCarPositionPayload posPayload;
    private final ReadableDriveSpeedPayload speedPayload;
    private final ReadableCarWeightPayload weightPayload;
    private final ReadableCarPositionIndicatorPayload indPayload;
    private final ReadableCarWeightAlarmPayload alarmPayload;
    private final ReadableCarLanternPayload upLanternPayload;
    private final ReadableCarLanternPayload downLanternPayload;
    private final LevelingPayload.ReadableLevelingPayload upLevelPayload;
    private final LevelingPayload.ReadableLevelingPayload downLevelPayload;
    private final DesiredFloorCanPayloadTranslator desFloorPayloadTranslator;
    private final JSlider posSlider;
    private final JTextField timeField;
    private final JTextField posField;
    private final JSlider speedSlider;
    private final JTextField speedField;
    private final JTextField weightField;
    private final JTextField indField;
    private final JCheckBox upLanternCheckBox;
    private final JCheckBox downLanternCheckBox;
    private final JCheckBox upLevelSensorCheckBox;
    private final JCheckBox downLevelSensorCheckBox;
    private final JLabel carPassengerCount;
    private final JTextField desDirTextField;
    private final JSpinner realtimeField;
    private final CallPanel frontCallPanel;
    private final CallPanel backCallPanel;
    private final JTextField breakpointField;
    private final JButton breakpointButton;
    private SimTime breakpoint;

    //number of nanoseconds between GUI refreshes
    private final static long REFRESH_INTERVAL_MS = 50;
    private java.util.Timer threadTimer = new java.util.Timer();

    private static interface Widget {

        public void update();
    }

    private class CallPanel extends JPanel {

        private static final long serialVersionUID = 0;
        private final JLabel[] passengerCounts;
        final Hallway hallway;

        public CallPanel(Hallway hallway) {
            super();
            this.hallway = hallway;

            setLayout(new GridBagLayout());
            GridBagConstraints cons = new GridBagConstraints();


            JLabel pcLabel = new JLabel();
            pcLabel.setText("#P");
            pcLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cons.gridy = 0;
            cons.gridx = 0;
            cons.weighty = 1.0;
            add(pcLabel, cons);


            JLabel upLabel = new JLabel();
            upLabel.setText("U");
            upLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cons.gridy = 0;
            cons.gridx = 1;
            cons.weighty = 1.0;
            add(upLabel, cons);

            JLabel downLabel = new JLabel();
            downLabel.setText("D");
            downLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cons = new GridBagConstraints();
            cons.gridy = 0;
            cons.gridx = 2;
            cons.weighty = 1.0;
            add(downLabel, cons);

            JLabel carLabel = new JLabel();
            carLabel.setText("C");
            carLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cons = new GridBagConstraints();
            cons.gridy = 0;
            cons.gridx = 3;
            cons.weighty = 1.0;
            add(carLabel);

            passengerCounts = new JLabel[Elevator.numFloors];

            int gridy = 1;
            for (int i = Elevator.numFloors; i >= 1; --i, ++gridy) {



                JLabel pc = new JLabel("0");
                if (!Elevator.hasLanding(i, hallway)) {
                    pc.setText("");
                }
                passengerCounts[i-1] = pc;
//                Dimension d = pc.getSize();
//                d.width = 50;
//                pc.setSize(d);
//                pc.setMinimumSize(d);
//                pc.setMaximumSize(d);

                cons = new GridBagConstraints();
                cons.weighty = 1.0;
                cons.gridy = gridy;
                cons.gridx = 0;
                add(pc, cons);


                JComponent up = new HallCallCheckBox(i, Direction.UP);
                if (i == Elevator.numFloors || !Elevator.hasLanding(i, hallway)) {
                    up.setEnabled(false);
                } else
                    widgets.add((Widget)up);
                cons = new GridBagConstraints();
                cons.weighty = 1.0;
                cons.gridy = gridy;
                cons.gridx = 1;
                add(up, cons);

                JComponent down = new HallCallCheckBox(i, Direction.DOWN);
                if (i == 1 || !Elevator.hasLanding(i, hallway)) {
                    down.setEnabled(false);
                } else
                    widgets.add((Widget)down);
                cons = new GridBagConstraints();
                cons.weighty = 1.0;
                cons.gridy = gridy;
                cons.gridx = 2;
                add(down, cons);

                JComponent car = new CarCallCheckBox(i);
                if (!Elevator.hasLanding(i, hallway)) {
                    car.setEnabled(false);
                } else
                    widgets.add((Widget)car);
                cons = new GridBagConstraints();
                cons.gridy = gridy;
                cons.gridx = 3;
                cons.weighty = 1.0;
                add(car, cons);
            }
        }

        private class CarCallCheckBox extends JCheckBox implements Widget {

            private static final long serialVersionUID = 0;
            final ReadableCarCallPayload call;
            final ReadableCarLightPayload light;

            public CarCallCheckBox(int floor) {
                super();
                setFocusable(false);
                call = CarCallPayload.getReadablePayload(floor, hallway);
                //canNetworkConnection.registerTimeTriggered(call);
                physicalConnection.registerTimeTriggered(call);
                light = CarLightPayload.getReadablePayload(floor, hallway);
                physicalConnection.registerTimeTriggered(light);
            }

            public void update() {
                setSelected(call.pressed());
                setBackground(light.lighted() ? Color.GREEN : Color.getHSBColor(0.0f, 0.0f, 0.93f));
            }
        }

        private class HallCallCheckBox extends JCheckBox implements Widget {

            private static final long serialVersionUID = 0;
            final ReadableHallCallPayload call;
            final ReadableHallLightPayload light;

            public HallCallCheckBox(int floor, Direction direction) {
                super();
                setFocusable(false);
                call = HallCallPayload.getReadablePayload(floor, hallway, direction);
                //canNetworkConnection.registerTimeTriggered(call);
                physicalConnection.registerTimeTriggered(call);
                light = HallLightPayload.getReadablePayload(floor, hallway, direction);
                physicalConnection.registerTimeTriggered(light);
            }

            public void update() {
                setSelected(call.pressed());
                setBackground(light.lighted() ? Color.GREEN : Color.getHSBColor(0.0f, 0.0f, 0.93f));
            }
        }
    }

    private class DoorPanel extends JPanel implements Widget {

        private static final long serialVersionUID = 0;
        final Hallway hallway;
        final Side side;
        final ReadableDoorMotorPayload motor;
        final ReadableDoorOpenPayload opened;
        final ReadableDoorClosedPayload closed;
        final ReadableDoorReversalPayload rev;
        final ReadableDoorPositionPayload pos;
        final JTextField commandField;
        final JCheckBox openedCheckBox;
        final JCheckBox closedCheckBox;
        final JCheckBox revCheckBox;
        final JSlider posSlider;

        public DoorPanel(Hallway hallway, Side side) {
            super();
            this.hallway = hallway;
            this.side = side;

            motor = DoorMotorPayload.getReadablePayload(hallway, side);
            opened = DoorOpenPayload.getReadablePayload(hallway, side);
            closed = DoorClosedPayload.getReadablePayload(hallway, side);
            rev = DoorReversalPayload.getReadablePayload(hallway, side);
            pos = DoorPositionPayload.getReadablePayload(hallway, side);
            physicalConnection.registerTimeTriggered(motor);
            physicalConnection.registerTimeTriggered(opened);
            physicalConnection.registerTimeTriggered(closed);
            physicalConnection.registerTimeTriggered(rev);
            physicalConnection.registerTimeTriggered(pos);

            setLayout(new GridBagLayout());
            GridBagConstraints cons;

            /* door command label + text field */

            JLabel commandLabel = new JLabel("Command");
            cons = new GridBagConstraints();
            cons.gridy = 0;
            cons.gridx = 0;
            cons.ipadx = 5;
            cons.anchor = GridBagConstraints.EAST;
            add(commandLabel, cons);

            commandField = new JTextField();
            commandField.setFocusable(false);
            cons = new GridBagConstraints();
            cons.gridy = 0;
            cons.gridx = 1;
            cons.weightx = 1.0;
            cons.fill = GridBagConstraints.HORIZONTAL;
            add(commandField, cons);

            /* open/closed/reversal checkboxes */

            openedCheckBox = new JCheckBox("Opened");
            openedCheckBox.setFocusable(false);
            cons = new GridBagConstraints();
            cons.gridy = 1;
            cons.gridx = 0;
            add(openedCheckBox, cons);

            closedCheckBox = new JCheckBox("Closed");
            closedCheckBox.setFocusable(false);
            cons = new GridBagConstraints();
            cons.gridy = 1;
            cons.gridx = 1;
            add(closedCheckBox, cons);

            revCheckBox = new JCheckBox("Reversal");
            revCheckBox.setFocusable(false);
            cons = new GridBagConstraints();
            cons.gridy = 1;
            cons.gridx = 2;
            add(revCheckBox, cons);

            /* position slider */
            posSlider = new JSlider();
            posSlider.setFocusable(false);
            if (side == Side.LEFT) {
                posSlider.setInverted(true);
            }
            posSlider.setMaximum(50);
            cons = new GridBagConstraints();
            cons.gridy = 2;
            cons.gridx = 0;
            cons.gridwidth = GridBagConstraints.REMAINDER;
            cons.fill = GridBagConstraints.HORIZONTAL;
            add(posSlider, cons);
        }

        public void update() {
            commandField.setText(motor.command().toString());
            openedCheckBox.setSelected(opened.isOpen());
            closedCheckBox.setSelected(closed.isClosed());
            revCheckBox.setSelected(rev.isReversing());
            revCheckBox.setBackground(
                rev.isReversing() ?
                Color.RED :
                Color.getHSBColor(0.0f, 0.0f, 0.93f));
            posSlider.setValue((int)pos.position());
        }
    }

    /**
     * Creates a new SwingDisplay that repaints its components at a fixed
     * frequency according to the simulation clock.
     */
    public SwingDisplay(PassengerHandler passengerHandler, boolean verbose) {
        /* This is ugly, because most of it was done with Netbeans.  If you
         * want to mess around with figuring out all the values by hand, be my
         * guest.  --KSS
         */
        super();

        this.verbose = verbose;
        this.passengerHandler = passengerHandler;

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        canNetworkConnection = Harness.getCANNetwork().getFrameworkConnection();
        physicalConnection = Harness.getPhysicalNetwork().getFrameworkConnection();
        widgets = new ArrayList<Widget>();

        JPanel widgetPanel = new JPanel();
        widgetPanel.setLayout(new GridBagLayout());
        GridBagConstraints cons;

        JPanel panel = new JPanel();
        Dimension dims = new Dimension(120, 210);
        frontCallPanel = new CallPanel(Hallway.FRONT);
        frontCallPanel.setBorder(BorderFactory.createTitledBorder("Front"));
        frontCallPanel.setMinimumSize(dims);
        frontCallPanel.setMaximumSize(dims);
        frontCallPanel.setPreferredSize(dims);
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 0;
        cons.fill = GridBagConstraints.VERTICAL;
        panel.add(frontCallPanel, cons);

        posSlider = new JSlider();
        posSlider.setMinimum(0);
        posSlider.setMajorTickSpacing(5000);
        posSlider.setMaximum(posSlider.getMajorTickSpacing() * Elevator.numFloors);
        posSlider.setPaintTicks(true);
        posSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        posSlider.setFocusable(false);
        {
            JPanel panel2 = new JPanel();
            panel2.setBorder(BorderFactory.createTitledBorder("Car"));
            /* make the slider a little thinner than the call panels */
            Dimension dims2 = new Dimension(60, 210);
            panel2.setMinimumSize(dims2);
            panel2.setMaximumSize(dims2);
            panel2.setPreferredSize(dims2);
            panel2.setLayout(new GridLayout());
            panel2.add(posSlider);
            Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
            for (int i = 0; i < Elevator.numFloors; ++i) {
                labels.put(i * posSlider.getMajorTickSpacing(), new JLabel(Integer.toString(1 + i)));
            }
            posSlider.setLabelTable(labels);
            posSlider.setPaintLabels(true);
            cons = new GridBagConstraints();
            cons.gridy = 0;
            cons.gridx = 1;
            cons.fill = GridBagConstraints.VERTICAL;
            panel.add(panel2, cons);
        }

        backCallPanel = new CallPanel(Hallway.BACK);
        backCallPanel.setBorder(BorderFactory.createTitledBorder("Back"));
        backCallPanel.setMinimumSize(dims);
        backCallPanel.setMaximumSize(dims);
        backCallPanel.setPreferredSize(dims);
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 2;
        cons.fill = GridBagConstraints.VERTICAL;
        panel.add(backCallPanel, cons);

        speedSlider = new JSlider(-100, 100, 0);
        speedSlider.setFocusable(false);
        speedSlider.setOrientation(JSlider.VERTICAL);
        {
            Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
            labels.put(0, new JLabel("0"));
            speedSlider.setLabelTable(labels);
            speedSlider.setPaintLabels(true);
        }
        {
            JPanel speedPanel = new JPanel();
            speedPanel.setBorder(BorderFactory.createTitledBorder("Speed"));
            speedPanel.setLayout(new GridLayout());
            cons = new GridBagConstraints();
            Dimension dims2 = new Dimension(60, 210);
            speedPanel.setMinimumSize(dims2);
            speedPanel.setMaximumSize(dims2);
            speedPanel.setPreferredSize(dims2);
            speedPanel.add(speedSlider);
            cons = new GridBagConstraints();
            cons.gridy = 0;
            cons.gridx = 3;
            cons.fill = GridBagConstraints.VERTICAL;
            panel.add(speedPanel, cons);
        }

        JPanel doorPanel = new JPanel();
        doorPanel.setBorder(BorderFactory.createTitledBorder("Doors"));
        doorPanel.setLayout(new GridBagLayout());

        DoorPanel frontLeftDoorPanel = new DoorPanel(Hallway.FRONT, Side.LEFT);
        frontLeftDoorPanel.setBorder(BorderFactory.createTitledBorder("Front Left"));
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 0;
        doorPanel.add(frontLeftDoorPanel, cons);
        widgets.add(frontLeftDoorPanel);

        DoorPanel frontRightDoorPanel = new DoorPanel(Hallway.FRONT, Side.RIGHT);
        frontRightDoorPanel.setBorder(BorderFactory.createTitledBorder("Front Right"));
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 1;
        doorPanel.add(frontRightDoorPanel, cons);
        widgets.add(frontRightDoorPanel);

        DoorPanel backLeftDoorPanel = new DoorPanel(Hallway.BACK, Side.LEFT);
        backLeftDoorPanel.setBorder(BorderFactory.createTitledBorder("Back Left"));
        cons = new GridBagConstraints();
        cons.gridy = 1;
        cons.gridx = 0;
        doorPanel.add(backLeftDoorPanel, cons);
        widgets.add(backLeftDoorPanel);

        DoorPanel backRightDoorPanel = new DoorPanel(Hallway.BACK, Side.RIGHT);
        backRightDoorPanel.setBorder(BorderFactory.createTitledBorder("Back Right"));
        cons = new GridBagConstraints();
        cons.gridy = 1;
        cons.gridx = 1;
        doorPanel.add(backRightDoorPanel, cons);
        widgets.add(backRightDoorPanel);

        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 4;
        cons.fill = GridBagConstraints.BOTH;
        cons.anchor = GridBagConstraints.WEST;
        cons.weightx = 1.0;
        panel.add(doorPanel, cons);
        
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 0;
        cons.fill = GridBagConstraints.HORIZONTAL;
        widgetPanel.add(panel, cons);

        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        JLabel label;

        label = new JLabel("Position Indicator");
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 0;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(label, cons);

        indField = new JTextField(2);
        indField.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 1;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(indField, cons);

        label = new JLabel("Car Level Position");
        cons = new GridBagConstraints();
        cons.gridy = 1;
        cons.gridx = 0;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(label, cons);

        posField = new JTextField(8);
        posField.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 1;
        cons.gridx = 1;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(posField, cons);

        label = new JLabel("Speed");
        cons = new GridBagConstraints();
        cons.gridy = 2;
        cons.gridx = 0;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(label, cons);

        speedField = new JTextField(8);
        speedField.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 2;
        cons.gridx = 1;
        cons.weightx = 1.0;
        cons.anchor = GridBagConstraints.WEST;
        cons.fill = GridBagConstraints.HORIZONTAL;
        panel.add(speedField, cons);

        label = new JLabel("Car Weight");
        cons = new GridBagConstraints();
        cons.gridy = 3;
        cons.gridx = 0;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(label, cons);

        weightField = new JTextField(8);
        weightField.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 3;
        cons.gridx = 1;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(weightField, cons);

        label = new JLabel("Desired Floor");
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 2;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(label, cons);

        desDirTextField = new JTextField(10);
        desDirTextField.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 3;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(desDirTextField, cons);

        upLanternCheckBox = new JCheckBox("Up Lantern");
        upLanternCheckBox.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 1;
        cons.gridx = 3;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(upLanternCheckBox, cons);

        downLanternCheckBox = new JCheckBox("Down Lantern");
        downLanternCheckBox.setFocusable(false);
        cons.gridy = 2;
        cons.gridx = 3;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(downLanternCheckBox, cons);

        label = new JLabel("Simulator Time");
        cons = new GridBagConstraints();
        cons.gridy = 3;
        cons.gridx = 2;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(label, cons);

        timeField = new JTextField(15);
        timeField.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 3;
        cons.gridx = 3;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(timeField, cons);

        upLevelSensorCheckBox = new JCheckBox("Up Level Sensor");
        upLevelSensorCheckBox.setFocusable(false);
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 4;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(upLevelSensorCheckBox, cons);

        downLevelSensorCheckBox = new JCheckBox("Down Level Sensor");
        downLevelSensorCheckBox.setFocusable(false);
        cons.gridy = 1;
        cons.gridx = 4;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(downLevelSensorCheckBox, cons);

        carPassengerCount = new JLabel(CAR_PASSENGER_COUNT_STR + "0");
        cons.gridy = 2;
        cons.gridx = 4;
        cons.anchor = GridBagConstraints.WEST;
        panel.add(carPassengerCount, cons);



        cons = new GridBagConstraints();
        cons.gridy = 1;
        cons.gridx = 0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.anchor = GridBagConstraints.WEST;
        widgetPanel.add(panel, cons);

        //panel with realtime rate buttons and info

        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        label = new JLabel("Realtime Execution Rate");
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 0;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(label, cons);

        realtimeField = new JSpinner(
                new SpinnerNumberModel(Harness.getRealtimeRate(),
                        0.0, Double.POSITIVE_INFINITY, 0.1));
        realtimeField.setEditor(new JSpinner.NumberEditor(realtimeField,"0.0"));
        ((JSpinner.NumberEditor)realtimeField.getEditor()).getTextField().setColumns(3);
        realtimeField.setFocusable(true);
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 1;
        cons.ipadx = 5;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(realtimeField, cons);

        realtimeField.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                JSpinner source = (JSpinner) ev.getSource();
                Harness.setRealtimeRate((Double)source.getValue());
                if (Harness.simulationIsBlocked()) Harness.stepSimulation();
            }
        });
        
        JButton pauseButton = new JButton("Pause");
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 2;
        cons.insets = new Insets(5,5,5,5);
        cons.anchor = GridBagConstraints.EAST;
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                realtimeField.setValue(0.0);
            }
        });
        panel.add(pauseButton, cons);

        JButton oneXButton = new JButton("1x");
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 3;
        cons.insets = new Insets(5,5,5,5);
        cons.anchor = GridBagConstraints.EAST;
        oneXButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                realtimeField.setValue(1.0);
            }
        });
        panel.add(oneXButton, cons);

        JButton twoXButton = new JButton("2x");
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 4;
        cons.insets = new Insets(5,5,5,5);
        cons.anchor = GridBagConstraints.EAST;
        twoXButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                realtimeField.setValue(2.0);
            }
        });
        panel.add(twoXButton, cons);

        JButton maxRateButton = new JButton("Max");
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 5;
        cons.gridwidth = 1;
        cons.insets = new Insets(5,5,5,5);
        cons.anchor = GridBagConstraints.EAST;
        maxRateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                realtimeField.setValue(Double.POSITIVE_INFINITY);
            }
        });
        panel.add(maxRateButton, cons);

        // Breakpoint controls
        breakpointField = new JTextField(10);
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 6;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(breakpointField, cons);

        Harness.addBreakpointListener(new BreakpointPrinter(System.err));
        breakpointButton = new JButton("Set Breakpoint");
        breakpointButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // Get time delta to desired break time
                    double time = Double.parseDouble(breakpointField.getText());

                    // Clear existing gui breakpoint if there is one
                    if(breakpoint != null) {
                        Harness.removeBreakpoint(breakpoint);
                    }

                    // Set breakpoint
                    breakpoint = new SimTime(time, SimTimeUnit.SECOND);
                    Harness.addBreakpoint(breakpoint);

                    // Change field color to show that breakpoint has been set
                    breakpointField.setBackground(Color.GREEN);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Error parsing breakpoint time. Enter value in seconds");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(null, "Error setting breakpoint. Did you try to set a breakpoint for a time that has already passed?");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error setting breakpoint. Check your input and try again.");
                }
            }
        });
        cons = new GridBagConstraints();
        cons.gridy = 0;
        cons.gridx = 7;
        cons.anchor = GridBagConstraints.EAST;
        panel.add(breakpointButton, cons);

        cons = new GridBagConstraints();
        cons.gridy = 2;
        cons.gridx = 0;
        cons.weightx = 1.0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.anchor = GridBagConstraints.EAST;
        cons.fill = GridBagConstraints.HORIZONTAL;
        widgetPanel.add(panel, cons);

        getContentPane().add(widgetPanel);
        pack();

        posPayload = CarPositionPayload.getReadablePayload();
        speedPayload = DriveSpeedPayload.getReadablePayload();
        weightPayload = CarWeightPayload.getReadablePayload();
        indPayload = CarPositionIndicatorPayload.getReadablePayload();
        alarmPayload = CarWeightAlarmPayload.getReadablePayload();

        upLanternPayload = CarLanternPayload.getReadablePayload(Direction.UP);
        physicalConnection.registerTimeTriggered(upLanternPayload);
        downLanternPayload = CarLanternPayload.getReadablePayload(Direction.DOWN);
        physicalConnection.registerTimeTriggered(downLanternPayload);

        upLevelPayload = LevelingPayload.getReadablePayload(Direction.UP);
        physicalConnection.registerTimeTriggered(upLevelPayload);
        downLevelPayload = LevelingPayload.getReadablePayload(Direction.DOWN);
        physicalConnection.registerTimeTriggered(downLevelPayload);

        //desFloorPayload = DesiredFloorPayload.getReadablePayload();
        //canNetworkConnection.registerTimeTriggered(desFloorPayload);
        {
        ReadableCanMailbox m = CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        desFloorPayloadTranslator = new DesiredFloorCanPayloadTranslator(m);
        canNetworkConnection.registerTimeTriggered(m);
        }
        
        //all formerly canNetworkConnection
        physicalConnection.registerTimeTriggered(posPayload);
        physicalConnection.registerTimeTriggered(speedPayload);
        physicalConnection.registerTimeTriggered(weightPayload);
        physicalConnection.registerTimeTriggered(indPayload);
        physicalConnection.registerTimeTriggered(alarmPayload);

        Harness.addBreakpointListener(this);

        threadTimer.schedule(new RefreshTimer(), 50, 50);
    }

    private class RefreshTimer extends TimerTask {

        @Override
        public void run() {
            Harness.interleaveLock();
            try {
                updateControls();
            } finally {
                Harness.interleaveUnlock();
            }
        }
        
    }
    
    private void updateControls() {
        for (Widget w : widgets) {
            w.update();
        }

        timeField.setText(String.format("%.6f", Harness.getTime().getFracSeconds()));
        posSlider.setValue((int)(posPayload.position() * 1000));
        posField.setText(String.format("%.3f", posPayload.position()));
        switch (speedPayload.direction())
        {
          case UP:
            speedSlider.setValue((int)Math.round(
                  speedSlider.getMaximum() * 
                  (speedPayload.speed()/DriveObject.FastSpeed)));
            break;
          case DOWN:
            speedSlider.setValue((int)Math.round(
                  speedSlider.getMinimum() * 
                  (speedPayload.speed()/DriveObject.FastSpeed)));
            break;
          case STOP:
            speedSlider.setValue(0);
            break;
        }
        speedField.setText(speedPayload.direction() + " @ " +
                String.format("%.3f", speedPayload.speed()));
        indField.setText("" + indPayload.floor());
        weightField.setText("" + (weightPayload.weight() / 10.0));
        weightField.setBackground(alarmPayload.isRinging() ? Color.getHSBColor(0.0f, 0.5f, 1.0f) : Color.WHITE);
        upLanternCheckBox.setSelected(upLanternPayload.lighted());
        upLanternCheckBox.setBackground(upLanternPayload.lighted() ? UP_LANTERN_COLOR : OFF_LANTERN_COLOR);
        downLanternCheckBox.setSelected(downLanternPayload.lighted());
        downLanternCheckBox.setBackground(downLanternPayload.lighted() ? DOWN_LANTERN_COLOR : OFF_LANTERN_COLOR);

        upLevelSensorCheckBox.setSelected(upLevelPayload.getValue());
        upLevelSensorCheckBox.setBackground(upLevelPayload.getValue() ? LEVEL_ON_COLOR : LEVEL_OFF_COLOR);
        downLevelSensorCheckBox.setSelected(downLevelPayload.getValue());
        downLevelSensorCheckBox.setBackground(downLevelPayload.getValue() ? LEVEL_ON_COLOR : LEVEL_OFF_COLOR);

        //update passenger counts
        for (int i=0; i < Elevator.numFloors; i++) {
            int floor = i+1;
            if (Elevator.hasLanding(i+1, Hallway.FRONT)) {
                frontCallPanel.passengerCounts[i].setText(Integer.toString(passengerHandler.getHallPassengerCount(floor, Hallway.FRONT)));
                frontCallPanel.passengerCounts[i].setToolTipText(passengerHandler.getHallPassengerInfo(floor, Hallway.FRONT));
            }
            if (Elevator.hasLanding(i+1, Hallway.BACK)) {
                backCallPanel.passengerCounts[i].setText(Integer.toString(passengerHandler.getHallPassengerCount(floor, Hallway.BACK)));
                backCallPanel.passengerCounts[i].setToolTipText(passengerHandler.getHallPassengerInfo(floor, Hallway.BACK));
            }
        }
        carPassengerCount.setText(CAR_PASSENGER_COUNT_STR + passengerHandler.getCarPassengerCount());
        carPassengerCount.setToolTipText(passengerHandler.getCarPassengerInfo());

        desDirTextField.setText(desFloorPayloadTranslator.getFloor() + " " +
                desFloorPayloadTranslator.getHallway() + " " + desFloorPayloadTranslator.getDirection());
    }

    public void breakpointOccured(SimTime breakpointTime) {
        breakpointField.setBackground(Color.WHITE);
        realtimeField.setValue(0.0);
        updateControls();
    }

    private void log(Object... msg) {
        if (verbose) {
            Harness.log("SwingDisplay", msg);
        }
    }
}
