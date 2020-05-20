package org.matsim.contrib.emissions.analysis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RasterTest {

    @Test
    public void test() {

        var bounds = new Raster.Bounds(10, 10, 100, 100);
        var raster = new Raster(bounds, 10);

        double adjustedValue = raster.adjustValue(10, 10, 1);
        double retreivedValue = raster.getValue(10, 10);
        assertEquals(adjustedValue, retreivedValue, Double.MIN_VALUE);

        double a = raster.adjustValue(21, 21, 21);
        double b = raster.getValue(21, 21);
        assertEquals(a, b, Double.MIN_VALUE);

        double c = raster.adjustValue(100, 100, 100);
        double d = raster.getValue(100, 100);
        assertEquals(c, d, Double.MIN_VALUE);

        double e = raster.adjustValue(100, 85, 185);
        double f = raster.getValue(100, 85);
        assertEquals(e, f, Double.MIN_VALUE);

        try {
            double g = raster.adjustValue(110, 100, 101);

            fail("Should have thrown array out of bounds exception");
        } catch (Exception ex) {
            // don't do anything we've expected this.
        }
    }

}