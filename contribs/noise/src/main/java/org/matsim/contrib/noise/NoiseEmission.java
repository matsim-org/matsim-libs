package org.matsim.contrib.noise;

/**
 * @author nkuehnel
 */
public interface NoiseEmission {

    /**
     * Trigegrs the calculation of noise emission for the current time bin at the link.
     * The method is not expected to return the emission but should use the methods provided in
     * {@link NoiseReceiverPoint} to set emission values.
     * @param link The link emissions are calculated for.
     */
    void calculateEmission(NoiseLink link);

    /**
     * Returns the noise contribution of a single vehicle of a given type on a given link.
     * @param type The vehicle type as specified in the implemented guideline.
     * @param noiseLink The link.
     * @return noise emission in dB(A)
     */
    double calculateSingleVehicleLevel(NoiseVehicleType type, NoiseLink noiseLink);
}
