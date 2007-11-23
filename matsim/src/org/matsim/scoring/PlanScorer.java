/* *********************************************************************** *
 * project: org.matsim.*
 * PlanScorer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.scoring;

import org.matsim.plans.Leg;
import org.matsim.plans.Plan;



/**
 * @author dgrether
 *
 */
public class PlanScorer {
	
	private ScoringFunctionFactory factory;
	/**
	 * 
	 * @param factory
	 */
	public PlanScorer(final ScoringFunctionFactory factory) {
		this.factory = factory;
	}

	
	public double getScore(final Plan plan) {
		ScoringFunction function = this.factory.getNewScoringFunction(plan);
	  org.matsim.plans.Leg leg;
		for (int i = 1; i < plan.getActsLegs().size(); i++) {
			if (i % 2 == 1) {
				leg = (Leg) plan.getActsLegs().get(i);
				function.startLeg(leg.getDepTime(), leg);
				function.endLeg(leg.getArrTime());
			}
		}
		function.finish();
		return function.getScore();
	}
}
