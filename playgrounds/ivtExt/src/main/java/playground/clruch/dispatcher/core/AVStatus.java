// code by clruch
package playground.clruch.dispatcher.core;

/** Created by Claudio on 2/3/2017. */
public enum AVStatus {
    DRIVEWITHCUSTOMER("del", "with customer"), //
    DRIVETOCUSTMER("d2c", "pickup"), //
    REBALANCEDRIVE("reb", "rebalance"), //
    STAY("stay", "stay"), //
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

    // TODO move this to another file
    // New method checking for real stay, rebalance drive or offservice
    public static AVStatus check_stay(int now, int lastTimeStamp, double distanceMoved, double timePassed) {
        if (Math.abs(now - lastTimeStamp) >= 2700) // TODO magic const.
            return AVStatus.OFFSERVICE;
        else if (timePassed >= 150 && distanceMoved <= 150) // TODO magic const, check with AVSTATUS graph.
            return AVStatus.STAY;
        else
            return AVStatus.REBALANCEDRIVE;
    }
}
