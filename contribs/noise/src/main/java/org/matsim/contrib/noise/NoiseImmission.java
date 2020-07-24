package org.matsim.contrib.noise;

import org.matsim.api.core.v01.network.Link;

public interface NoiseImmission {

    void calculateImmission(NoiseReceiverPoint rp, double currentTimeBinEndTime);

    double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink);
}
