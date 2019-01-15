package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class SpatialInterpolationTest {

    @Test
    public void calculateWeightFromLine() {

        final Coordinate from = new Coordinate(1, 1);
        final Coordinate to = new Coordinate(4, 5);
        final Coordinate cell = new Coordinate (4, 1);
        final double smoothingRadius = 0.9;

        double weight = SpatialInterpolation.calculateWeightFromLine(from, to, cell, smoothingRadius);
        // expected value is taken from running playground.agarwalamit.analysis.spatial.SpatialInterpolation with same
        // parameters
        assertEquals(0.0011109434390476694, weight, 0.001);
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

        final Coordinate point = new Coordinate(5, 5);
        final Coordinate cell = new Coordinate (9, 5);
        final double smoothingRadius = 1;

        double weight = SpatialInterpolation.calculateWeightFromPoint(point, cell, smoothingRadius);

        // expected value is taken from running playground.agarwalamit.analysis.spatial.SpatialInterpolation with same
        // parameters
        assertEquals(1.1253517471925912E-7, weight, 0.001);
    }
}
