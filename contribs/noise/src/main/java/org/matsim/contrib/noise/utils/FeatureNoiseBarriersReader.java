package org.matsim.contrib.noise.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.noise.data.FeatureNoiseBarrierImpl;
import org.matsim.contrib.noise.data.NoiseBarrier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * @author nkuehnel
 */
public class FeatureNoiseBarriersReader {

    private final static Logger logger = Logger.getLogger(FeatureNoiseBarriersReader.class);
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
        while (features.hasNext()) {
            SimpleFeature feature = (SimpleFeature) features.next();

            final Object geometry = feature.getAttribute("geometry");
            if(!(geometry instanceof  Polygon)) {
                System.out.println("test1");
                continue;
            }
            Polygon polygon = (Polygon) geometry;
            polygon.apply(FILTER);
            Geometry transformedPolygon = null;
            try {
                transformedPolygon = JTS.transform(polygon, transform);;
            } catch (TransformException e) {
                e.printStackTrace();
            }
            transformedPolygon.apply(FILTER);
            if(!polygon.isValid()) {
                System.out.println("test");
                continue;
            }
            Id<NoiseBarrier> id = Id.create((String) feature.getAttribute("id"), NoiseBarrier.class);
            double height = getHeight(feature);


            FeatureNoiseBarrierImpl noiseBarrier = new FeatureNoiseBarrierImpl(id, transformedPolygon, height);
            if (Double.isNaN(polygon.getCentroid().getX()) || Double.isNaN(polygon.getCentroid().getY())) {
                logger.debug("Noise barrier ignored due to invalid centroid coordinates.");
            } else {
                barriers.add(noiseBarrier);
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

    private static class InvertCoordinateFilter implements CoordinateFilter {
        @Override
        public void filter(Coordinate coord) {
            double oldX = coord.x;
            coord.x = coord.y;
            coord.y = oldX;
        }
    }
}
