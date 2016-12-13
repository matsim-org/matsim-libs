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

    protected Double pickupDurationPerStop = null;
    protected Double pickupDurationPerPassenger = null;

    protected Double dropoffDurationPerStop = null;
    protected Double dropoffDurationPerPassenger = null;

    public AVTimingParameters() {
        super(TIMING);
    }

    @StringGetter(PICKUP_DURATION_PER_STOP)
    public double getPickupDurationPerStop() {
        return pickupDurationPerStop;
    }

    @StringSetter(PICKUP_DURATION_PER_STOP)
    public void setPickupDurationPerStop(double pickupDurationPerStop) {
        this.pickupDurationPerStop = pickupDurationPerStop;
    }

    @StringGetter(PICKUP_DURATION_PER_PASSENGER)
    public double getPickupDurationPerPassenger() {
        return pickupDurationPerPassenger;
    }

    @StringSetter(PICKUP_DURATION_PER_PASSENGER)
    public void setPickupDurationPerPassenger(double pickupDurationPerPassenger) {
        this.pickupDurationPerPassenger = pickupDurationPerPassenger;
    }

    @StringGetter(DROPOFF_DURATION_PER_STOP)
    public double getDropoffDurationPerStop() {
        return dropoffDurationPerStop;
    }

    @StringSetter(DROPOFF_DURATION_PER_STOP)
    public void setDropoffDurationPerStop(double dropoffDurationPerStop) {
        this.dropoffDurationPerStop = dropoffDurationPerStop;
    }

    @StringGetter(DROPOFF_DURATION_PER_PASSENGER)
    public double getDropoffDurationPerPassenger() {
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
