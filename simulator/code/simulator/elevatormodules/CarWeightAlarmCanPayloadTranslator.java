package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class CarWeightAlarmCanPayloadTranslator extends BooleanCanTranslator {

    /**
     * CAN translator for Car weight alarm message
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public CarWeightAlarmCanPayloadTranslator(WriteableCanMailbox p) {
        super(p, MessageDictionary.CAR_WEIGHT_ALARM_CAN_ID, "CarWeightAlaram");
    }

    /**
     * CAN translator for Car weight alarm message
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public CarWeightAlarmCanPayloadTranslator(ReadableCanMailbox p) {
        super(p, MessageDictionary.CAR_WEIGHT_ALARM_CAN_ID, "CarWeightAlaram");
    }
}
