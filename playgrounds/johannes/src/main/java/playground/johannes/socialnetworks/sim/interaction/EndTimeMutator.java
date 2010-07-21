/* *********************************************************************** *
 * project: org.matsim.*
 * EndTimeMutator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 * 
 */
public class EndTimeMutator {

	private static final double RANGE = 900.0;

	private Random random = new Random();

	public boolean mutatePlan(Plan plan) {
		int numActs = Math.max(0, (plan.getPlanElements().size() - 1) / 2);
		if (numActs > 1) {
			int idx = random.nextInt(numActs - 1);
			idx = idx * 2;
			if(idx == 0)
				return false;
			
			Activity act = (Activity) plan.getPlanElements().get(idx);
			if (!act.getType().contains("work") || !act.getType().contains("education")) {
				double epsilon;
				if (random.nextBoolean())
					epsilon = RANGE;
				else
					epsilon = -RANGE;

				double time = act.getEndTime() + epsilon;

				double minStartTime = 0;
				if(idx > 0)
					minStartTime = ((Activity)plan.getPlanElements().get(idx - 2)).getEndTime();
				
				double maxEndTime = 86400;
				if(idx < plan.getPlanElements().size() - 1)
					maxEndTime = ((Activity)plan.getPlanElements().get(idx + 2)).getEndTime()-1;
				
				double t = Math.max(time, minStartTime);
				act.setEndTime(Math.min(t, maxEndTime));
				
				Leg leg = (Leg) plan.getPlanElements().get(idx + 1);
				leg.setDepartureTime(time);
				
				return true;
			}
		}
		
		return false;
	}
}
