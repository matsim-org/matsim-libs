package org.matsim.contrib.analysis.spatial;

import org.apache.commons.math3.special.Erf;
import org.locationtech.jts.geom.Coordinate;

public abstract class SpatialInterpolation {

    /**
     * This uses a gaussian distance weighting to calculate the impact of link based emissions onto the centroid of a
     * grid cell. The level of emission is assumed to be linear over the link. The calculation is described in Argawal's
     * PhD thesis https://depositonce.tu-berlin.de/handle/11303/6266 in Appendix A.2
     *
     * @param from         Link from coordinate
     * @param to           Link to coordinate
     * @param cellCentroid centroid of the impacted cell
     * @return weight factor by which the emission value should be multiplied to calculate the impact of the cell
     */
    public static double calculateWeightFromLine(final Coordinate from, final Coordinate to, final Coordinate cellCentroid, final double smoothingRadius) {

        if (smoothingRadius <= 0)
            throw new IllegalArgumentException("smoothing radius must be greater 0");

        double a = from.distance(cellCentroid) * from.distance(cellCentroid);
        double b = (to.x - from.x) * (from.x - cellCentroid.x) + (to.y - from.y) * (from.y - cellCentroid.y);
        double linkLength = from.distance(to);

        double c = (smoothingRadius * Math.sqrt(Math.PI) / (linkLength * 2)) * Math.exp(-(a - (b * b / (linkLength * linkLength))) / (smoothingRadius * smoothingRadius));

        double upperLimit = linkLength + b / linkLength;
        double lowerLimit = b / linkLength;
        double integrationUpperLimit = Erf.erf(upperLimit / smoothingRadius);
        double integrationLowerLimit = Erf.erf(lowerLimit / smoothingRadius);
        double weight = c * (integrationUpperLimit - integrationLowerLimit);

        if (weight < 0)
            throw new RuntimeException("Weight may not be negative! Value: " + weight);

        return weight;
    }

    /**
     * This uses an exponential distance weighting to calculate the impact of point based emissions onto the centroid of
     * a grid cell. The calculation is described in Kickhoefer's PhD thesis https://depositonce.tu-berlin.de/handle/11303/4386
     * in Appendix A.2
     *
     * @param emissionSource Centroid of the link
     * @param cellCentroid   Centroid of the impacted cell
     * @return weight factor by which the emission value should be multiplied to calculate the impact of the cell
     */
    @SuppressWarnings("WeakerAccess")
    public static double calculateWeightFromPoint(final Coordinate emissionSource, final Coordinate cellCentroid, double smoothingRadius) {

        if (smoothingRadius <= 0)
            throw new IllegalArgumentException("smoothing radius must be greater 0");
        double dist = emissionSource.distance(cellCentroid);
        return Math.exp((-dist * dist) / (smoothingRadius * smoothingRadius));
    }
}
