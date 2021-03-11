package org.matsim.application.options;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

/**
 * Reusable class for crs options.
 *
 * @see picocli.CommandLine.Mixin
 */
public class CRSOptions {

    @CommandLine.Option(names = "--input-cs", description = "Input coordinate system of the data", defaultValue = TransformationFactory.WGS84)
    private String inputCRS;

    @CommandLine.Option(names = "--target-cs", description = "Target coordinate system of the output", defaultValue = TransformationFactory.WGS84)
    private String targetCRS;


    /**
     * Get the provided input crs.
     */
    public String getInputCRS() {
        return inputCRS;
    }

    /**
     * Get the provided target crs.
     */
    public String getTargetCRS() {
        return targetCRS;
    }
}
