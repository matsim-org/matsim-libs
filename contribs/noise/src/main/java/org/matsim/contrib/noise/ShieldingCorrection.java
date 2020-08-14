package org.matsim.contrib.noise;

public interface ShieldingCorrection {

    /**
     * Pegeländerung D_B durch bauliche Maßnahmen (und topografische Gegebenheiten)
     * @param s distance between emission source and immission receiver point
     * @param a distance between emission source and (first) edge of diffraction
     * @param b distance between (last) edge of diffraction and immission receiver point
     * @param c sum of distances between edges of diffraction
     * @return shielding correction term for the given parameters
     */
    double calculateShieldingCorrection(double s, double a, double b, double c);
}
