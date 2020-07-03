package org.matsim.contrib.noise;

import java.util.Collection;

public interface NoiseImmission {


    double calculateIsolatedLinkImmission(NoiseReceiverPoint rp, NoiseLink noiseLink);

    double calculateResultingNoiseImmission(Collection<Double> collection);

    double calculateIsolatedLinkImmissionPlusOneVehicle(NoiseReceiverPoint rp, NoiseLink noiseLink, NoiseVehicleType type);
}
