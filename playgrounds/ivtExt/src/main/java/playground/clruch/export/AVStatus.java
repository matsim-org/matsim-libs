package playground.clruch.export;

/**
 * Created by Claudio on 2/3/2017.
 */
public enum AVStatus {
    STAY("stay"), //
    DRIVETOCUSTMER("d2c"), //
    DRIVEWITHCUSTOMER("del"), //
    REBALANCEDRIVE("reb");

    public final String xmlTag;

    AVStatus(String xmlTag) {
        this.xmlTag=xmlTag;
    }

    @Override
    public String toString() {
        return this.xmlTag;
    }

}
