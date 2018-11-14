package org.matsim.contrib.noise.utils;

import mil.nga.sf.GeometryEnvelope;
import mil.nga.sf.geojson.Feature;
import mil.nga.sf.geojson.FeatureCollection;
import mil.nga.sf.geojson.FeatureConverter;
import org.apache.log4j.Logger;
import org.matsim.contrib.noise.data.FeatureNoiseBarrierImpl;
import org.matsim.core.utils.collections.QuadTree;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FeatureNoiseBarriersReader {

    private final static Logger logger = Logger.getLogger(FeatureNoiseBarriersReader.class);

    private FeatureNoiseBarriersReader(){}


    public static QuadTree<FeatureNoiseBarrierImpl> read(String path) {

        String geoJson = null;
        try {
            geoJson = readFile(path, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        FeatureCollection featureCollection = FeatureConverter.toFeatureCollection(geoJson);

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for(Feature feature: featureCollection.getFeatures()) {
            final GeometryEnvelope envelope = feature.getGeometry().getGeometry().getEnvelope();
            if(envelope.getMinX() < minX) {
                minX = envelope.getMinX();
            }
            if(envelope.getMinY() < minY) {
                minY = envelope.getMinY();
            }
            if(envelope.getMaxX() > maxX) {
                maxX = envelope.getMaxX();
            }
            if(envelope.getMaxY() > maxY) {
                maxY = envelope.getMaxY();
            }
        }

        QuadTree<FeatureNoiseBarrierImpl> quadTree = new QuadTree<>(minX, minY, maxX, maxY);
        for(Feature feature: featureCollection.getFeatures()) {
            FeatureNoiseBarrierImpl noiseBarrier = new FeatureNoiseBarrierImpl(feature);
            if(Double.isNaN(noiseBarrier.getCentroid().getX()) || Double.isNaN(noiseBarrier.getCentroid().getY())) {
                logger.debug("Noise barrier ignored due to invalid centroid coordinates.");
            } else {
                quadTree.put(noiseBarrier.getCentroid().getX(), noiseBarrier.getCentroid().getY(), noiseBarrier);
            }
        }
        return quadTree;
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
