package org.matsim.contrib.noise.data;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author nkuehnel
 */
public final class FeatureNoiseBarrierImpl implements NoiseBarrier {

    private final Geometry geom;
    private final double height;

    public FeatureNoiseBarrierImpl(Geometry geom, double height) {
        this.geom = geom.convexHull();
        this.height = height;
    }

    @Override
    public Geometry getGeometry() {
        return geom;
    }

    @Override
    public double getHeight() {
        return height;
    }
}
