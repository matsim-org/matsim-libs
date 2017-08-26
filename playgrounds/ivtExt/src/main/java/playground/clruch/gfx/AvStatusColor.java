// code by jph
package playground.clruch.gfx;

import java.awt.Color;

import playground.clruch.dispatcher.core.AVStatus;

/* package */ enum AvStatusColor {
    Standard( //
            new Color(128, 0, 128), // with customer
            new Color(255, 51, 0), // to customer
            new Color(0, 153, 255), // rebalance
            new Color(0, 204, 0)), // stay
    Mild( //
            new Color(128, 0, 128), //
            new Color(255, 51, 0), //
            new Color(0, 153, 255), //
            new Color(128, 128, 128, 64)), //
    ;

    private final Color[] colors;
    private final Color[] dest = new Color[4];

    private AvStatusColor(Color... colors) {
        this.colors = colors;
        for (int index = 0; index < 4; ++index)
            dest[index] = _ofDest(colors[index]);
    }

    public Color of(AVStatus avStatus) {
        return colors[avStatus.ordinal()];
    }

    public Color ofDest(AVStatus avStatus) {
        return dest[avStatus.ordinal()];
    }

    private static Color _ofDest(Color color) {
        return new Color( //
                color.getRed(), //
                color.getGreen(), //
                color.getBlue(), //
                64);
    }
}
