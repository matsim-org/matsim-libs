package playground.clruch.gheat.graphics;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Gamma correction algorithm
 * <p/>
 * Author: Bostjan Cigan (http://zerocool.is-a-geek.net)
 */
public class GammaCorrection {
    private static BufferedImage original, gamma_corrected;

    public static BufferedImage gammaCorrection(BufferedImage original, double gamma) {
        int alpha, red, green, blue;
        int newPixel;
        double gamma_new = 1 / gamma;
        int[] gamma_LUT = gamma_LUT(gamma_new);
        BufferedImage gamma_cor = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {
                // Get pixels by R, G, B
                alpha = new Color(original.getRGB(i, j)).getAlpha();
                red = new Color(original.getRGB(i, j)).getRed();
                green = new Color(original.getRGB(i, j)).getGreen();
                blue = new Color(original.getRGB(i, j)).getBlue();
                red = gamma_LUT[red];
                green = gamma_LUT[green];
                blue = gamma_LUT[blue];
                // Return back to original format
                newPixel = colorToRGB(alpha, red, green, blue);
                // Write pixels into image
                gamma_cor.setRGB(i, j, newPixel);
            }
        }
        return gamma_cor;
    }

    // Create the gamma correction lookup table
    private static int[] gamma_LUT(double gamma_new) {
        int[] gamma_LUT = new int[256];
        for (int i = 0; i < gamma_LUT.length; i++) {
            gamma_LUT[i] = (int) (255 * (Math.pow((double) i / (double) 255, gamma_new)));
        }
        return gamma_LUT;
    }

    // Convert R, G, B, Alpha to standard 8 bit
    private static int colorToRGB(int alpha, int red, int green, int blue) {
        int newPixel = 0;
        newPixel += alpha;
        newPixel = newPixel << 8;
        newPixel += red;
        newPixel = newPixel << 8;
        newPixel += green;
        newPixel = newPixel << 8;
        newPixel += blue;
        return newPixel;
    }
}