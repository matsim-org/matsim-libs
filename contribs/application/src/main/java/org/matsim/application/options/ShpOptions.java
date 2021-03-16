package org.matsim.application.options;

import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * Reusable class for shape file options.
 *
 * @see CommandLine.Mixin
 */
public class ShpOptions {

    @CommandLine.Option(names = "--shp", description = "Optional path to shape file used for filtering", required = false)
    private Path shp;

    @CommandLine.Option(names = "--shp-crs", description = "Overwrite coordinate system of the shape-file", defaultValue = TransformationFactory.WGS84)
    private String shpCrs;

    @CommandLine.Option(names = "--shp-charset", description = "Overwrite coordinate system of the shape-file", defaultValue = "ISO-8859-1")
    private Charset shpCharset;

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
    @Nullable
    public List<SimpleFeature> readFeatures() {
        if (shp == null)
            return null;

        try {
            ShapefileDataStore ds = (ShapefileDataStore) FileDataStoreFinder.getDataStore(shp.toFile());
            ds.setCharset(shpCharset);
            return ShapeFileReader.getSimpleFeatures(ds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
