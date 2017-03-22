package playground.clruch.red3gen;

/**
 *
 */
public class SquareEuclideanDistanceFunction implements DistanceFunction {
  @Override
  public double distance(double[] p1, double[] p2) {
    double d = 0;
    for (int i = 0; i < p1.length; i++) {
      double diff = (p1[i] - p2[i]);
      d += diff * diff;
    }
    return d;
  }

  @Override
  public double distanceToRect(double[] point, double[] min, double[] max) {
    double d = 0;
    for (int i = 0; i < point.length; i++) {
      double diff = 0;
      if (point[i] > max[i]) {
        diff = (point[i] - max[i]);
      } else if (point[i] < min[i]) {
        diff = (point[i] - min[i]);
      }
      d += diff * diff;
    }
    return d;
  }
}