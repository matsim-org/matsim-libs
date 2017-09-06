// code by jph
package playground.clib.gheat.gui;

import java.awt.Color;

import ch.ethz.idsc.tensor.img.Hue;

public class CustomPalettes {

    public static ColorScheme createOrange() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, Hue.of(.11111, 1 - c / 256., 1, (1 - c / 256.)));
        // colorScheme.set(c, new Color(255, 128 + c / 2, c));
        colorScheme.set(255, new Color(0, 0, 0, 0));
        return colorScheme;
    }

    public static ColorScheme createGreen() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, Hue.of(.33333, 1 - c / 256., 1, (1 - c / 256.)));
        colorScheme.set(255, new Color(0, 0, 0, 0));
        return colorScheme;
    }

    public static ColorScheme createBlack() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, new Color(0, 0, 0, 255 - c));
        return colorScheme;
    }

    public static ColorScheme createOrangeContour() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, c < 192 ? new Color(255, 159, 86, 128) : new Color(0, 0, 0, 0));
        return colorScheme;
    }

    public static ColorScheme createGreenContour() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, c < 192 ? new Color(0, 255, 0, 128) : new Color(0, 0, 0, 0));
        return colorScheme;
    }

}
