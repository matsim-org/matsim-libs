package org.matsim.contrib.noise;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

/**
 * @author nkuehnel
 */
class FeatureNoiseBarriersReader {

    private final static Logger logger = LogManager.getLogger(FeatureNoiseBarriersReader.class);
    private static final double HEIGHT_PER_LEVEL = 3.5;
    private final static String LEVELS = "levels";
    private final static String HEIGHT = "height";
    private final static int DEFAULT_HEIGHT = 10;
    private final static InvertCoordinateFilter FILTER = new InvertCoordinateFilter();

    public static Collection<FeatureNoiseBarrierImpl> read(String path, String sourceCrs, String targetCrs) {

        logger.info("Reading noise shielding objects geojson from " + path);

        FeatureJSON json = new FeatureJSON();
        FeatureCollection featureCollection = null;
        try {
            final Reader reader = GeoJSONUtil.toReader(new FileInputStream(path));
            featureCollection = json.readFeatureCollection(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<FeatureNoiseBarrierImpl> barriers = new ArrayList<>();

        MathTransform transform = null;
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(sourceCrs);
            CoordinateReferenceSystem targetCRS = CRS.decode(targetCrs);
            transform = CRS.findMathTransform(sourceCRS, targetCRS);
        } catch (FactoryException e) {
            e.printStackTrace();
        }

        final FeatureIterator features = featureCollection.features();
        int idCounter = 0;
        Counter counter = new Counter(" noise barrier #");
        while (features.hasNext()) {

            counter.incCounter();

            SimpleFeature feature = (SimpleFeature) features.next();

            final Object geometry = feature.getAttribute("geometry");

            Set<Polygon> polygons = new HashSet<>();

            if(geometry instanceof  Polygon) {
                Polygon polygon = (Polygon) geometry;
                polygons.add(polygon);
            } else if(geometry instanceof MultiPolygon) {
                for (int i = 0; i < ((MultiPolygon) geometry).getNumGeometries(); i++) {
                    polygons.add((Polygon) ((MultiPolygon) geometry).getGeometryN(i));
                }
            }

            for(Polygon polygon: polygons) {

                polygon.apply(FILTER);
                Geometry transformedPolygon = null;
                try {
                    transformedPolygon = JTS.transform(polygon, transform);
                    ;
                } catch (TransformException e) {
                    e.printStackTrace();
                }
                transformedPolygon.apply(FILTER);
                if (!polygon.isValid()) {
                    continue;
                }
                Id<NoiseBarrier> id;
                try {
                    id = Id.create((String) feature.getAttribute("id"), NoiseBarrier.class);
                } catch (Exception e) {
                    id = Id.create(idCounter, NoiseBarrier.class);
                    idCounter++;
                }

                double height = getHeight(feature);

                FeatureNoiseBarrierImpl noiseBarrier = new FeatureNoiseBarrierImpl(id, transformedPolygon, height);
                if (Double.isNaN(polygon.getCentroid().getX()) || Double.isNaN(polygon.getCentroid().getY())) {
                    logger.debug("Noise barrier ignored due to invalid centroid coordinates.");
                } else {
                    barriers.add(noiseBarrier);
                }
            }
        }
        return barriers;
    }

    private static double getHeight(SimpleFeature feature) {
        double height = DEFAULT_HEIGHT;
        final Double height1 = (Double) feature.getAttribute(HEIGHT);
        if (height1 != null) {
            height = height1;
        } else {
            final Double levels = (Double) feature.getAttribute(LEVELS);
            if (levels != null) {
                height = levels * HEIGHT_PER_LEVEL;
            }
        }
        return height;
    }

    /**
     * GeoJson uses x and y the other way around
     */
    private static class InvertCoordinateFilter implements CoordinateFilter {
        @Override
        public void filter(Coordinate coord) {
            double oldX = coord.x;
            coord.x = coord.y;
            coord.y = oldX;
        }
    }
}
