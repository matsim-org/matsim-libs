package org.matsim.contrib.noise;

import org.matsim.api.core.v01.network.Link;

import java.util.Collection;

public interface NoiseImmission {

    ImmissionInfo calculateImmission(NoiseReceiverPoint rp);

    double calculateIsolatedLinkImmission(NoiseReceiverPoint rp, NoiseLink noiseLink);

    double calculateResultingNoiseImmission(Collection<Double> collection);

    double calculateIsolatedLinkImmissionPlusOneVehicle(NoiseReceiverPoint rp, NoiseLink noiseLink, NoiseVehicleType type);

    double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink);
}
