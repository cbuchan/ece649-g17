/*
 */
package simulator.payloads.translators;

import java.util.BitSet;
import java.util.Random;
import simulator.elevatorcontrol.DesiredFloorCanPayloadTranslator;
import simulator.elevatorcontrol.DriveCommandCanPayloadTranslator;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 * Utility superclass for CanPayloadTranslators.  Descendants of this class
 * know how to encode and decode a specific message type from the binary
 * representation of data used by the CanMailbox object.
 * 
 * This class provides constructors for both WriteableCanMailbox objects and
 * ReadableCanMailbox objects so that the same translator can be used with
 * both types of objects.  Any translator descended from this class should
 * implement both constructors.
 * 
 * @see BooleanCanPayloadTranslator
 * @see IntegerCanPayloadTranslator
 * @see DesiredFloorCanPayloadTranslator
 * @see DriveCommandCanPayloadTranslator
 *
 * @author Justin Ray
 */
public abstract class CanPayloadTranslator
{

    private final WriteableCanMailbox wmailbox;  //The bit-level representation of the message value.
    private final ReadableCanMailbox rmailbox;  //The bit-level representation of the message value.
    private final int byteSize;  ///The expected byte size of the can payload

    /**
     * Construct a CanPayloadTranslator using the data from the payload object.
     * This constructor does not perform a CAN message ID check, and should only
     * be used for generic contructors (like BooleanCanPayloadTranslator) which
     * can be used for payloads with different IDs.
     *
     * @param payload the WriteableCanMailbox containing the bit-level network data
     * that is read or written by the translator.
     * @param byteSize the byte size of the data that is expected in the payload.
     * This value is read by the payload object, and an error is generated if you
     * try to set bits on the payload that exceed the size specified by byteSize.
     */
    public CanPayloadTranslator(WriteableCanMailbox payload, int byteSize) {
        this(payload, byteSize, 0);
    }
    
    /**
     * Construct a CanPayloadTranslator using the data from the payload object.
     *
     * In general, it is safer to use this consructor for CAN translators that
     * deal with specific message values.
     * 
     * @param payload the WriteableCanMailbox containing the bit-level network data
     * that is read or written by the translator.
     * @param byteSize the byte size of the data that is expected in the payload.
     * This value is read by the payload object, and an error is generated if you
     * try to set bits on the payload that exceed the size specified by byteSize.
     * @param expectedId The expected CAN message ID.  If this doesn't match the
     * value of the ID in the payload object, then an IllegalArgumentException 
     * is thrown.  This check tries to prevent you from registering the wrong
     * payload object with a translator object.  A value of 0 will cause the 
     * translator to skip the check.
     */
    public CanPayloadTranslator(WriteableCanMailbox payload, int byteSize, int expectedId) {
        if (payload == null) throw new NullPointerException("payload");
        this.wmailbox = payload;
        this.rmailbox = null;
        this.byteSize = byteSize;
        if (expectedId != 0) {
            if (payload.getMessageId() != expectedId) {
                throw new IllegalArgumentException(
                    "expected message ID 0x" + Integer.toHexString(expectedId) + 
                    ", received 0x"+Integer.toHexString(payload.getMessageId()));
            }            
        }
        payload.setTranslator(this);
    }
    
    /**
     * Constructor behaves in the same way as the WriteableCanMailbox methods.  See those constructors for details.
     */
    public CanPayloadTranslator(ReadableCanMailbox payload, int byteSize) {
        this(payload, byteSize, 0);
    }
    
    /**
     * Constructor behaves in the same way as the WriteableCanMailbox methods.  See those constructors for details.
     */
    public CanPayloadTranslator(ReadableCanMailbox payload, int byteSize, int expectedId) {
        if (payload == null) throw new NullPointerException("payload");
        this.rmailbox = payload;
        this.wmailbox = null;
        this.byteSize = byteSize;
        if (expectedId != 0) {
            if (payload.getMessageId() != expectedId) {
                throw new IllegalArgumentException(
                    "expected message ID 0x" + Integer.toHexString(expectedId) + 
                    ", received 0x"+Integer.toHexString(payload.getMessageId()));
            }            
        }
        payload.setTranslator(this);
    }

    public boolean hasReadablePayload() {
        return rmailbox != null;
    }

    public boolean hasWriteablePayload() {
        return wmailbox != null;
    }


    /**
     *
     * @return The writeable can payload message object, or null if translator was created with a readable can mailbox
     */
    public final WriteableCanMailbox getWriteablePayload() {
        if (wmailbox == null) throw new IllegalStateException("Can't call getWriteablePayload when created with a ReadableCanMailbox");
        return wmailbox;
    }

    /**
     *
     * @return The readable can payload message object, or null if translator was created with a writeable can mailbox
     */
    public final ReadableCanMailbox getReadablePayload() {
        if (rmailbox == null) throw new IllegalStateException("Can't call getReadablePayload when created with a WriteableCanMailbox");
        return rmailbox;
    }

    /**
     * 
     * @return the current binary message value
     */
    protected final BitSet getMessagePayload() {
        if (wmailbox != null) return wmailbox.getMessagePayload();
        else if (rmailbox != null) return rmailbox.getMessagePayload();
        else throw new IllegalStateException("One mailbox must be initialized");
    }

    /**
     * 
     * @param newValue the new binary representation to put in the message
     * @param payloadSize the size of the message
     * @throws IllegalStateException if this method is called when the translator
     * is initialized with a ReadableCanMailbox
     */
    protected final void setMessagePayload(BitSet newValue, int payloadSize) {
        if (wmailbox != null) wmailbox.setMessagePayload(newValue, payloadSize);
        else if (rmailbox != null) throw new IllegalStateException("Cannot call setBitSet when created with a ReadableCanMailbox");
        else throw new IllegalStateException("One mailbox must be initialized");
    }

    
    /**
     * the expected byte size of the message
     * @return
     */
    public final int getByteSize() {
        return byteSize;
    }

    /**
     * implement this method to generate a human-readable string representation
     * of the values in the message.
     * 
     * @return String representation of CAN mailbox
     */
    public abstract String payloadToString();

    @Override
    public String toString() {
        if (wmailbox != null) return wmailbox.toString();
        else if (rmailbox != null) return rmailbox.toString();
        else throw new IllegalStateException("One mailbox must be initialized");
        
    }
    
    

    /**
     * Utility method to add an integer value to a bit set. This method
     * modifies the bits from <code>startLocation</code> to <code>startLocation
     * + bitSize</code> by setting them according to the given integer value.
     * By calling this method several times with different startLocations,
     * multiple values can be stored in a single bitset.
     * 
     * @param b
     *        BitSet to modify.
     * @param value
     *        integer value to set. Negative values will be preserved
     * @param startLocation
     *        the index in the bit set that corresponds to the least significant bit of the value.
     *        This value is zero-indexed.
     * @param bitSize
     *        the number of bits used to represent the integer. Values larger
     *        than 32 will generate an error.
     */
    public static void addIntToBitset(BitSet b, int value, int startLocation,
        int bitSize)
    {
        if (bitSize > 32)
        {
            throw new IllegalArgumentException("bitSize too large");
        }
        if (bitSize <= 0)
        {
            throw new IllegalArgumentException("bitSize must be positive");
        }
        if (bitSize < 32)
        {
            // check min/max
            int max = (int) Math.pow(2.0, bitSize - 1) - 1;
            int min = -(int) Math.pow(2.0, bitSize - 1);
            if (value > max)
            {
                throw new IllegalArgumentException("Value " + value
                        + " is too large place into " + bitSize + " bits.");
            }
            if (value < min)
            {
                throw new IllegalArgumentException("Value " + value
                        + " is too small to place into " + bitSize + " bits.");
            }
        }
        int mask = 0x1;
        int bitOffset = startLocation;
        for (int i = 0; i < bitSize; i++)
        {
            b.set(bitOffset, (value & mask) == mask);
            mask = mask << 1;
            bitOffset++;
        }
    }

    
    /**
     * Recovers an integer value from the specified bit range. This method is
     * designed to be used in conjunction with addIntToBitset.
     * 
     * @param b
     *        The BitSet to read
     * @param startLocation
     *        The location of the lsb of the value. This value is zero-indexed.
     * @param bitSize
     *        The number of bits to read.
     * @return The recovered (positive) integer value.
     */
    public static int getIntFromBitset(BitSet b, int startLocation, int bitSize)
    {
        if (bitSize > 32)
        {
            throw new RuntimeException("bitSize too large");
        }
        if (bitSize <= 0)
        {
            throw new RuntimeException("bitSize must be positive");
        }
        int value = 0;
        int mask = 0x1;
        int bitOffset = startLocation;
        for (int i = 0; i < bitSize; i++)
        {
            if (b.get(bitOffset))
            {
                value = value | mask;
            }
            mask = mask << 1;
            bitOffset++;
        }
        if (bitSize < 32 && b.get(bitOffset - 1))
        {
            // sign extend the result is the top bit was set
            for (int i = bitSize; i < 32; i++)
            {
                value = value | mask;
                mask = mask << 1;
            }
        }
        return value;
    }

    
    /**
     * Utility method to add an integer value to a bit set. This method
     * modifies the bits from <code>startLocation</code> to <code>startLocation
     * + bitSize</code> by setting them according to the given integer value.
     * By calling this method several times with different startLocations,
     * multiple values can be stored in a single bitset.
     * 
     * @param b
     *        BitSet to modify.
     * @param value
     *        integer value to set. negative values are not allowed
     * @param startLocation
     *        the index in the bit set that corresponds to the least significant bit of the value.
     *        This value is zero-indexed.
     * @param bitSize
     *        the number of bits used to represent the integer. Values larger
     *        than 32 will generate an error.
     */
    public static void addUnsignedIntToBitset(BitSet b, int value, int startLocation,
        int bitSize)
    {
        if (bitSize > 32)
        {
            throw new IllegalArgumentException("bitSize too large");
        }
        if (bitSize <= 0)
        {
            throw new IllegalArgumentException("bitSize must be positive");
        }
        if (bitSize < 32)
        {
            // check min/max
            int max = (int) Math.pow(2.0, bitSize) - 1;
            if (value > max)
            {
                throw new IllegalArgumentException("Value " + value
                        + " is too large place into " + bitSize + " bits.");
            }
            if (value < 0)
            {
                throw new IllegalArgumentException("Value " + value
                        + " cannot be negative");
            }
        }
        int mask = 0x1;
        int bitOffset = startLocation;
        for (int i = 0; i < bitSize; i++)
        {
            b.set(bitOffset, (value & mask) == mask);
            mask = mask << 1;
            bitOffset++;
        }
    }

    /**
     * Recovers an integer value from the specified bit range. This method is
     * designed to be used in conjunction with addIntToBitset.
     * 
     * @param b
     *        The BitSet to read
     * @param startLocation
     *        The location of the lsb of the value. This value is zero-indexed.
     * @param bitSize
     *        The number of bits to read.
     * @return The recovered (positive) integer value.
     */
    public static int getUnsignedIntFromBitset(BitSet b, int startLocation, int bitSize)
    {
        if (bitSize > 32)
        {
            throw new RuntimeException("bitSize too large");
        }
        if (bitSize <= 0)
        {
            throw new RuntimeException("bitSize must be positive");
        }
        int value = 0;
        int mask = 0x1;
        int bitOffset = startLocation;
        for (int i = 0; i < bitSize; i++)
        {
            if (b.get(bitOffset))
            {
                value = value | mask;
            }
            mask = mask << 1;
            bitOffset++;
        }
        if (value < 0) {
            throw new RuntimeException("Something unexpected happened because the unsigned value came out negative.");
        }
        return value;
    }


    /**
     * Utility method to convert bitset to a hex string
     * 
     * @param b
     * @return
     */
    public static String bitSetToHex(BitSet b)
    {
        int val = 0;
        if (b.length() == 0)
        {
            return ("0x0");
        }
        // compute a length that is the rounded-up number of nibbles times 4
        int nibbleLen = (int) Math.ceil(b.length() / 4.0) * 4;

        StringBuilder buf = new StringBuilder("0x");
        int mask = 0x8;
        for (int i = nibbleLen - 1; i >= 0; i--)
        {
            if (b.get(i))
            {
                val |= mask;
            }
            mask >>= 1;
            if (mask == 0)
            {
                buf.append(Character.forDigit(val, 16));
                val = 0;
                mask = 0x8;
            }
        }
        return buf.toString();
    }

    /**
     * Utility method to convert bitset to a binary string
     * 
     * @param b
     * @return
     */
    public static String bitSetToBinary(BitSet b)
    {
        return bitSetToBinary(b, 0);
    }

    /**
     * Returns a binary string representation of the <code>BitSet</code>. The
     * low order bits will appear in the higher ("rightmost") indices of the
     * returned string.  The return value will be padded on the left with
     * <code>'0'</code> characters to make it at least <code>minLength</code>
     * characters long, if necessary.
     * 
     * @param b
     * @param minLength
     *        the minimum number of characters in the return value.
     * @return
     */
    public static String bitSetToBinary(BitSet b, int minLength)
    {
        if (b.length() == 0)
        {
            return "b0";
        }
        StringBuilder binStr = new StringBuilder("b");
        // pad to minLength with leading zeros
        if (b.length() < minLength)
        {
            for (int i = 0; i < (minLength - b.length()); i++)
            {
                binStr.append('0');
            }
        }
        for (int i = b.length() - 1; i >= 0; i--)
        {
            binStr.append(b.get(i) ? '1' : '0');
        }
        return binStr.toString();
    }

    public static void main(String[] args)
    {

        int errorCount = 0;
        int runCount = 0;
        int missCount = 0;
        Random r = new Random();
        for (int i = 0; i < 10000; i++)
        {
            runCount++;
            int value1 = r.nextInt();
            int value2 = r.nextInt();
            BitSet b = new BitSet();
            try
            {
                addIntToBitset(b, value1, 0, 30);
                addIntToBitset(b, value2, 30, 30);
            }
            catch (RuntimeException ex)
            {
                System.out.println(ex);
                missCount++;
                continue;
            }
            int v1Out = getIntFromBitset(b, 0, 30);
            int v2Out = getIntFromBitset(b, 30, 30);
            String line = "";
            line += "Value 1:  In = " + value1 + "; nOut = " + v1Out + "; ";
            line += "Value 2:  In = " + value2 + "; nOut = " + v2Out + "; ";
            if (value1 != v1Out || value2 != v2Out)
            {
                System.out.println("****" + line);
                errorCount++;
            }
            else
            {
                System.out.println(line);
            }

        }
        System.out.println("ErrorCount = " + errorCount + " RunCount = " + runCount + " MissCount = " + missCount);
        
        /*BitSet b = new BitSet();
        int[] testSet = {
        0,
        1,
        50,
        127
        };
        int[] testSet2 = {
        0,
        127
        };
        try {
        addIntToBitset(b, -4242, 0, 32);
        } catch (RuntimeException ex) {
        System.out.println("Exception: " + ex.getMessage());
        } finally {
        System.out.println();
        System.out.println();
        }
        for (int i : testSet) {
        for (int j : testSet2) {
        //for (int i = 0; i < 10; i++) {
        System.out.println("Value:  " + i);
        System.out.println("Value2:  " + j);
        b.clear();
        addIntToBitset(b, i, 0, 8);
        addIntToBitset(b, j, 8, 8);
        System.out.println("Size: " + b.size());
        System.out.println("Length: " + b.length());
        System.out.println("Binary Bitset:" + bitSetToBinary(b));
        System.out.println("Hex Bitset:" + bitSetToHex(b));
        System.out.println("Extracted Value:  " + getIntFromBitset(b, 0, 8));
        System.out.println("Extracted Value2:  " + getIntFromBitset(b, 8, 8));
        System.out.println();
        System.out.println();
        }
        }
         */

        /*BitSet b = new BitSet();
        int[] testSet = {
            0,
            1,
            2,
            10,
            500,
            10000,
            Integer.MAX_VALUE
        };
        for (int i : testSet) {
            //for (int i = 0; i < 10; i++) {
            System.out.println("Value:  " + i);
            System.out.println("HexValue:  " + Integer.toString(i, 16));
            b.clear();
            addIntToBitset(b, i, 0, 32);
            System.out.println("Size: " + b.size());
            System.out.println("Length: " + b.length());
            System.out.println("Binary Bitset:" + bitSetToBinary(b));
            System.out.println("Hex Bitset:" + bitSetToHex(b));
            System.out.println("Extracted Value:  " + getIntFromBitset(b, 0, 32));
            System.out.println();
            System.out.println();
        }*/
    }
}
