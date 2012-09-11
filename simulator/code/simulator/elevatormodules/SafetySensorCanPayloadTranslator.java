package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class SafetySensorCanPayloadTranslator extends BooleanCanTranslator {

    /**
     * CAN payload translator for emergency brake notification network messages
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public SafetySensorCanPayloadTranslator(WriteableCanMailbox p) {
        super(p, MessageDictionary.EMERGENCY_BRAKE_CAN_ID, "SafetySensor");
    }

    /**
     * CAN payload translator for emergency brake notification network messages
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public SafetySensorCanPayloadTranslator(ReadableCanMailbox p) {
        super(p, MessageDictionary.EMERGENCY_BRAKE_CAN_ID, "SafetySensor");
    }
}
