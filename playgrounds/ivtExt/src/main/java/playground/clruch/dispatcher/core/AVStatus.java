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

    public final String xmlTag;
    public final String description;

    AVStatus(String xmlTag, String description) {
        this.xmlTag = xmlTag;
        this.description = description;
    }

    @Override
    public String toString() {
        return this.xmlTag;
    }
}
