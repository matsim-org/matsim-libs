package roadclassification;

/**
 * Created by michaelzilske on 08/10/15.
 */
public class LinkSettings {

    private final double freespeed;
    private final double capacity;
    private final double nofLanes;

    public LinkSettings(double freespeed, double capacity, double nofLanes) {
        this.freespeed = freespeed;
        this.capacity = capacity;
        this.nofLanes = nofLanes;
    }

    public double getFreespeed() {
        return freespeed;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getNofLanes() {
        return nofLanes;
    }
}
