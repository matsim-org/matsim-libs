package playground.clruch.export;

import java.awt.Color;

/**
 * Created by Claudio on 2/3/2017.
 */
public enum AVStatus {
    DRIVEWITHCUSTOMER("del", "with customer", new Color(128, 0, 128)), //
    DRIVETOCUSTMER("d2c", "pickup", new Color(255, 51, 0)), //
    REBALANCEDRIVE("reb", "rebalance", new Color(0, 153, 255)), //
    STAY("stay", "stay", new Color(0, 204, 0)), //
    ;

    public final String xmlTag;
    public final String description;
    public final Color color;

    AVStatus(String xmlTag, String description, Color color) {
        this.xmlTag = xmlTag;
        this.description = description;
        this.color = color;
    }

    @Override
    public String toString() {
        return this.xmlTag;
    }

}
