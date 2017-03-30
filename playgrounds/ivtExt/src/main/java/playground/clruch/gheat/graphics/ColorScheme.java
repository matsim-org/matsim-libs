package playground.clruch.gheat.graphics;

import java.awt.Color;
import java.awt.image.BufferedImage;

import playground.clruch.utils.GlobalAssert;

public class ColorScheme {
    public final BufferedImage bufferedImage = new BufferedImage(1, 256, BufferedImage.TYPE_INT_ARGB);

    public void set(int i, Color color) {
        bufferedImage.setRGB(0, i, color.getRGB());
        int rgb = bufferedImage.getRGB(0, i);
        GlobalAssert.that(rgb == color.getRGB());
    }
}
