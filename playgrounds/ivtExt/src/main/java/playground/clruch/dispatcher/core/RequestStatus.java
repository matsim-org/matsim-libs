package playground.clruch.dispatcher.core;

/** @author Andreas Aumiller */
// TODO check if useful.
public enum RequestStatus {
    REQUEST("req", "taxi requested"), //
    PICKUP("pu", "pickup"), //
    DROPOFF("do", "dropoff"), //
    CANCELED("can", "request canceled"), //
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
