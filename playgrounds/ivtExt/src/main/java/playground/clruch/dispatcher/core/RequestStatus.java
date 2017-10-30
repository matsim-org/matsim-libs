package playground.clruch.dispatcher.core;

/** @author Andreas Aumiller */
// TODO check if useful.
public enum RequestStatus {
    REQUESTED("req", "taxi requested"), //
    ONTHEWAY("otw", "taxi on the way"), //
    PICKUP("pu", "pickup"), //
    DRIVING("drv", "driving with customer"), //
    DROPOFF("do", "dropoff"), //
    CANCELLED("can", "request cancelled"), //
    EMPTY("noc", "no customer"), //
    ;

    public final String xmlTag;
    public final String description;

    RequestStatus(String xmlTag, String description) {
        this.xmlTag = xmlTag;
        this.description = description;
    }

    @Override
    public String toString() {
        return this.xmlTag;
    }

    
}
