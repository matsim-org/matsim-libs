package org.matsim.contrib.noise;

public interface NoiseEmission {

    void calculateEmission(NoiseLink link);

    double calculateSingleVehicleLevel(NoiseVehicleType type, NoiseLink noiseLink);
}
