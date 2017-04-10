package playground.clruch.gheat.graphics;

import java.awt.image.BufferedImage;

public enum ColorSchemes {
    ORANGE(CommonPalettes.createOrange()), //
    ORANGE_CONTOUR(CommonPalettes.createOrangeContour()), //
    GREEN(CommonPalettes.createGreen()), //
    GREEN_CONTOUR(CommonPalettes.createGreenContour()), //
    BLACK(CommonPalettes.createBlack()), //
    CLASSIC(CommonPalettes.createClassic()), //
    ;

    public final ColorScheme colorScheme;

    private ColorSchemes(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public BufferedImage getBufferedImage() {
        return colorScheme.bufferedImage;
    }

}
