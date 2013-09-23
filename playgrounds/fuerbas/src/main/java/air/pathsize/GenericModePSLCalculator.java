/* *********************************************************************** *
 * project: org.matsim.*
 * GenericModePSLCalculator
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.GenericRoute;


/**
 * @author dgrether
 *
 */
public class GenericModePSLCalculator implements PSLCalculator {

//	private static final Logger log = Logger.getLogger(GenericModePSLCalculator.class);

	public void calculatePSLValues(List<PSLPlanData> plans) {
		for (PSLPlanData plan : plans) {
			this.calcGenericPlanLength(plan);
		}
		for (PSLPlanData plan : plans) {
			double pslValue = 0.0;
			for (Leg leg : plan.getLegsOfMainMode()) {
				double weight = leg.getTravelTime() / plan.getLength();
				double overlap = calcGenericOverlap(plan, leg, plans);
				if (overlap == 0.0) {
					overlap = 1.0;
				}
				pslValue += weight * 1.0/overlap;
			}
			plan.setPslValue(pslValue);
		}		
	}

	/**
	 * Calculate \sum_{j \in C_n} \delta_{aj} for plans with "generic" routes
	 */
	private double calcGenericOverlap(PSLPlanData plan, Leg leg, List<PSLPlanData> plans) {
		GenericRoute genericRoute = (GenericRoute) leg.getRoute();
		double overlap = 1.0;
		for (PSLPlanData otherPlan : plans) {
			if (otherPlan.getId() == plan.getId()) {
				continue; 
			}
			for (Leg otherLeg : otherPlan.getLegsOfMainMode()) {
				GenericRoute otherGenericRoute = (GenericRoute) otherLeg.getRoute();
				if (genericRoute.getStartLinkId().equals(otherGenericRoute.getStartLinkId())
						&& genericRoute.getEndLinkId().equals(otherGenericRoute.getEndLinkId())
						&& genericRoute.getTravelTime() == otherGenericRoute.getTravelTime()) {
					overlap++;
				}
			}
		}
		return overlap;
	}
	
	private void calcGenericPlanLength(PSLPlanData plan){
		double length = 0.0;
		for (Leg l : plan.getLegsOfMainMode()){
			length += l.getTravelTime();
		}
		plan.setLength(length);
	}

	
}
