package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class CarLevelPositionCanPayloadTranslator extends VarIntegerCanTranslator {

    /**
     * CAN payload translator for Car level position message
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public CarLevelPositionCanPayloadTranslator(WriteableCanMailbox p) {
        super(p, MessageDictionary.CAR_LEVEL_POSITION_CAN_ID, "CarLevelPosition", 4);
    }

    /**
     * CAN payload translator for Car level position message
     * @param p  CAN payload object whose message is interpreted by this translator
     */
    public CarLevelPositionCanPayloadTranslator(ReadableCanMailbox p) {
        super(p, MessageDictionary.CAR_LEVEL_POSITION_CAN_ID, "CarLevelPosition", 4);
    }


    public int getPosition() {
        return getValue();
    }

    public void setPosition(int position) {
        setValue(position);
    }
}
