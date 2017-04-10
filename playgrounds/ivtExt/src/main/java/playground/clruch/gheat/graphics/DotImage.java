package playground.clruch.gheat.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.ImageFormat;

public class DotImage {

    final int size;
    public final BufferedImage bufferedImage;
    public final BufferedImage bufferedImageRGB;

    public DotImage(int zoom) {
        size = 3 * (zoom + 1);
        double mid = (size - 1) / 2;
        Tensor img = Array.zeros(size, size);
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y) {
                double rad = Math.min(Math.hypot((x - mid) / mid, (y - mid) / mid), 1);
                int rgb = (int) Math.min(Math.max(0, Math.round(poly(rad) * 255)), 255);
                img.set(RealScalar.of(rgb), x, y);
            }
        bufferedImage = ImageFormat.of(img);
        bufferedImageRGB = convert(bufferedImage, BufferedImage.TYPE_INT_RGB);
    }

    private static double poly(double x) {
        // 0.498335 + 0.258199 x + 0.7263 x^2 - 0.482759 x^3
        return Math.pow(0.498335 + 0.258199 * x + 0.7263 * x * x - 0.482759 * x * x * x, .25);
    }

    public int getWidth() {
        return size;
    }

    private static BufferedImage convert(BufferedImage src, int bufImgType) {
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), bufImgType);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        return img;
    }

}
