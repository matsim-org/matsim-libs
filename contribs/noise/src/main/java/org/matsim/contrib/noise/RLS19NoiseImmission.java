package org.matsim.contrib.noise;

import org.matsim.api.core.v01.network.Link;

public class RLS19NoiseImmission implements NoiseImmission {
    @Override
    public ImmissionInfo calculateImmission(NoiseReceiverPoint rp) {
        return null;
    }

    @Override
    public double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink) {
        return 0;
    }
}
