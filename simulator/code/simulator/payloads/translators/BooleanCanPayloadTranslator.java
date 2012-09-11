/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.payloads.translators;

import java.util.BitSet;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 * Translates a single boolean value into a 4-byte payload.
 * 
 * @author Justin Ray
 */
public class BooleanCanPayloadTranslator extends CanPayloadTranslator {

    
    /**
     * Constructor for use with WriteableCanMailbox objects
     * @param payload
     */
    public BooleanCanPayloadTranslator(WriteableCanMailbox payload) {
        super(payload, 4);
    }

    /**
     * Constructor for use with ReadableCanMailbox objects
     * @param payload
     */

    public BooleanCanPayloadTranslator(ReadableCanMailbox payload) {
        super(payload, 4);
    }
    
    //required for reflection
    public void set(boolean value) {
        setValue(value);
    }

    
    public void setValue(boolean value) {       
        BitSet b = new BitSet(32);
        b.set(31, value);
        setMessagePayload(b, getByteSize());
    }
    
    public boolean getValue() {
        return getMessagePayload().get(31);
    }
    
    @Override
    public String payloadToString() {
        return Boolean.toString(getValue());
    }
}
