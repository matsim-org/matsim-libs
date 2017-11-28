/**
 * 
 */
package playground.clruch.dispatcher.selfishdispatcher;

import ch.ethz.idsc.tensor.Tensor;

/** @author Claudio Ruch */
public enum FareRatioCalculator {
    ;

    /* package */ static double calcOptLightLoadFareRatio(Tensor lambda, int nV) {

        double lambda0 = lambda.Get(0).number().doubleValue();
        double lambda1 = lambda.Get(1).number().doubleValue();

        double f1 = calcf1(lambda0, lambda1, nV);

        double brackTerm = (1 + Math.pow(lambda0 / lambda1, 1 / (nV - 1)));

        double fN = Math.pow(brackTerm, nV) - Math.pow(lambda0 / lambda1, nV / (nV - 1));

        double fD = Math.pow(brackTerm, nV) - 1;

        Double fareRatio = f1 * fN * (1 / fD);

        if (fareRatio.isNaN() || lambda0 == 0.0 || lambda1 == 0.0) {
            System.err.println("fare ratio was NaN.....");
            return 1.0;
        } else {
            return fareRatio;
        }

    }

    private static double calcf1(double lambda0, double lambda1, double nV) {
        double t1 = lambda0 / lambda1;
        double t2 = (-((nV - 2) / (nV - 1)));
        double res = Math.pow(t1, t2);
        return res;

    }

}
