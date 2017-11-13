// code by clruch
package playground.clruch.dispatcher.core;

/** Created by Claudio on 2/3/2017. */
public enum AVStatus {
    DRIVEWITHCUSTOMER("dwc", "with customer"), //
    DRIVETOCUSTOMER("d2c", "pickup"), //
    REBALANCEDRIVE("reb", "rebalance"), //
    STAY("sty", "stay"), //
    OFFSERVICE("off", "off service"), // TODO check if useful.
    ;

    public final String tag;
    public final String description;

    AVStatus(String xmlTag, String description) {
        this.tag = xmlTag;
        this.description = description;
    }

    public String tag() {
        return tag;
    }

    @Override
    @Deprecated // use tag() instead
    public String toString() {
        // TODO andy, use tag() instead so that we have better control of where tag is used
        return tag;
    }
}
