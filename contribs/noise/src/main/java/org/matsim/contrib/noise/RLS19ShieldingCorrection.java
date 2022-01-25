package org.matsim.contrib.noise;

public class RLS19ShieldingCorrection implements ShieldingCorrection{

    @Override
    public double calculateShieldingCorrection(double s, double a, double b, double c) {
        /**
         * Shielding value (Schirmwert) z: the difference between length of the way from the emission source via the
         * obstacle to the immission receiver point and direct distance between emission source and immission
         * receiver point ~ i.e. the additional length that sound has to propagate because of shielding
         */
        double z = a + b + c - s;

        //Weather correction for diffracted rays
        double k = Math.exp(- 1. / 2000. * Math.sqrt( (a*b*s) / (2 * z)) );

        return 10 * Math.log10(3 + 80 * z * k);
    }
}
