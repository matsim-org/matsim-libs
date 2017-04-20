package playground.clruch.simonton;

/**
 * Created by Claudio on 3/23/2017.
 */
public class EuclideanDistancer implements Distancer {
    @Override
    public double getDistance(double[] d1, double[] d2) {
        return Math.hypot(d1[0] - d2[0], d1[1] - d2[1]);
    }
}
