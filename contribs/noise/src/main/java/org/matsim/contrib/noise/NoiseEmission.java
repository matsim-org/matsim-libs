package org.matsim.contrib.noise;

public interface NoiseEmission {

    void calculateEmission(NoiseLink link);


    double calculateVehicleTypeEmission(NoiseVehicleType vehicleType, double vehicleVelocity, NoiseLink noiseLink);
}
