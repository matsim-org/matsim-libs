package playground.clruch.export;

import java.awt.Color;

/**
 * Created by Claudio on 2/3/2017.
 */
public enum AVStatus {
    STAY("stay", new Color(0, 204, 0)), //
    DRIVETOCUSTMER("d2c", new Color(255, 51, 0)), //
    DRIVEWITHCUSTOMER("del", new Color(128, 0, 128)), //
    REBALANCEDRIVE("reb", new Color(0, 153, 255));

    // color avStayColor = color(0, 204, 0);
    // color avD2CColor = color(255, 51, 0);
    // color avDelColor = color(128, 0, 128);
    // color avRebColor = color(0, 153, 255);

    public final String xmlTag;
    public final Color color;

    AVStatus(String xmlTag, Color color) {
        this.xmlTag = xmlTag;
        this.color = color;
    }

    @Override
    public String toString() {
        return this.xmlTag;
    }

}
