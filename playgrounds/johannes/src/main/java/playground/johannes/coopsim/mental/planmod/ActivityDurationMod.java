/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityDurationMod.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.planmod;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class ActivityDurationMod implements PlanModifier {

	private int planIndex;
	
	private double desiredDuration;
	
	public void setPlanIndex(int planIndex) {
		this.planIndex = planIndex;
	}

	public void setDesiredDuration(double desiredDuration) {
		this.desiredDuration = desiredDuration;
	}

	@Override
	public void apply(Plan plan) {
		double start = 0;
		if(planIndex > 1) {
			Activity prev = (Activity) plan.getPlanElements().get(planIndex - 2);
			Leg leg = (Leg) plan.getPlanElements().get(planIndex - 1);
			
			start = prev.getEndTime() + leg.getTravelTime();
		}
		
		Activity act = (Activity) plan.getPlanElements().get(planIndex);
		act.setEndTime(start + desiredDuration);
	}

}
