/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.payloads;

import simulator.payloads.translators.CanPayloadTranslator;
import java.util.BitSet;

/**
 * CanMailbox is the Payload object that is used by the elevator.framework.CANNetwork.
 *
 * It implements a bit-level representation of the CAN message, including the CAN message ID
 * and a binary payload of up to 8 bytes.
 *
 * This class also provides the utility methods necessary to compute bit-stuffing
 * over the actual binary representation of the message.
 *
 * @author Justin Ray
 */
public final class CanMailbox extends Payload {

    private boolean lastDropped;
    private final int messageId; //lower 29 bits represent the message ID
    private BitSet payload; //the data of the message
    private int payloadSize;  //byte size of the payload
    private CanPayloadTranslator translator = null;
    //cache the current data payload every time the size (with bitstuffing) is computed
    private BitSet lastSizedPayload = null;
    //cached message size value
    private final CanBitStream headerBitStream; //cached bit stream of the header bits, which don't change
    private int lastSize = -1;
    /**
     * limit the message ID to 29 bits;
     */
    private final static int MESSAGE_ID_MASK = 0x1FFFFFFF;

    public final static class ReadableCanMailbox extends ReadablePayload {

        private final CanMailbox mailbox;

        public ReadableCanMailbox(CanMailbox mailbox) {
            super(mailbox);
            this.mailbox = mailbox;
        }

        /**
         * 
         * @return the mailbox object for internal use by the network scheduler
         */
        CanMailbox asCanMailbox() {
            return mailbox;
        }

        /**
         * @return CAN message ID in the lower 29 bits of the int value
         */
        public int getMessageId() {
            return mailbox.getMessageId();
        }

        /**
         * 
         * @return A BitSet representing the payload - note that the size of 
         * payload (in bits) cannot be determined from the BitSet because it is
         * a sparse data structure.  
         * Compute it using getPayloadSize() instead.
         */
        public BitSet getMessagePayload() {
            return mailbox.getMessagePayload();
        }

        /**
         *
         * @return the size of the message payload in bytes
         */
        public int getPayloadSize() {
            return mailbox.getPayloadSize();
        }

        /**
         * 
         * @param translator Specify a translator to use with this mailbox.  
         * CanPayloadTranslator automatically calls this when the translator
         * is created with a reference to the mailbox.
         */
        public void setTranslator(CanPayloadTranslator translator) {
            mailbox.setTranslator(translator);
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public final static class WriteableCanMailbox extends WriteablePayload {

        private final CanMailbox mailbox;

        public WriteableCanMailbox(CanMailbox mailbox) {
            super(mailbox);
            this.mailbox = mailbox;
        }

        /**
         *
         * @return the mailbox object for internal use by the network scheduler
         */
        CanMailbox asCanMailbox() {
            return mailbox;
        }

        /**
         * @return CAN message ID in the lower 29 bits of the int value
         */
        public int getMessageId() {
            return mailbox.getMessageId();
        }

        /**
         *
         * @return A BitSet representing the payload - note that the size of
         * payload (in bits) cannot be determined from the BitSet because it is
         * a sparse data structure.
         * Compute it using getPayloadSize() instead.
         */
        public BitSet getMessagePayload() {
            return mailbox.getMessagePayload();
        }

        /**
         *
         * @return the size of the message payload in bytes
         */
        public int getPayloadSize() {
            return mailbox.getPayloadSize();
        }

        /**
         *
         * @param translator Specify a translator to use with this mailbox.
         * CanPayloadTranslator automatically calls this when the translator
         * is created with a reference to the mailbox.
         */
        public void setTranslator(CanPayloadTranslator translator) {
            mailbox.setTranslator(translator);
        }

        /**
         * Set the payload value and size.
         * @param payload BitSet with the payload value
         * @param payloadSize size in bytes.
         */
        public void setMessagePayload(BitSet payload, int payloadSize) {
            mailbox.setMessagePayload(payload, payloadSize);
        }
    }

    /**
     * @return a ReadableCanMailbox that can be used to read messages from the network
     */
    public final static ReadableCanMailbox getReadableCanMailbox(int messageID) {
        return new ReadableCanMailbox(new CanMailbox(messageID));
    }

    /**
     * @return a WriteableCanMailbox that can be used to write messages to the network
     */
    public final static WriteableCanMailbox getWriteableCanMailbox(int messageID) {
        return new WriteableCanMailbox(new CanMailbox(messageID));
    }

    /**
     * Copy constructor
     * @param p the CanMailbox to copy.
     */
    CanMailbox(CanMailbox p) {
        super(p);
        this.lastDropped = p.lastDropped;
        this.messageId = p.messageId;
        this.payload = (BitSet) p.payload.clone();
        this.payloadSize = p.payloadSize;
        this.headerBitStream = new CanBitStream(p.headerBitStream);
    }

    /**
     * Create a new CAN payload with the specified ID and payload size.  Since
     * CAN headers are 29 bits long, only the 29 least significant bits of the
     * message ID are used.
     * 
     * @param messageId
     *        the message ID
     * @param payloadSize
     *        the size of the payload in bytes
     */
    CanMailbox(int messageId) {
        super(messageId & MESSAGE_ID_MASK);
        if (messageId != (messageId & MESSAGE_ID_MASK)) {
            throw new IllegalArgumentException("invalid message ID: "
                    + Integer.toHexString(messageId));
        }
        if ((~messageId & 0x1FC00000) == 0) {
            throw new IllegalArgumentException("at least one of the 7 most significant bits of the message ID must be dominant: " + Integer.toHexString(messageId));
        }
        this.messageId = messageId;
        this.payload = new BitSet();
        this.payloadSize = 0;
        this.headerBitStream = buildHeaderBitStream(this.messageId);
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        CanMailbox copyFrom = (CanMailbox) p;
        if (this.messageId != copyFrom.messageId) {
            throw new RuntimeException("Cannot copy can message with id " + copyFrom.messageId + " to message with id " + messageId);
        }
        this.payloadSize = copyFrom.payloadSize;
        this.payload = (BitSet) copyFrom.payload.clone();
        payload.clear();
        payload.or(copyFrom.payload);
        this.lastDropped = copyFrom.lastDropped;
    }

    public int getMessageId() {
        return messageId;
    }

    /**
     * 
     * @return a defensive copy of the payload bitset
     */
    public BitSet getMessagePayload() {
        return (BitSet) payload.clone();
    }

    /**
     * Set the message values
     * @param newPayload the bitset containing the binary payload data
     * @param payloadSize the size (in bytes) of the payload - must be in the range [0,8]
     */
    public void setMessagePayload(BitSet newPayload, int payloadSize) {
        if (payloadSize < 0 || payloadSize > 8) {
            throw new RuntimeException("payloadSize invalid");
        }
        if (newPayload.length() > payloadSize * 8) {
            throw new RuntimeException("payload is larger than the specified size");
        }
        this.payloadSize = payloadSize;
        payload.clear();
        payload.or(newPayload);
    }

    /**
     * @return the current payload size in bytes.
     */
    public int getPayloadSize() {
        return payloadSize;
    }

    /**
     * 
     * @return the lastDropped parameter, which is used to prevent dropping two-in-a-row of the same message type.
     */
    public final boolean isLastDropped() {
        return lastDropped;
    }

    /**
     * call to set the lastDropped parameter
     * @param lastDropped
     */
    public final void setLastDropped(boolean lastDropped) {
        this.lastDropped = lastDropped;
    }

    /**
     * Override toString method prints ID and payload - if a CanPayloadTranslator is set,
     * it is used to generate a human-readable message.
     * @return
     */
    @Override
    public String toString() {
        if (translator == null) {
            return "ID=" + Integer.toHexString(messageId) + "; Payload=" + CanPayloadTranslator.bitSetToHex(payload);
        } else {
            return "ID=" + Integer.toHexString(messageId) + "; Payload=" + translator.payloadToString();
        }
    }

    /**
     * 
     * @return the current translator instantiated for this object
     */
    public CanPayloadTranslator getTranslator() {
        return translator;
    }

    /**
     * Set a translator and force the payload byte size to match.
     * @param translator
     */
    public void setTranslator(CanPayloadTranslator translator) {
        this.translator = translator;
        this.payloadSize = translator.getByteSize();
    }

    /**
     * Compute the size of the CAN message with headers and bitstuffing
     * @return
     */
    @Override
    public int getSize() {
        if (!payload.equals(lastSizedPayload)) {
            //copy the header bit stream and add the payload to it
            CanBitStream cb = new CanBitStream(headerBitStream);
            //payload has changed, so compute the new size
            cb.addBits(15 - payloadSize, 4);  //set length field
            //cb.insertMarker();
            for (int i = (payloadSize * 8) - 1; i >= 0; i--) {
                cb.addBit(payload.get(i));
                //if (i % 8 == 0) cb.insertMarker();
            }
            //add the crc
            cb.addBits(cb.getCRC(), 15);
            //cb.insertMarker();
            //this is the end of the bitstuffed part of the message
        /*if we were really encoding the full message, we would add these parts
            cb.addBit(false); //CRC deliminter
            cb.addBit(true); //ack slot
            cb.addBit(false); //ack delimiter
            cb.addBits(0,9); //6 bits for eof space + 3 bits for intermission*/
            //debug print the bitstream
            //System.out.println("SizeBitStream=" + cb.getBitString());

            //the total message length, not including stuff bits or payload
            //is 66
            lastSize = 66 + (8 * payloadSize) + cb.getStuffBitCount();
            lastSizedPayload = payload;
            //Harness.log("Message size for ",payloadSize," bytes is ", lastSize," bits.  Bitstuff = ", cb.getStuffBitCount());
        } else {
            // Harness.log("Using cached size");
        }
        //return the cached or computed size
        return lastSize;
    }

    private static CanBitStream buildHeaderBitStream(int messageId) {
        CanBitStream cb = new CanBitStream();

        cb.addBit(true); //start bit
        //cb.insertMarker();
        cb.addBits((messageId >> 18) & 0x7FF, 11);  //11 upper bits of
        //cb.insertMarker();
        cb.addBit(false);  //SRR = recessive
        cb.addBit(false);  //IDE bit to recessive
        //cb.insertMarker();
        cb.addBits(messageId & 0x3FFFF, 18);  //lower 18 bits of message id
        //cb.insertMarker();
        cb.addBit(true); //set RTR bit
        //cb.insertMarker();
        cb.addBit(true); //set 2 bits = reserved field
        cb.addBit(true);

        return cb;
    }

    /**
     * Utility class for computing the actual binary representation of a message
     */
    private static class CanBitStream {

        int crcChecksum = 0;
        final int crcPolynomial = 0x4599;
        //bitstuff state
        int stuffBitCount = 0;
        int sameBitCount = 0;
        boolean lastbit;
        boolean isFirstBit = true;

        public CanBitStream() {
            //do nothing;
        }

        public CanBitStream(CanBitStream copyFrom) {
            this.crcChecksum = copyFrom.crcChecksum;
            this.stuffBitCount = copyFrom.stuffBitCount;
            this.sameBitCount = copyFrom.sameBitCount;
            this.isFirstBit = copyFrom.isFirstBit;
            this.lastbit = copyFrom.lastbit;
        }

        public void clear() {
            //reset crc state
            crcChecksum = 0;

            //reset bitstuff state
            stuffBitCount = 0;
            isFirstBit = true;

            //string state
            //bitString.setLength(0);
        }

        public void addBits(int value, int bitCount) {
            for (int mask = 1 << (bitCount - 1); mask != 0; mask >>= 1) {
                addBit((mask & value) == mask);
            }
        }

        public void addBit(boolean bit) {
            //addToBitString(bit);
            addToCrc(bit);
            addToBitstuff(bit);
        }

        public int getCRC() {
            return crcChecksum;
        }

        public int getStuffBitCount() {
            return stuffBitCount;
        }

        private void addToCrc(boolean bit) {
            int crcNext = (crcChecksum >> 14) ^ (bit ? 1 : 0); //crcChecksum[14] XOR next_data_bit
            crcChecksum = (crcChecksum << 1) & 0x7FFE; //clear highest and lowest bits
            if (crcNext == 1) {
                crcChecksum ^= crcPolynomial;  //xor in the polynomial value
            }
        }

        private void addToBitstuff(boolean bit) {
            if (isFirstBit) {
                lastbit = bit;
                isFirstBit = false;
                sameBitCount = 1;
            } else {
                //not the first bit
                if (bit == lastbit) {
                    sameBitCount++;
                    if (sameBitCount == 5) {
                        stuffBitCount++;
                        //add stuff bit to bit string
                        //bitString.append(!lastbit ? '*' : '_');
                        lastbit = !lastbit;
                        sameBitCount = 1;
                    }
                } else {
                    lastbit = bit;
                    sameBitCount = 1;
                }
            }
        }
    }

    public static void main(String args[]) {
        //test the CanMailbox

        CanMailbox m = new CanMailbox(0x1F000000);

        BitSet b = m.getMessagePayload();
        b.set(0, true);
        m.setMessagePayload(b, 1);

        CanMailbox m2 = new CanMailbox(0x1F000000);
        m2.copyFrom(m);

        System.out.println(m);
        System.out.println(m2);

        /*
        CanMailbox cp = new CanMailbox(0x1F0000FF, 4);
        IntegerCanPayloadTranslator t1 = new IntegerCanPayloadTranslator(cp);
        int value = 0x0F0F0F0;
        System.out.println("SetValue=" + value);
        t1.setValue(value);
        System.out.println("GetValue=" + t1.getValue());
        System.out.println("CanPayload.tostring=" + cp);
        System.out.println("Message ID=" + Integer.toHexString(cp.getMessageId()));
        System.out.println("PayloadSize=" + cp.getPayloadSize());
        System.out.println("Payload=" + CanPayloadTranslator.bitSetToHex(cp.getMessagePayload()));

        System.out.println("MessageSize=" + cp.getSize());

        System.out.println("SecondMessageSizeTest=" + cp.getSize());

        System.out.println();
        System.out.println();
        System.out.println();

        value = 0x0A1A1A;
        System.out.println("SetValue=" + value);
        t1.setValue(value);
        System.out.println("GetValue=" + t1.getValue());
        System.out.println("CanPayload.tostring=" + cp);
        System.out.println("Message ID=" + cp.getMessageId());
        System.out.println("PayloadSize=" + cp.getPayloadSize());
        System.out.println("Payload=" + CanPayloadTranslator.bitSetToHex(cp.getMessagePayload()));

        System.out.println("MessageSize=" + cp.getSize());

        System.out.println("SecondMessageSizeTest=" + cp.getSize());


        System.out.println();
        System.out.println();
        System.out.println();


        System.out.println("Orphaned payload");
        cp = new CanMailbox(0x1F0000FF, 4);
        BitSet bs = new BitSet();
        for (int i=0; i < 32; i++) {
        bs.set(i, (i % 3 == 0));
        }
        System.out.println("BitSet=" + CanPayloadTranslator.bitSetToHex(bs));
        cp.setMessagePayload(bs);
        System.out.println("CanPayload.tostring (notrans)=" + cp);
        


        System.out.println();
        System.out.println();
        System.out.println();

        System.out.println("Boolean trans");
        cp = new CanMailbox(0x1F0000FF, 4);
        BooleanCanPayloadTranslator bt = new BooleanCanPayloadTranslator(cp);
        System.out.println("SetValue=" + true);
        bt.setValue(true);
        System.out.println("GetValue=" + bt.getValue());
        System.out.println("CanPayload.tostring=" + cp);
        System.out.println("Message ID=" + cp.getMessageId());
        System.out.println("PayloadSize=" + cp.getPayloadSize());
        System.out.println("Payload=" + CanPayloadTranslator.bitSetToHex(cp.getMessagePayload()));

        System.out.println("SetValue=" + false);
        bt.setValue(false);
        System.out.println("GetValue=" + bt.getValue());
        System.out.println("CanPayload.tostring=" + cp);
        System.out.println("Message ID=" + cp.getMessageId());
        System.out.println("PayloadSize=" + cp.getPayloadSize());
        System.out.println("Payload=" + CanPayloadTranslator.bitSetToHex(cp.getMessagePayload()));
         */

    }

    @Override
    public Payload clone() {
        //use the copy constructor
        return new CanMailbox(this);
    }
}
