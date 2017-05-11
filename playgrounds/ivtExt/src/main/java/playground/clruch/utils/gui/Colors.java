// code by jph
package playground.clruch.utils.gui;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JProgressBar;

/** static functionality */
public class Colors {
    /**
     * JToggleButton background when selected is 184 207 229
     * selection color subtracts 24 from each RGB value
     */
    public static final Color selection = new Color(160, 183, 205);
    public static final Color selectionBrighter = new Color(182, 199, 216); // new Color(185, 212, 237); // new Color(175, 200, 224);
    // public static final Color selectionBrightest = new Color(185, 212, 237);
    //
    /** imitates color of {@link JProgressBar} text */
    public static final Color progressBar = new Color(99, 130, 191);
    /**
     * background color of java native dialogs, e.g. JFileChooser
     * color can replace gradient paint of {@link JButton}s
     */
    public static final Color panel = new Color(238, 238, 238);
    /** approximation of color gold */
    public static final Color gold = new Color(224, 149, 4);
    /** foreground color of JLabel */
    public static final Color label = new Color(51, 51, 51);
    /** background for items in menus that are selected; not Java official */
    public static final Color activeItem = new Color(243, 239, 124);

    public static Color alpha000(Color myColor) {
        return new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), 0);
    }

    public static Color alpha064(Color myColor) {
        return new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), 64);
    }

    public static Color alpha128(Color myColor) {
        return new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), 128);
    }

    public static Color alpha192(Color myColor) {
        return new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), 192);
    }

    public static Color withAlpha(Color myColor, int alpha) {
        return new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), alpha);
    }

    private Colors() {
    }
}
