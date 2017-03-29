package playground.clruch.gheat.graphics;

import java.awt.image.BufferedImage;

public enum ColorSchemes {
    ORANGE(CommonPalettes.createOrange()), //
    ;

    public final ColorScheme colorScheme;

    private ColorSchemes(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public BufferedImage getBufferedImage() {
        return colorScheme.bufferedImage;
    }

}
