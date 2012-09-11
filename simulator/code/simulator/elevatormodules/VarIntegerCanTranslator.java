package simulator.elevatormodules;

import java.util.BitSet;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.translators.CanPayloadTranslator;

/**
 * Utility superclass for CAN payload translators with an integer value.
 * This class consolidates most of the functionality required for an integer 
 * translator so that most of the actual translators used by system objects
 * can be specified by a set of constructor parameters in the VarIntegerCanTranslator constructor.
 * 
 * This class is intentionally not available to classes in simulator.elevatorcontrol.
 * You are welcome to implement a similar generic class if you want to.
 *
 * Note that this translator and the ones descended from it are not compatible
 * with simulator.payloads.IntegerCanPayloadTranslator.  The same translator used
 * to encode a value should also be used to read it.
 *
 * @author Justin Ray
 */
public class VarIntegerCanTranslator extends CanPayloadTranslator {

    String name;

    VarIntegerCanTranslator(WriteableCanMailbox p, int expectedMask, String name, int byteSize) {
        super(p, byteSize, expectedMask);
        this.name = name;
    }

    VarIntegerCanTranslator(ReadableCanMailbox p, int expectedMask, String name, int byteSize) {
        super(p, byteSize, expectedMask);
        this.name = name;
    }

    
    //required for reflection
    public void set(int value) {
        setValue(value);
    }


    public int getValue() {
        return getIntFromBitset(getMessagePayload(), 0, getByteSize() * 8);
    }

    public void setValue(int position) {
        BitSet b = getMessagePayload();
        addIntToBitset(b, position, 0, getByteSize() * 8);
        setMessagePayload(b, getByteSize());
    }

    @Override
    public String payloadToString() {
        return name + " = " + getValue();
    }
}
