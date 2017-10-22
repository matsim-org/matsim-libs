// code by jph
package playground.clruch.gfx;

import java.awt.Color;

import playground.clruch.dispatcher.core.AVStatus;

/* package */ enum AvStatusColor {
	// Added new OFFSERVICE color set otherwise it leads to error running simulationviewer
    Standard( //
            new Color(128, 0, 128), // with customer
            new Color(255, 51, 0), // to customer
            new Color(0, 153, 255), // rebalance
            new Color(0, 204, 0), // stay
    		new Color(64, 64, 64)), // off service
    Mild( //
            new Color(92, 0, 92), //
            new Color(224, 32, 0), //
            new Color(0, 128, 224), //
            new Color(0, 128, 0, 64), //
    		new Color(128, 128, 128)), // off service
    /***
     * New poppy color set
     */
    Pop( // 
    		new Color(255, 0, 0), // with customer
            new Color(255, 192, 0), // to customer
            new Color(0, 224, 255), // rebalance
            new Color(0, 255, 0), // stay
    		new Color(32, 32, 32)), // off service
    ;

    private final Color[] colors;
    private final Color[] dest = new Color[5];

    private AvStatusColor(Color... colors) {
        this.colors = colors;
        for (int index = 0; index < 5; ++index)
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
