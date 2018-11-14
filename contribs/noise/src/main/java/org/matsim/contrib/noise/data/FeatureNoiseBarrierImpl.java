package org.matsim.contrib.noise.data;

import mil.nga.sf.geojson.Feature;

public class FeatureNoiseBarrierImpl implements NoiseBarrier {

    private final Feature geoJsonFeature;

    public FeatureNoiseBarrierImpl(Feature feature) {
        this.geoJsonFeature = feature;
    }
}
