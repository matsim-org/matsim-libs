package playground.clruch.gheat.graphics;

import java.awt.Color;

public class CommonPalettes {

    public static ColorScheme createOrange() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, new Hue(.11111, 1 - c / 256., 1, 1 - c / 256.).rgba);
        // colorScheme.set(c, new Color(255, 128 + c / 2, c));
        colorScheme.set(255, new Color(0, 0, 0, 0));
        return colorScheme;
    }

    public static ColorScheme createGreen() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, new Hue(.33333, 1 - c / 256., 1, 1 - c / 256.).rgba);
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
            colorScheme.set(c, c < 128 ? new Color(255, 159, 86, 128) : new Color(0, 0, 0, 0));
        return colorScheme;
    }

    public static ColorScheme createGreenContour() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, c < 128 ? new Color(0, 255, 0, 128) : new Color(0, 0, 0, 0));
        return colorScheme;
    }

}
