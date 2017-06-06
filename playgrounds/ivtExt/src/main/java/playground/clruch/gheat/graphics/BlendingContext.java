// code by varunpant
package playground.clruch.gheat.graphics;

import java.awt.CompositeContext;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

final class BlendingContext implements CompositeContext {
    private final Blender blender;
    private final BlendComposite composite;

    BlendingContext(BlendComposite composite) {
        this.composite = composite;
        this.blender = Blender.getBlenderFor(composite);
    }

    public void dispose() {
    }

    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
        int width = Math.min(src.getWidth(), dstIn.getWidth());
        int height = Math.min(src.getHeight(), dstIn.getHeight());
        float alpha = composite.getAlpha();
        int[] result = new int[4];
        int[] srcPixel = new int[4];
        int[] dstPixel = new int[4];
        int[] srcPixels = new int[width];
        int[] dstPixels = new int[width];
        for (int y = 0; y < height; y++) {
            src.getDataElements(0, y, width, 1, srcPixels);
            dstIn.getDataElements(0, y, width, 1, dstPixels);
            for (int x = 0; x < width; x++) {
                // pixels are stored as INT_ARGB
                // our arrays are [R, G, B, A]
                int pixel = srcPixels[x];
                srcPixel[0] = (pixel >> 16) & 0xFF;
                srcPixel[1] = (pixel >> 8) & 0xFF;
                srcPixel[2] = (pixel) & 0xFF;
                srcPixel[3] = (pixel >> 24) & 0xFF;
                pixel = dstPixels[x];
                dstPixel[0] = (pixel >> 16) & 0xFF;
                dstPixel[1] = (pixel >> 8) & 0xFF;
                dstPixel[2] = (pixel) & 0xFF;
                dstPixel[3] = (pixel >> 24) & 0xFF;
                blender.blend(srcPixel, dstPixel, result);
                // mixes the result with the opacity
                dstPixels[x] = ((int) (dstPixel[3] + (result[3] - dstPixel[3]) * alpha) & 0xFF) << 24 | ((int) (dstPixel[0] + (result[0] - dstPixel[0]) * alpha) & 0xFF) << 16
                        | ((int) (dstPixel[1] + (result[1] - dstPixel[1]) * alpha) & 0xFF) << 8 | (int) (dstPixel[2] + (result[2] - dstPixel[2]) * alpha) & 0xFF;
            }
            dstOut.setDataElements(0, y, width, 1, dstPixels);
        }
    }
}
