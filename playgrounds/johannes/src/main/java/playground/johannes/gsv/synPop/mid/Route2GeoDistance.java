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

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTask;

/**
 * @author jillenberger
 */
public class Route2GeoDistance implements ProxyPlanTask {

    private double min = 0.5;

    private double A = 4;

    private double alpha = -0.5;

    public Route2GeoDistance(double A, double alpha, double min) {
        this.A = A;
        this.alpha = alpha;
        this.min = min;
    }

    @Override
    public void apply(ProxyPlan plan) {
        for(ProxyObject leg : plan.getLegs()) {
            String routeDist = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
            if(routeDist != null) {
                double rDist = Double.parseDouble(routeDist);
                rDist = rDist / 1000.0;

                double factor = 1 - A * Math.pow(rDist, alpha);
                factor = Math.max(0.5, factor);

                double gDist = rDist * factor * 1000.0;

                leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, String.valueOf(gDist));
            }
        }

    }
}
