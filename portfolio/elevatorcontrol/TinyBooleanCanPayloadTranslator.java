/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) - Author
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.translators.CanPayloadTranslator;

import java.util.BitSet;

/**
 * Translates a single boolean value into a 1-byte payload.
 * 
 * @author Rajeev Sharma
 */
public class TinyBooleanCanPayloadTranslator extends CanPayloadTranslator {

    
    /**
     * Constructor for use with WriteableCanMailbox objects
     * @param payload
     */
    public TinyBooleanCanPayloadTranslator(WriteableCanMailbox payload) {
        super(payload, 1);
    }

    /**
     * Constructor for use with ReadableCanMailbox objects
     * @param payload
     */

    public TinyBooleanCanPayloadTranslator(ReadableCanMailbox payload) {
        super(payload, 1);
    }
    
    //required for reflection
    public void set(boolean value) {
        setValue(value);
    }

    
    public void setValue(boolean value) {       
        BitSet b = new BitSet(8);
        b.set(0, value);
        setMessagePayload(b, getByteSize());
    }
    
    public boolean getValue() {
        return getMessagePayload().get(0);
    }
    
    @Override
    public String payloadToString() {
        return Boolean.toString(getValue());
    }
}
