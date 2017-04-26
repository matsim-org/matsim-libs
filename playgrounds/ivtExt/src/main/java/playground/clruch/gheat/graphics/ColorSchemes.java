package playground.clruch.gheat.graphics;

import java.awt.image.BufferedImage;

public enum ColorSchemes {
    Classic(GheatPalettes.createClassic()), //
    Fire(GheatPalettes.createFire()), //
    Pbj(GheatPalettes.createPbj()), //
    Pgaitch(GheatPalettes.createPgaitch()), //
    Omg(GheatPalettes.createOmg()), //
    Orange(CustomPalettes.createOrange()), //
    OrangeContour(CustomPalettes.createOrangeContour()), //
    Green(CustomPalettes.createGreen()), //
    GreenContour(CustomPalettes.createGreenContour()), //
    Black(CustomPalettes.createBlack()), //
    Jet(InternetPalettes.createJet()), //
    Parula(InternetPalettes.createParula()), //
    ;

    public final ColorScheme colorScheme;

    private ColorSchemes(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public BufferedImage getBufferedImage() {
        return colorScheme.bufferedImage;
    }

}
