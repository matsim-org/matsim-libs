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

package org.matsim.core.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

/**
 * @author dgrether
 */
public class PlanScorer {

	private ScoringFunctionFactory factory;

	public PlanScorer(final ScoringFunctionFactory factory) {
		this.factory = factory;
	}

	public double getScore(final Plan plan) {
		ScoringFunction function = this.factory.getNewScoringFunction((PlanImpl) plan);
		boolean firstActivityDone = false;

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if (firstActivityDone) {
					function.startActivity(act.getStartTime(), act);
					firstActivityDone = true;
				}
				function.endActivity(act.getEndTime());					
			} else if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				function.startLeg(leg.getDepartureTime(), leg);
				function.endLeg(leg.getArrivalTime());
			}
		}
		function.finish();
		return function.getScore();
	}
}
