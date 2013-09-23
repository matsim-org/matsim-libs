/* *********************************************************************** *
 * project: org.matsim.*
 * PtPSLCalculator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.pathsize;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.routes.ExperimentalTransitRoute;


/**
 * @author dgrether
 *
 */
public class PtPSLCalculator implements PSLCalculator {

//	private static final Logger log = Logger.getLogger(PtPSLCalculator.class);
	
	@Override
	public void calculatePSLValues(List<PSLPlanData> plans) {
		for (PSLPlanData plan : plans) {
			this.calcPtPlanLength(plan);
		}
		for (PSLPlanData plan : plans) {
			double pslValue = 0.0;
			for (Leg leg : plan.getLegsOfMainMode()) {
				double weight = leg.getTravelTime() / plan.getLength();
				double overlap = calcPtOverlap(plan, leg, plans);
				if (overlap == 0.0) {
					overlap = 1.0;
				}
				pslValue += weight * 1.0/overlap;
			}
			plan.setPslValue(pslValue);
		}		
	}
	
	/**
	 * Calculate \sum_{j \in C_n} \delta_{aj} for pt plans
	 */
	private double calcPtOverlap(PSLPlanData plan, Leg leg, List<PSLPlanData> plans) {
		Route r = leg.getRoute();
		ExperimentalTransitRoute transitRoute = (ExperimentalTransitRoute) r;
		double overlap = 1.0;
		for (PSLPlanData otherPlan : plans) {
			if (otherPlan.getId() == plan.getId()) {
				continue; 
			}
			for (Leg otherLeg : otherPlan.getLegsOfMainMode()) {
				ExperimentalTransitRoute otherTransitRoute = (ExperimentalTransitRoute) otherLeg.getRoute();
				if (transitRoute.getRouteDescription().equals(otherTransitRoute.getRouteDescription())) {
					overlap++;
				}
			}
		}
		return overlap;
	}

	private void calcPtPlanLength(PSLPlanData plan){
		double length = 0.0;
		for (Leg l : plan.getLegsOfMainMode()){
			length += l.getTravelTime();
		}
		plan.setLength(length);
	}

}
