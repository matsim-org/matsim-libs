/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,     *
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
package playground.johannes.gsv.synPop.mid;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * @author jillenberger
 */
public class Route2GeoDistance implements EpisodeTask {

    private UnivariateRealFunction function;

    public Route2GeoDistance(UnivariateRealFunction function) {
        this.function = function;
    }

    @Override
    public void apply(Episode episode) {
        for(Attributable leg : episode.getLegs()) {
            String routeDist = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
            if(routeDist != null) {
                double rDist = Double.parseDouble(routeDist);

                double gDist = 0;
                try {
                    gDist = function.value(rDist);
                } catch (FunctionEvaluationException e) {
                    e.printStackTrace();
                }

                leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, String.valueOf(gDist));
            }
        }
    }
}
