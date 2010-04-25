/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatXPlanScorer.java
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
package playground.mfeil;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * @author Matthias Feil
 * Extends the standard PlanScorer to enable scoring of plans that do not have fully documented legs or acts. 
 * acts: Works also for acts that have only duration or endTime (first activity only)
 * legs: Works also for legs that have only travTime
 * Necessary e.g., for equil scenario.
 */
public class PlanomatXPlanScorer extends PlanScorer {

	/**
	 * @param factory
	 */
	private ScoringFunctionFactory factory;
	
	public PlanomatXPlanScorer(ScoringFunctionFactory factory) {
		super(factory);
		this.factory = factory;
	}
	
	@Override
	public double getScore(final Plan plan) {
		ScoringFunction function = this.factory.createNewScoringFunction(plan);
		org.matsim.core.population.LegImpl leg;
		org.matsim.core.population.ActivityImpl act;
		for (int i = 1; i < plan.getPlanElements().size(); i++) {
			if (i % 2 != 0) {
				leg = (LegImpl) plan.getPlanElements().get(i);
				double depTime = 0;
				double arrTime = 0;
				for (int x = 0;x<i;x++){
					if (x % 2 == 0){
						ActivityImpl actHelp = (ActivityImpl) (plan.getPlanElements().get(x));
						if (actHelp.getDuration()>=0){
								depTime = actHelp.getDuration() + depTime;
						}
						else if (actHelp.getEndTime()>=0){
							depTime = actHelp.getEndTime() + depTime;
						}
						else {
							depTime = 24*3600;
						}
					}
				}
				
				act = ((PlanImpl) plan).getPreviousActivity(leg);
				function.endActivity(act.getEndTime());
				function.startLeg(depTime, leg);
				
				if (leg.getTravelTime()>=0){
					arrTime = depTime + leg.getTravelTime();
				}
				else{
					arrTime = depTime;
				}
				
				function.endLeg(arrTime);
				act = ((PlanImpl) plan).getNextActivity(leg);
				function.startActivity(act.getStartTime(), act);
			}
		}
		function.finish();
		return function.getScore();
	}
}
