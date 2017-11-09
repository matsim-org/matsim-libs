package playground.clruch.dispatcher.core;

/** @author Andreas Aumiller */
// TODO check if useful.
public enum RequestStatus {
    REQUESTED("req", "taxi requested"), // TODO add descriptions
    PICKUPDRIVE("otw", "taxi on the way"), //
    PICKUP("pup", "pickup"), //
    DRIVING("drv", "driving with customer"), //
    CANCELLED("can", "request cancelled"), //
    DROPOFF("dof", "dropoff"), //
    EMPTY("noc", "no customer"), //
    ;

    public final String tag;
    public final String description;

    RequestStatus(String xmlTag, String description) {
        this.tag = xmlTag;
        this.description = description;
    }

    public String tag() {
        return tag;
    }
    
    @Override
    @Deprecated // use tag() instead
    public String toString() {
        return tag;
    }

    
}
