package org.matsim.contrib.noise;

import org.matsim.api.core.v01.network.Link;

/**
 * @author nkuehnel
 */
public interface NoiseImmission {

    /**
     * Trigegrs the calculation of noise immission for the current time bin at the given receiver point.
     * The method is not expected to return the immission but should use the methods provided in
     * {@link NoiseReceiverPoint} to set immission values.
     * @param rp The receiver point.
     * @param currentTimeBinEndTime The current time bin end time (usually first minute of an hour, e.g. 08:00:00),
     *                             in seconds.
     */
    void calculateImmission(NoiseReceiverPoint rp, double currentTimeBinEndTime);

    /**
     * Returns the correction term for a given link-receiver point relation.
     * @param projectedDistance The orthogonal projected distance from receiver point to link.
     * @param nrp The noise receiver point.
     * @param candidateLink The related link.
     * @return the correction term in dB(A)
     */
    double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink);

    void setCurrentRp(NoiseReceiverPoint nrp);
}
