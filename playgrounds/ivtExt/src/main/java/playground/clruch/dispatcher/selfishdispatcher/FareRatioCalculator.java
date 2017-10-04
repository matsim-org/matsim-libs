/**
 * 
 */
package playground.clruch.dispatcher.selfishdispatcher;

import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.traveldata.TravelData;

/** @author Claudio Ruch */
public enum FareRatioCalculator {
    ;

    /* package */ static double calcOptLightLoadFareRatio(TravelData travelData, int time, int nV) {
        Tensor lambda = travelData.getLambdaforTime(time);
        
//        System.out.println("lambda = " +  Pretty.of(lambda));
//        System.out.println("nV = " +  nV);

        double lambda0 = lambda.Get(0).number().doubleValue();
        double lambda1 = lambda.Get(1).number().doubleValue();
        
//        System.out.println("lambda0 = " +  lambda0);
//        System.out.println("lambda1 = " +  lambda1);

        double f1 = calcf1(lambda0, lambda1, nV);
        
//        System.out.println("f1 = " +  f1);

        double brackTerm = (1 + Math.pow(lambda0 / lambda1, 1 / (nV - 1)));
        
//        System.out.println("brackTerm = " +  brackTerm);

        double fN = Math.pow(brackTerm, nV) - Math.pow(lambda0 / lambda1, nV / (nV - 1));
        
//        System.out.println("fN = " +  fN);

        double fD = Math.pow(brackTerm, nV) - 1;
        
//        System.out.println("fD = " +  fD);

        Double fareRatio = f1 * fN * (1 / fD);
        
//        System.out.println("fare ratio full precision = " +  fareRatio);

        if (fareRatio.isNaN() || lambda0 == 0.0 || lambda1 == 0.0 ) {
            return 1.0;
        } else {
            return fareRatio;
        }

    }
    
    
    private static double calcf1(double lambda0,double lambda1, double nV){
        double t1 = lambda0/lambda1;
        double t2 = (-((nV - 2) / (nV - 1)));
        double res = Math.pow(t1    ,t2);
        
//        System.out.println("t1 = " + t1);
//        System.out.println("t2 = " + t2);
//        System.out.println("res = " + res);
        
        
        return res;
        
    }

}
