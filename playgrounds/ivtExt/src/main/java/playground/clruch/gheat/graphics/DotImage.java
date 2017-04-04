package playground.clruch.gheat.graphics;

import java.awt.image.BufferedImage;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.ImageFormat;

public class DotImage {

    final int size;
    final BufferedImage bufferedImage;

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
    }

    private static double poly(double x) {
        // 0.498335 + 0.258199 x + 0.7263 x^2 - 0.482759 x^3
        return 0.498335 + 0.258199 * x + 0.7263 * x * x - 0.482759 * x * x * x;
    }
}
