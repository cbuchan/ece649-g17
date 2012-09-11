package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 * @author Justin Ray
 */
public class CarWeightCanPayloadTranslator extends VarIntegerCanTranslator {

    /**
     * CAN payload translator for car weight network message
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public CarWeightCanPayloadTranslator(WriteableCanMailbox p) {
        super(p, MessageDictionary.CAR_WEIGHT_CAN_ID, "CarWeight", 2);
    }

    /**
     * CAN payload translator for car weight network message
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public CarWeightCanPayloadTranslator(ReadableCanMailbox p) {
        super(p, MessageDictionary.CAR_WEIGHT_CAN_ID, "CarWeight", 2);
    }


    public int getWeight() {
        return getValue();
    }

    public void setWeight(int weight) {
        setValue(weight);
    }
}
