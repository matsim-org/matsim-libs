package playground.clruch.gheat.graphics;

import java.awt.Color;

public class CommonPalettes {

    public static ColorScheme createOrange() {
        ColorScheme colorScheme = new ColorScheme();
        for (int c = 0; c < 256; ++c)
            colorScheme.set(c, new Color(255, 128 + c / 2, c));
        return colorScheme;
    }

}
