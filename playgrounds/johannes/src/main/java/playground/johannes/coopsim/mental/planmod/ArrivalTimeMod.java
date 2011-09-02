/* *********************************************************************** *
 * project: org.matsim.*
 * ArrivalTimeMod.java
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
public class ArrivalTimeMod implements PlanModifier {

	private int planIndex;
	
	private double desiredArrivalTime;
	
	public void setPlanIndex(int planIndex) {
		this.planIndex = planIndex;
	}

	public void setDesiredArrivalTime(double desiredArrivalTime) {
		this.desiredArrivalTime = desiredArrivalTime;
	}

	@Override
	public void apply(Plan plan) {
		if(planIndex < 2)
			throw new IllegalArgumentException("Plan index must not be less than 2.");

		Activity act = (Activity) plan.getPlanElements().get(planIndex - 2);
		Leg leg = (Leg) plan.getPlanElements().get(planIndex - 1);
		
		act.setEndTime(desiredArrivalTime - leg.getTravelTime());
	}

}
