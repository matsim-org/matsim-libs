// code by jph
package playground.clruch.gheat.graphics;

import java.awt.Color;
import java.awt.image.BufferedImage;

import playground.clruch.utils.GlobalAssert;

public class ColorScheme {
    public final BufferedImage bufferedImage = new BufferedImage(1, 256, BufferedImage.TYPE_INT_ARGB);

    /**
     * 
     * @param i
     *            ranges from 0 to 255 inclusive
     * @param color
     */
    public void set(int i, Color color) {
        bufferedImage.setRGB(0, i, color.getRGB());
        int rgb = bufferedImage.getRGB(0, i);
        GlobalAssert.that(rgb == color.getRGB());
    }

    /**
     * 
     * @param i
     *            ranges from 0 to 255 inclusive
     * @return color corresponding to value i in range [0, 255]
     */
    public Color get(int i) {
        return new Color(bufferedImage.getRGB(0, i), true);
    }
}
