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

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.handler.EventHandler;
import playground.agarwalamit.analysis.legMode.distributions.LegModeBeelineDistanceDistributionHandler;
import playground.agarwalamit.opdyts.OpdytsObjectiveFunctionCases;

/**
 * Created by amit on 21/11/2016.
 */


public class PatnaObjectiveFunctionValueWriter {

    private final PatnaCMPDistanceDistribution referenceStudyDistri ;
    private final LegModeBeelineDistanceDistributionHandler handler ;
    private final SortedMap<String, SortedMap<Double, Integer>> mode2DistanceClass2LegCount = new TreeMap<>();

    public PatnaObjectiveFunctionValueWriter(final Scenario scenario, final OpdytsObjectiveFunctionCases opdytsObjectiveFunctionCases) {
        referenceStudyDistri = new PatnaCMPDistanceDistribution(opdytsObjectiveFunctionCases);
        List<Double> dists = Arrays.stream(this.referenceStudyDistri.getDistClasses()).boxed().collect(Collectors.toList());
        this.handler = new LegModeBeelineDistanceDistributionHandler(dists, scenario.getNetwork());
    }

    public EventHandler getEventHandler() {
        return this.handler;
    }

    public double getObjectiveFunctionValue(){
        double value = 0.;

        SortedMap<String, double []> realCounts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
        double [] distClasses =  this.referenceStudyDistri.getDistClasses();
        SortedMap<String, SortedMap<Double, Integer>> simCounts = this.handler.getMode2DistanceClass2LegCounts();

        for (String mode : realCounts.keySet()) {
            for (int index = 0 ; index < realCounts.get(mode).length; index ++) {
                double distClass = distClasses[index];
                double realValue = realCounts.get(mode)[index];
                double simValue ;
                if (simCounts.containsKey(mode) && simCounts.get(mode).containsKey(distClass)) simValue = simCounts.get(mode).get(distClass);
                else simValue = 0.;
                value += Math.abs(  realValue - simValue );
            }
        }
        return value;
    }
}
