package org.matsim.contrib.emissions.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class RasterTest {

	@Test
	void test() {

        var bounds = new Raster.Bounds(10, 10, 100, 100);
        var raster = new Raster(bounds, 10);

        double adjustedValue = raster.adjustValueForCoord(10, 10, 1);
        double retreivedValue = raster.getValueByCoord(10, 10);
        assertEquals(adjustedValue, retreivedValue, Double.MIN_VALUE);

        double a = raster.adjustValueForCoord(21, 21, 21);
        double b = raster.getValueByCoord(21, 21);
        assertEquals(a, b, Double.MIN_VALUE);

        double c = raster.adjustValueForCoord(100, 100, 100);
        double d = raster.getValueByCoord(100, 100);
        assertEquals(c, d, Double.MIN_VALUE);

        double e = raster.adjustValueForCoord(100, 85, 185);
        double f = raster.getValueByCoord(100, 85);
        assertEquals(e, f, Double.MIN_VALUE);

        try {
            double g = raster.adjustValueForCoord(110, 100, 101);

            fail("Should have thrown array out of bounds exception");
        } catch (Exception ex) {
            // don't do anything we've expected this.
        }
    }

	@Test
	void testInsertion() {

        var bounds = new Raster.Bounds(4, 5, 123, 244);
        var raster = new Raster(bounds, 10);

        // put 10 into each pixel
        raster.setValueForEachIndex((xi, yi) -> 10);

        raster.forEachIndex((x, y, value) -> assertEquals(10, value, 0.000001));
    }

	@Test
	void testIterationByIndex() {

        var bounds = new Raster.Bounds(4, 5, 123, 244);
        var raster = new Raster(bounds, 10);

        // put 10 into each pixel
        raster.setValueForEachIndex((xi, yi) -> 10);

        raster.forEachIndex((x, y, value) -> {
            assertEquals(10, value, 0.000001);

            // hm, what else to test
            var valueByIndex = raster.getValueByIndex(x, y);
            assertEquals(valueByIndex, value, 0.0);
        });
    }

	@Test
	void testIterationByCoord() {

        var bounds = new Raster.Bounds(4, 5, 123, 244);
        var raster = new Raster(bounds, 10);

        // put 10 into each pixel
        raster.setValueForEachIndex((xi, yi) -> 10);

        raster.forEachCoordinate((x, y, value) -> {
            assertEquals(10, value, 0.000001);

            assertEquals(0, (int) (x - bounds.getMinX()) % 10, 0.0);
            assertEquals(0., (int) (y - bounds.getMinY()) % 10, 0.0);
        });
    }
}