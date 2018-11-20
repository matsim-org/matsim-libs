package org.matsim.contrib.noise.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import mil.nga.sf.GeometryType;
import mil.nga.sf.geojson.*;
import org.apache.log4j.Logger;
import org.matsim.contrib.noise.data.FeatureNoiseBarrierImpl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nkuehnel
 */
public class FeatureNoiseBarriersReader {

    private final static Logger logger = Logger.getLogger(FeatureNoiseBarriersReader.class);
    private static final double HEIGHT_PER_LEVEL = 3.5;
    private final static String LEVELS = "levels";
    private final static String HEIGHT = "height";
    private final static int DEFAULT_HEIGHT = 10;

    public static Collection<FeatureNoiseBarrierImpl> read(String path) {

        logger.info("Reading noise shielding objects geojson from " + path);
        String geoJson = null;
        try {
            geoJson = readFile(path, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        FeatureCollection featureCollection = FeatureConverter.toFeatureCollection(geoJson);

        List<FeatureNoiseBarrierImpl> barriers = new ArrayList<>();

        for (Feature feature : featureCollection.getFeatures()) {
            final GeometryType type = feature.getGeometryType();
            if (type != GeometryType.POLYGON) {
                continue;
            }

            com.vividsolutions.jts.geom.Polygon geom = createPolygon(feature);

            double height = getHeight(feature);

            FeatureNoiseBarrierImpl noiseBarrier = new FeatureNoiseBarrierImpl(geom, height);
            if (Double.isNaN(geom.getCentroid().getX()) || Double.isNaN(geom.getCentroid().getY())) {
                logger.debug("Noise barrier ignored due to invalid centroid coordinates.");
            } else {
                barriers.add(noiseBarrier);
            }
        }
        return barriers;
    }

    private static com.vividsolutions.jts.geom.Polygon createPolygon(Feature feature) {
        int noCoordinates = 0;
        for (List<Position> positions : ((Polygon) feature.getGeometry()).getCoordinates()) {
            noCoordinates += positions.size();
        }

        //Last coordinate must be the same as first
        Coordinate[] coordinates = new Coordinate[noCoordinates+1];

        int idx = 0;
        for (List<Position> positions : ((Polygon) feature.getGeometry()).getCoordinates()) {
            for (Position position : positions) {
                coordinates[idx] = new Coordinate(position.getX(), position.getY());
                idx++;
            }
        }
        coordinates[coordinates.length-1] = coordinates[0];
        return new GeometryFactory().createPolygon(coordinates);
    }

    private static double getHeight(Feature feature) {
        double height = DEFAULT_HEIGHT;
        if (feature.getProperties().containsKey(FeatureNoiseBarriersReader.HEIGHT)) {
            Object value = feature.getProperties().get(FeatureNoiseBarriersReader.HEIGHT);
            height = parse(value);
        } else if (feature.getProperties().containsKey(LEVELS)) {
            Object value = feature.getProperties().get(LEVELS);
            double levels = parse(value);
            height = levels * HEIGHT_PER_LEVEL;
        }
        return height;
    }

    private static double parse(Object o) {
        if(o instanceof Double) {
            return (Double) o;
        } else if(o instanceof  Integer){
            return (Integer) o;
        } else {
            throw new NumberFormatException("Could not parse value for height.");
        }
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
