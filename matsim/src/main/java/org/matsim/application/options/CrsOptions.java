package org.matsim.application.options;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

/**
 * Reusable class for crs options.
 *
 * @see picocli.CommandLine.Mixin
 */
public final class CrsOptions {

    @CommandLine.Option(names = "--input-crs", description = "Input coordinate system of the data")
    private String inputCRS;

    @CommandLine.Option(names = "--target-crs", description = "Target coordinate system of the output")
    private String targetCRS;

    /**
     * Construct crs options with default input crs.
     */
    public CrsOptions(String inputCRS) {
        this.inputCRS = inputCRS;
    }

    /**
     * Construct crs options with default input and target crs.
     */
    public CrsOptions(String inputCRS, String targetCRS) {
        this.inputCRS = inputCRS;
        this.targetCRS = targetCRS;
    }

    /**
     * Default with new predefined options.
     */
    public CrsOptions() { }

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

    /**
     * Create coordinate transformation from the options.
     */
    public CoordinateTransformation getTransformation() {
        if (inputCRS == null)
            throw new IllegalArgumentException(("inputCRS not specified!"));

        if (targetCRS == null)
            throw new IllegalArgumentException(("targetCRS not specified!"));

        return TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS);
    }

}
