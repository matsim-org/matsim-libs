// code by clruch
package playground.clruch.dispatcher.core;

/**
 * Created by Claudio on 2/3/2017.
 */
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
    
    // New method checking for real stay or rebalance drive
    public static AVStatus check_stay(double dist, double time) {
    	if (time >= 150 && dist <= 150) // TODO magic const, check with AVSTATUS graph.
    		return AVStatus.STAY;
    	else
    		return AVStatus.REBALANCEDRIVE;
    }
}
