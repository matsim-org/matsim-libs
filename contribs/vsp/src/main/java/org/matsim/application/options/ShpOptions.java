package org.matsim.application.options;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

import java.nio.file.Path;

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
}
