package org.matsim.contrib.noise.data;

import mil.nga.sf.Point;
import mil.nga.sf.geojson.Feature;
import org.matsim.api.core.v01.Coord;

public class FeatureNoiseBarrierImpl implements NoiseBarrier {

    private final Feature geoJsonFeature;

    public FeatureNoiseBarrierImpl(Feature feature) {
        this.geoJsonFeature = feature;
    }

    public Coord getCentroid() {
        final Point centroid = geoJsonFeature.getGeometry().getGeometry().getCentroid();
        return new Coord(centroid.getX(), centroid.getY());
    }
}
