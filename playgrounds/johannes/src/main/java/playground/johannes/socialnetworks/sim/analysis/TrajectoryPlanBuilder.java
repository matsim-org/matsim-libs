/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryPlanBuilder.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class TrajectoryPlanBuilder {

	public Set<Trajectory> buildTrajectory(Set<Plan> plans) {
		Set<Trajectory> trajectories = new HashSet<Trajectory>();
		for(Plan plan : plans) {
			if(plan.getPlanElements().size() % 2 == 0) {
				System.out.println("Invalid plan.");
			} else {
			Trajectory t = new Trajectory();
			for(int i = 0; i < plan.getPlanElements().size(); i++) {
				if(i % 2 == 0) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					double endTime = act.getEndTime();
					if(Double.isInfinite(endTime)) {
						endTime = 86400;
						if(i > 0) {
							endTime = Math.max(t.getTransitions().get(i), 86400);
						}
					}
					t.addElement(act, endTime);
				} else {
					Leg leg = (Leg) plan.getPlanElements().get(i);
					Activity act = (Activity) plan.getPlanElements().get(i + 1);
					double endTime = act.getStartTime();
					if(Double.isInfinite(endTime) || endTime == 0)
						endTime = leg.getDepartureTime() + leg.getTravelTime();
					
					t.addElement(leg, endTime);
				}
			}
			trajectories.add(t);
			}
		}
		
		return trajectories;
	}
}
