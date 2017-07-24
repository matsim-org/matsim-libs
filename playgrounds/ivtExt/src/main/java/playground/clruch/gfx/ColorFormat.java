// code by jph
package playground.clruch.gfx;

import java.awt.Color;

import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.Primitives;

// EXPERIMENTAL
/* package */ enum ColorFormat {
  ;
  /** @param argb encoding color as 0xAA:RR:GG:BB
   * @return tensor with {@link Scalar} entries as {R, G, B, A} */
  static Tensor toVector(int argb) {
    return toVector(new Color(argb, true));
  }

  /** @param color
   * @return tensor with {@link Scalar} entries as {R, G, B, A} */
  static Tensor toVector(Color color) {
    return Tensors.vector(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
  }

  /** @param vector with length() == 4
   * @return encoding color as 0xAA:RR:GG:BB */
  static int toInt(Tensor vector) {
    return toColor(vector).getRGB();
  }

  /** @param vector with length() == 4
   * @return int in hex 0xAA:RR:GG:BB */
  static Color toColor(Tensor vector) {
    int[] rgba = Primitives.toArrayInt(vector);
    return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
  }
}
