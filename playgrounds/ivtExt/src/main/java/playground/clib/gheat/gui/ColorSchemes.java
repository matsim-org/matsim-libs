// code by jph
package playground.clib.gheat.gui;

import java.awt.image.BufferedImage;

public enum ColorSchemes {
    Jet(InternetPalettes.createJet()), //
    Classic(GheatPalettes.createClassic()), //
    Fire(GheatPalettes.createFire()), //
    Pbj(GheatPalettes.createPbj()), //
    Parula(InternetPalettes.createParula()), //
    Pgaitch(GheatPalettes.createPgaitch()), //
    Omg(GheatPalettes.createOmg()), //
    Orange(CustomPalettes.createOrange()), //
    OrangeContour(CustomPalettes.createOrangeContour()), //
    Green(CustomPalettes.createGreen()), //
    GreenContour(CustomPalettes.createGreenContour()), //
    Black(CustomPalettes.createBlack()), //
    Bone(InternetPalettes.createBone()), //
    Cool(InternetPalettes.createCool()), //
    ;

    public final ColorScheme colorScheme;

    private ColorSchemes(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public BufferedImage getBufferedImage() {
        return colorScheme.bufferedImage;
    }

}
