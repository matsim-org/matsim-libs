package playground.clruch.export;

/**
 * Created by Claudio on 2/3/2017.
 */
public enum AVStatus {
    STAY("stay"), //
    DRIVETOCUSTMER("d2c"), //
    DRIVEWITHCUSTOMER("del"), //
    REBDRIVE("reb");

    public final String xmlTag;

    private AVStatus(String xmlTag) {
        this.xmlTag=xmlTag;
    }
}
