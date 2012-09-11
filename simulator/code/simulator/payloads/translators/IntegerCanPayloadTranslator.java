/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.payloads.translators;

import java.util.BitSet;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 * This class takes a single integer or boolean value and translates it to a
 * 4 byte CanMailbox
 * @author justinr2
 */
public class IntegerCanPayloadTranslator extends CanPayloadTranslator {

    /**
     * Constructor for use with WriteableCanMailbox objects
     * @param payload
     */
    public IntegerCanPayloadTranslator(WriteableCanMailbox payload) {
        super(payload, 4);
    }

    /**
     * Constructor for use with ReadableCanMailbox objects
     * @param payload
     */

    public IntegerCanPayloadTranslator(ReadableCanMailbox payload) {
        super(payload, 4);
    }

    
    //required for reflection
    public void set(int value) {
        setValue(value);
    }
    
    public void setValue(int value) {
        BitSet b = new BitSet();
        addIntToBitset(b, value, 0, 32);
        setMessagePayload(b, getByteSize());
    }
    
    public int getValue() {
        return getIntFromBitset(getMessagePayload(), 0, 32);
    }
    
    @Override
    public String payloadToString() {
        return "0x" + Integer.toString(getValue(),16);
    }
}
