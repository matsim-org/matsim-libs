package org.matsim.contrib.noise.data;


import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;

/**
 * @author nkuehnel
 */
public final class FeatureNoiseBarrierImpl implements NoiseBarrier {

    private final Id<NoiseBarrier> id;

    private final Geometry geom;
    private final double height;
    public FeatureNoiseBarrierImpl(Id<NoiseBarrier> id, Geometry geom, double height) {
        this.id = id;
        this.geom = geom;
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

    @Override
    public Id<NoiseBarrier> getId() {
        return id;
    }
}
