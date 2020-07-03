package org.matsim.contrib.noise;

public interface NoiseEmissionStrategy {

    void calculateEmission(NoiseLink link);
}
