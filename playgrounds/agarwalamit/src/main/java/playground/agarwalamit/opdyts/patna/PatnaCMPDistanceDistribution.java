/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.opdyts.patna;

import java.util.SortedMap;
import java.util.TreeMap;
import playground.agarwalamit.opdyts.OpdytsScenarios;

/**
 * Created by amit on 21/10/16.
 */


public final class PatnaCMPDistanceDistribution {

    private final SortedMap<String, double []> mode2legs = new TreeMap<>();
    private final double [] distClasses = new double[] {0., 2000., 4000., 6000., 8000., 10000.};
    private double legsSumAllModes = 0;

    public PatnaCMPDistanceDistribution (final OpdytsScenarios opdytsScenarios){
        double totalLegs = 0 ;
        switch (opdytsScenarios) {
            case EQUIL:
            case EQUIL_MIXEDTRAFFIC:
                default:
                throw new RuntimeException("not implemented yet.");
            case PATNA_1Pct:
                totalLegs = 13278.0 * 2.0 ;
                break;
            case PATNA_10Pct:
                totalLegs = 13278.0 * 2.0 * 10.0 ;
                break;
        }
        {
            double [] carVals = {6.0, 17.0, 20.0, 19.0, 26.0, 12.0};
            double carLegs = 0.02 * totalLegs;
            mode2legs.put("car", getModeDistanceLegs(carLegs, carVals));
        }
        {
            double [] motorbikeVals = {7.0, 35.0, 19.0, 23.0, 8.0, 8.0};
            double motorbikeLegs = 0.14 * totalLegs;
            mode2legs.put("motorbike", getModeDistanceLegs(motorbikeLegs, motorbikeVals));
        }
        {
            double [] bikeVals = {10.0, 51.0, 16.0, 15.0, 1.0, 7.0};
            double bikeLegs = 0.33 * totalLegs;
            mode2legs.put("bike", getModeDistanceLegs(bikeLegs, bikeVals));
        }
//        {
//            double [] ptVals = {6.4, 23.9, 34.5, 10.5, 12.7, 12.0};
//            double ptLegs = 0.22 * totalLegs;
//            mode2legs.put("pt", getModeDistanceLegs(ptLegs, ptVals));
//        }
//        {
//            double [] walkVals = {70.0, 28.0, 1.0, 1., 0.0, 0.0};
//            double walkLegs = 0.29 * totalLegs;
//            mode2legs.put("walk", getModeDistanceLegs(walkLegs, walkVals));
//        }

        System.out.println("Total legs "+totalLegs+" ans sum of all legs "+legsSumAllModes);

        // check if difference is not greaater than 1%, due to rounding.
        if( legsSumAllModes >= 0.99*totalLegs && legsSumAllModes <= 1.01 * totalLegs) {
            // everything is fine
        }  else {
//            throw new RuntimeException("sum of legs is wrong.");
        }
    }

    private double [] getModeDistanceLegs(final double modeLegs, final double [] modeModalSharePerDistance) {
        double [] modeDistLegs = new double [this.distClasses.length] ;
        for (int idx = 0; idx < this.distClasses.length; idx++) {
            double legs = Math.round( modeLegs * modeModalSharePerDistance[idx] / 100.);
            modeDistLegs [idx] = legs;
            legsSumAllModes += legs;
        }
        return modeDistLegs;
    }

    public double [] getDistClasses(){
        return this.distClasses;
    }

    public SortedMap<String, double []> getMode2DistanceBasedLegs(){
        return this.mode2legs;
    }
}
