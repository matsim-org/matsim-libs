package playground.clruch.gheat.graphics;

import java.awt.image.BufferedImage;

public enum ColorSchemes {
    Classic(CommonPalettes.createClassic()), //
    Fire(CommonPalettes.createFire()), //
    Pbj(CommonPalettes.createPbj()), //
    Pgaitch(CommonPalettes.createPgaitch()), //
    Omg(CommonPalettes.createOmg()), //
    Orange(CommonPalettes.createOrange()), //
    OrangeContour(CommonPalettes.createOrangeContour()), //
    Green(CommonPalettes.createGreen()), //
    GreenContour(CommonPalettes.createGreenContour()), //
    Black(CommonPalettes.createBlack()), //
    ;

    public final ColorScheme colorScheme;

    private ColorSchemes(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public BufferedImage getBufferedImage() {
        return colorScheme.bufferedImage;
    }

}
