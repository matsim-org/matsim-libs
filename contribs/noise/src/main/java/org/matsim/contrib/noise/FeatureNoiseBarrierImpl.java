package org.matsim.contrib.noise;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;

/**
 * @author nkuehnel
 */
 final class FeatureNoiseBarrierImpl implements NoiseBarrier {

    private final Id<NoiseBarrier> id;

    private final PreparedGeometry geom;
    private final double height;
    public FeatureNoiseBarrierImpl(Id<NoiseBarrier> id, Geometry geom, double height) {
        this.id = id;
        this.geom = PreparedGeometryFactory.prepare(geom);
        this.height = height;
    }

    @Override
    public PreparedGeometry getGeometry() {
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
