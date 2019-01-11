package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class SpatialInterpolationTest {

    @Test
    public void calculateWeightFromLine() {

        final Coordinate from = new Coordinate(0, 1);
        final Coordinate to = new Coordinate(10, 1);
        final Coordinate cellCentroid = new Coordinate(5, 1);
        final double smoothingRadius = 1;

        double weight = SpatialInterpolation.calculateWeightFromLine(from, to, cellCentroid, smoothingRadius);

        assertTrue(0 < weight && weight < 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculateWeightFromLine_invalidSmoothingRadius() {

        final Coordinate from = new Coordinate(0, 1);
        final Coordinate to = new Coordinate(10, 1);
        final Coordinate cellCentroid = new Coordinate(5, 1);
        final double smoothingRadius = 0;

        double weight = SpatialInterpolation.calculateWeightFromLine(from, to, cellCentroid, smoothingRadius);

        fail("smoothing radius <= 0 should cause exception");
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculateWeightFromPoint_invalidSmoothingRadius() {

        final Coordinate source = new Coordinate(10, 10);
        final Coordinate centroid = new Coordinate(10, 10);
        final double smoothingRadius = 0;

        double weight = SpatialInterpolation.calculateWeightFromPoint(source, centroid, smoothingRadius);

        fail("smoothing radius <= 0 should cause exception");
    }

    @Test
    public void calculateWeightFromPoint() {

        final Coordinate source = new Coordinate(10, 10);
        final Coordinate centroid = new Coordinate(10, 10);
        final double smoothingRadius = 1;

        double weight = SpatialInterpolation.calculateWeightFromPoint(source, centroid, smoothingRadius);

        assertEquals(1, weight, 0.001);
    }
}
