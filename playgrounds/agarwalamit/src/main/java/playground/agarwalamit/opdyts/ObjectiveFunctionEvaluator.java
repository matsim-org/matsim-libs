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

package playground.agarwalamit.opdyts;

import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.contrib.analysis.kai.Databins;

/**
 * Created by amit on 28/11/2016.
 */


@SuppressWarnings("DefaultFileTemplate")
public class ObjectiveFunctionEvaluator {

    private static final Logger LOG = Logger.getLogger(ObjectiveFunctionEvaluator.class);
    private final SortedMap<String, Double> mode2share = new TreeMap<>();

    public enum ObjectiveFunctionType {
        SUM_ABS_DIFF, SUM_SQR_DIFF_NORMALIZED
    }

    private final ObjectiveFunctionType objectiveFunctionType;

    public ObjectiveFunctionEvaluator(){
        this(ObjectiveFunctionType.SUM_SQR_DIFF_NORMALIZED);
    }

    public ObjectiveFunctionEvaluator(final ObjectiveFunctionType objectiveFunctionType) {
        this.objectiveFunctionType = objectiveFunctionType;
    }

    public double getObjectiveFunctionValue(final Databins<String> realCounts, final Databins<String> simCounts){
        // databins to maps.

        Map<String, double [] > realCountMap = realCounts.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
        );

        Map<String, double [] > simCountMap = simCounts.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
        );

        return getObjectiveFunctionValue(realCountMap, simCountMap);
    }

    public double getObjectiveFunctionValue(final Map<String, double[]> realCounts, final Map<String, double[]> simCounts){
        double objective = 0. ;
        double realValueSum = 0;
        for ( Map.Entry<String, double[]> theEntry : realCounts.entrySet() ) {
            String mode = theEntry.getKey() ;
            double[] realValue = theEntry.getValue() ;
            double[] simValue = simCounts.get(mode);

            mode2share.put(mode, Arrays.stream(simValue).sum());

            for ( int ii=0 ; ii < realValue.length ; ii++ ) {
                double diff ;

                // realValue cant be null here.
                if (simValue == null) diff = realValue[ii];
                else if(realValue.length != simValue.length) {
                    throw new RuntimeException("The length of the real ("+realValue.length+") and sim value ("+simValue.length+ ") arrays for the mode "+mode+" are not same " +
                            "i.e. one of the distance class is missing. The simulation is aborting, because not sure, which bin is missing.");
                }
                else diff = Math.abs( realValue[ii] - simValue[ii] ) ;

                switch (this.objectiveFunctionType) {
                    case SUM_ABS_DIFF:
                        objective += diff;
                        break;
                    case SUM_SQR_DIFF_NORMALIZED:
                        objective += diff * diff;
                        realValueSum += realValue[ii];
                        break;
                    default:
                        throw new RuntimeException("not implemented yet.");
                }
            }
        }
        switch (this.objectiveFunctionType) {
            case SUM_ABS_DIFF:
                LOG.error("This should be used with great caution, with Patna, it was not a good experience. See email from GF on 24.11.2016");
                break;
            case SUM_SQR_DIFF_NORMALIZED:
                objective /= (realValueSum * realValueSum);
                break;
            default:
                throw new RuntimeException("not implemented yet.");
        }

        LOG.warn( "objective=" + objective );
        return objective;
    }

    public Map<String,Double> getModeToShare(){
        double sumShare = mode2share.values().stream().mapToDouble(Number::doubleValue).sum();
        return mode2share.entrySet().stream().collect(Collectors.toMap(entry-> entry.getKey(), entry -> entry.getValue() / sumShare ));
    }
}
