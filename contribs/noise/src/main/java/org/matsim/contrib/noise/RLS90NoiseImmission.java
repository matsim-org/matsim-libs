package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Collection;

import static org.matsim.contrib.noise.RLS90VehicleType.car;

class RLS90NoiseImmission implements NoiseImmission {

    @Override
    public double calculateIsolatedLinkImmission(NoiseReceiverPoint rp, NoiseLink noiseLink) {

        double correction = rp.getLinkCorrection(noiseLink.getId());

        double noiseImmission = 0.;
        if (!(noiseLink.getEmission() == 0.)) {
            noiseImmission = noiseLink.getEmission() + correction;
            if (noiseImmission < 0.) {
                noiseImmission = 0.;
            }
        }
        return noiseImmission;
    }


    @Override
    public double calculateResultingNoiseImmission(Collection<Double> collection){

        double resultingNoiseImmission = 0.;

        if (collection.size() > 0) {
            double sumTmp = 0.;
            for (double noiseImmission : collection) {
                if (noiseImmission > 0.) {
                    sumTmp = sumTmp + (Math.pow(10, (0.1 * noiseImmission)));
                }
            }
            resultingNoiseImmission = 10 * Math.log10(sumTmp);
            if (resultingNoiseImmission < 0) {
                resultingNoiseImmission = 0.;
            }
        }
        return resultingNoiseImmission;
    }

    @Override
    public double calculateIsolatedLinkImmissionPlusOneVehicle(NoiseReceiverPoint rp, NoiseLink noiseLink, NoiseVehicleType type) {
        double plusOne = 0;
        if (!(noiseLink.getEmissionPlusOneVehicle(type.getId()) == 0.)) {
            double correction = rp.getLinkCorrection(noiseLink.getId());
            plusOne = noiseLink.getEmissionPlusOneVehicle(type.getId())
                    + correction;
        }
        return plusOne;
    }

    /**
     * Pegeländerung D_B durch bauliche Maßnahmen (und topografische Gegebenheiten)
     * @param s distance between emission source and immission receiver point
     * @param a distance between emission source and (first) edge of diffraction
     * @param b distance between (last) edge of diffraction and immission receiver point
     * @param c sum of distances between edges of diffraction
     * @return shielding correction term for the given parameters
     */
    public static double calculateShieldingCorrection(double s, double a, double b, double c) {

        //Shielding value (Schirmwert) z: the difference between length of the way from the emission source via the
// obstacle to the immission receiver point and direct distance between emission source and immission
// receiver point ~ i.e. the additional length that sound has to propagate because of shielding
        double z = a + b + c - s;

        //Weather correction for diffracted rays
        double k = Math.exp(- 1. / 2000. * Math.sqrt( (a*b*s) / (2 * z)) );

        double temp = 5 + ((70 + 0.25 * s) / (1+ 0.2 * z)) * z * Math.pow(k,2);
        double correction = 7 * Math.log10(temp);
        return correction;
    }

    public static double calculateDistanceCorrection(double distance) {
        double correctionTermDs = 15.8 - (10 * Math.log10(distance)) - (0.0142 * (Math.pow(distance, 0.9)));
        return correctionTermDs;
    }

    public static double calculateAngleCorrection(double angle) {
        double angleCorrection = 10 * Math.log10((angle) / (180));
        return angleCorrection;
    }


}
