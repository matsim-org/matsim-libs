package playground.sebhoerl.avtaxi.config;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

public class AVTimingParameters extends ReflectiveConfigGroup {
    final static String TIMING = "timing";

    final static String PICKUP_DURATION_PER_STOP = "pickupDurationPerStop";
    final static String PICKUP_DURATION_PER_PASSENGER = "pickupDurationPerPassenger";
    final static String DROPOFF_DURATION_PER_STOP = "dropoffDurationPerStop";
    final static String DROPOFF_DURATION_PER_PASSENGER = "dropoffDurationPerPassenger";

    final private AVTimingParameters parent;

    protected Double pickupDurationPerStop = null;
    protected Double pickupDurationPerPassenger = null;

    protected Double dropoffDurationPerStop = null;
    protected Double dropoffDurationPerPassenger = null;

    public AVTimingParameters(AVTimingParameters parent) {
        super(TIMING);
        this.parent = parent;
    }

    public AVTimingParameters() {
        this(null);
    }

    @StringGetter(PICKUP_DURATION_PER_STOP)
    public double getPickupDurationPerStop() {
        if (pickupDurationPerStop == null && parent != null) {
            return parent.getPickupDurationPerStop();
        }

        return pickupDurationPerStop;
    }

    @StringSetter(PICKUP_DURATION_PER_STOP)
    public void setPickupDurationPerStop(double pickupDurationPerStop) {
        this.pickupDurationPerStop = pickupDurationPerStop;
    }

    @StringGetter(PICKUP_DURATION_PER_PASSENGER)
    public double getPickupDurationPerPassenger() {
        if (pickupDurationPerPassenger == null && parent != null) {
            return parent.getPickupDurationPerPassenger();
        }

        return pickupDurationPerPassenger;
    }

    @StringSetter(PICKUP_DURATION_PER_PASSENGER)
    public void setPickupDurationPerPassenger(double pickupDurationPerPassenger) {
        this.pickupDurationPerPassenger = pickupDurationPerPassenger;
    }

    @StringGetter(DROPOFF_DURATION_PER_STOP)
    public double getDropoffDurationPerStop() {
        if (dropoffDurationPerStop == null && parent != null) {
            return parent.getDropoffDurationPerStop();
        }

        return dropoffDurationPerStop;
    }

    @StringSetter(DROPOFF_DURATION_PER_STOP)
    public void setDropoffDurationPerStop(double dropoffDurationPerStop) {
        this.dropoffDurationPerStop = dropoffDurationPerStop;
    }

    @StringGetter(DROPOFF_DURATION_PER_PASSENGER)
    public double getDropoffDurationPerPassenger() {
        if (dropoffDurationPerPassenger == null && parent != null) {
            return parent.getDropoffDurationPerPassenger();
        }

        return dropoffDurationPerPassenger;
    }

    @StringSetter(DROPOFF_DURATION_PER_PASSENGER)
    public void setDropoffDurationPerPassenger(double dropoffDurationPerPassenger) {
        this.dropoffDurationPerPassenger = dropoffDurationPerPassenger;
    }

    public boolean hasPickupDurationPerStop() {
        return pickupDurationPerStop != null;
    }

    public boolean hasPickupDurationPerPassenger() {
        return pickupDurationPerPassenger != null;
    }

    public boolean hasDropoffDurationPerStop() {
        return dropoffDurationPerStop != null;
    }

    public boolean hasDropoffDurationPerPassenger() {
        return dropoffDurationPerPassenger != null;
    }

    public static AVTimingParameters createDefault() {
        AVTimingParameters timing = new AVTimingParameters();
        timing.setPickupDurationPerStop(120.0);
        timing.setPickupDurationPerPassenger(0.0);
        timing.setDropoffDurationPerStop(60.0);
        timing.setDropoffDurationPerPassenger(0.0);
        return timing;
    }
}
