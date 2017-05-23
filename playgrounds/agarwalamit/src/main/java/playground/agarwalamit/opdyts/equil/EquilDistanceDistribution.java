/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts.equil;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.matsim.api.core.v01.TransportMode;
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.opdyts.patna.PatnaCMPDistanceDistribution;

/**
 * Created by amit on 13/01/2017.
 */

@SuppressWarnings("ALL")
public final class EquilDistanceDistribution implements DistanceDistribution {

    private final Map<String, double []> mode2legs = new TreeMap<>();
    private final double [] distClasses = new double[] {0., 100., 200., 500., 1000., 2000., 5000., 10000., 20000., 50000., 100000.};
    private final OpdytsScenario opdytsScenario;

    public EquilDistanceDistribution(final OpdytsScenario opdytsScenario){
        this.opdytsScenario = opdytsScenario;
        double totalLegs = 4000;
        double carVals = 1000.;
        int bin = 8;
        String secondMode ;
        switch (opdytsScenario) {
            case EQUIL:
                secondMode = TransportMode.pt;
                break;
            case EQUIL_MIXEDTRAFFIC:
                secondMode = "bicycle";
                break;
            case PATNA_1Pct:
            case PATNA_10Pct:
                throw new RuntimeException("Use "+ PatnaCMPDistanceDistribution.class.getSimpleName()+" instead.");
            default:
                 throw new RuntimeException("not implemented yet.");
        }

        double [] carArray = new double [distClasses.length];
        double [] secondModeArray = new double[distClasses.length];

        for (int inx = 0; inx < distClasses.length; inx++) {
            if (inx == bin) {
                carArray[inx] = carVals;
                secondModeArray[inx] = totalLegs -carVals;
            } else {
                carArray[inx] = 0.;
                secondModeArray[inx] = 0.;
            }
        }

        this.mode2legs.put(TransportMode.car,carArray);
        this.mode2legs.put(secondMode,secondModeArray);
    }

    @Override
    public double [] getDistClasses(){
        return this.distClasses;
    }

    @Override
    public Map<String, double []> getMode2DistanceBasedLegs(){
        return Collections.unmodifiableMap(this.mode2legs);
    }

    @Override
    public OpdytsScenario getOpdytsScenario(){
        return this.opdytsScenario;
    }

}

