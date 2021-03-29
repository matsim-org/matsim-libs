package org.matsim.application.options;

import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Reusable class for shape file options.
 *
 * @see CommandLine.Mixin
 */
public final class ShpOptions {

    @CommandLine.Option(names = "--shp", description = "Optional path to shape file used for filtering", required = false)
    private Path shp;

    @CommandLine.Option(names = "--shp-crs", description = "Overwrite coordinate system of the shape-file", defaultValue = TransformationFactory.WGS84)
    private String shpCrs;

    @CommandLine.Option(names = "--shp-charset", description = "Overwrite coordinate system of the shape-file", defaultValue = "ISO-8859-1")
    private Charset shpCharset;

    public ShpOptions() {
    }

    /**
     * Constructor to use shape options manually.
     */
    public ShpOptions(Path shp, String shpCrs, Charset shpCharset) {
        this.shp = shp;
        this.shpCrs = shpCrs;
        this.shpCharset = shpCharset;
    }

    /**
     * Get the provided input crs.
     */
    public Path getShapeFile() {
        return shp;
    }

    /**
     * Get the provided target crs.
     */
    public String getShapeCrs() {
        return shpCrs;
    }

    /**
     * Read features from configured shape file.
     *
     * @return null if no shp configured.
     */
    public List<SimpleFeature> readFeatures() {
        if (shp == null)
            throw new IllegalStateException("Shape file path not specified");

        try {
            ShapefileDataStore ds = (ShapefileDataStore) FileDataStoreFinder.getDataStore(shp.toFile());
            ds.setCharset(shpCharset);
            return ShapeFileReader.getSimpleFeatures(ds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Return the union of all geometries in the shape file.
     */
    public Geometry getGeometry() {

        if (shp == null)
            throw new IllegalStateException("Shape file path not specified");

        Collection<SimpleFeature> features = readFeatures();
        if (features.size() < 1) {
            throw new IllegalStateException("There is no feature in the shape file. Aborting...");
        }
        Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry();
        if (features.size() > 1) {
            for (SimpleFeature simpleFeature : features) {
                Geometry subArea = (Geometry) simpleFeature.getDefaultGeometry();
                geometry.union(subArea);
            }
        }
        return geometry;
    }

    /**
     * Creates an index to query features from this shape file.
     *
     * @param queryCRS coordinate system of the queries
     * @param attr     the attribute to query from the shape file
     */
    public Index createIndex(String queryCRS, String attr) {

        if (shp == null)
            throw new IllegalStateException("Shape file path not specified");

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(queryCRS, shpCrs);

        try {
            return new Index(ct, attr);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Helper class to provide an index for a shapefile lookup.
     */
    public final class Index {

        private final STRtree index = new STRtree();
        private final CoordinateTransformation ct;
        private final String attr;

        /**
         * Constructor.
         *
         * @param ct   coordinate transform from query to target crs
         * @param attr attribute for the result of {@link #query(Coord)}
         */
        private Index(CoordinateTransformation ct, String attr)
                throws IOException {
            ShapefileDataStore ds = (ShapefileDataStore) FileDataStoreFinder.getDataStore(shp.toFile());
            ds.setCharset(shpCharset);

            FeatureReader<SimpleFeatureType, SimpleFeature> it = ds.getFeatureReader();
            while (it.hasNext()) {
                SimpleFeature ft = it.next();
                MultiPolygon polygon = (MultiPolygon) ft.getDefaultGeometry();

                Envelope env = polygon.getEnvelopeInternal();
                index.insert(env, ft);
            }

            it.close();
            ds.dispose();

            //log.info("Created index with size: {}, depth: {}", index.size(), index.depth());

            this.ct = ct;
            this.attr = attr;
        }

        /**
         * Query the index for first feature including a certain point.
         *
         * @return null when no features was found that contains the point
         */
        @Nullable
        @SuppressWarnings("unchecked")
        public String query(Coord coord) {
            // Because we can not easily transform the feature geometry with MATSim we have to do it the other way around...
            Coordinate p = MGC.coord2Coordinate(ct.transform(coord));

            List<SimpleFeature> result = index.query(new Envelope(p));
            for (SimpleFeature ft : result) {
                MultiPolygon polygon = (MultiPolygon) ft.getDefaultGeometry();
                if (polygon.contains(MGC.coordinate2Point(p)))
                    return (String) ft.getAttribute(attr);
            }

            return null;
            // throw new NoSuchElementException(String.format("No matching entry found for x:%f y:%f %s", x, y, p));
        }
    }

}
