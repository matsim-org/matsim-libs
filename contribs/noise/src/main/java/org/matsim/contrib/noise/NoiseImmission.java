package org.matsim.contrib.noise;

import org.matsim.api.core.v01.network.Link;

public interface NoiseImmission {

    ImmissionInfo calculateImmission(NoiseReceiverPoint rp);

    double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink);
}
