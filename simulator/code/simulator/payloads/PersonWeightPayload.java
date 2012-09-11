package simulator.payloads;

/**
 * Indicates that a Passenger of a particular mass has entered or exited the
 * car.
 * 
 * @author Charles Shelton
 */
public class PersonWeightPayload extends PhysicalPayload {

    private int weight;

    public static final class ReadablePersonWeightPayload extends PhysicalReadablePayload {

        private final PersonWeightPayload payload;

        private ReadablePersonWeightPayload(PersonWeightPayload payload) {
            super(payload);
            this.payload = payload;
        }

        public int weight() {
            return payload.weight;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }

    }

    public static final class WriteablePersonWeightPayload extends PhysicalWriteablePayload {

        private final PersonWeightPayload payload;

        private WriteablePersonWeightPayload(PersonWeightPayload payload) {
            super(payload);
            this.payload = payload;
        }

        public int weight() {
            return payload.weight;
        }

        public void set(int weight) {
            payload.set(weight);
        }

    }

    public static final WriteablePersonWeightPayload getWriteablePayload(int weight) {
        return new WriteablePersonWeightPayload(new PersonWeightPayload(weight));
    }

    public static final ReadablePersonWeightPayload getReadablePayload(int weight) {
        return new ReadablePersonWeightPayload(new PersonWeightPayload(weight));
    }


    PersonWeightPayload(int weight) {
        super(PhysicalPayload.PersonWeightEvent);
        this.weight = weight;
        setName("PersonWeightPayload");
    }

    public PersonWeightPayload(PersonWeightPayload source) {
        this(source.weight);
    }
    
    public PersonWeightPayload set(int value) {
        this.weight = value;
        return this;
    }

    @Override
    public void copyFrom(Payload src) {
        super.copyFrom(src);
        weight = ((PersonWeightPayload) src).weight;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + weight + ")";
    }

    @Override
    public Payload clone() {
        return new PersonWeightPayload(this);
    }
}
