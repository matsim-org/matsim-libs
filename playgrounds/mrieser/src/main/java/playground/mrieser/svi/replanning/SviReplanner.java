/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.replanning;

import java.util.Random;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.utils.misc.Time;

/**
 * @author mrieser
 */
public class SviReplanner implements PersonAlgorithm {

	private final Random random;
	private final double range;
	
	public SviReplanner(final Random random, final double range) {
		this.random = random;
		this.range = range;
	}
	
	@Override
	public void run(final Person person) {
		Plan plan = person.getSelectedPlan();
		
		// 1st plan, set everything to car-mode
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				((Leg) pe).setMode(TransportMode.car);
				((Leg) pe).setRoute(null);
			}
		}
		
		// 2nd plan, set everything to pt-mode
		Plan p2 = PopulationUtils.createPlan();
		person.addPlan(p2);
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				// reuse the activity, not the nice way, but it works
				p2.addActivity((Activity) pe);
			}
			if (pe instanceof Leg) {
				// copy leg
				Leg leg = PopulationUtils.createLeg(TransportMode.pt);
				leg.setDepartureTime(leg.getDepartureTime());
				p2.addLeg(leg);
			}
		} 

		// 3rd plan, set everything to car-mode and modify times
		Plan p3 = PopulationUtils.createPlan();
		person.addPlan(p3);
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity a = (Activity) pe;
				// copy activity
				Activity act = PopulationUtils.createActivityFromCoordAndLinkId(a.getType(), a.getCoord(), a.getLinkId());
				if (a.getEndTime() != Time.UNDEFINED_TIME) {
					act.setEndTime(a.getEndTime() + getRandomDifference());
				} else if (a.getMaximumDuration() != Time.UNDEFINED_TIME) {
					act.setMaximumDuration(a.getMaximumDuration() + getRandomDifference());
				}
				
				p3.addActivity(act);
			}
			if (pe instanceof Leg) {
				// copy leg
				Leg leg = PopulationUtils.createLeg(TransportMode.car);
				leg.setDepartureTime(Time.UNDEFINED_TIME);
				p3.addLeg(leg);
			}
		} 
		
		// 4th plan, set everything to pt-mode and modify times
		Plan p4 = PopulationUtils.createPlan();
		person.addPlan(p4);
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity a = (Activity) pe;
				// copy activity
				Activity act = PopulationUtils.createActivityFromCoordAndLinkId(a.getType(), a.getCoord(), a.getLinkId());
				if (a.getEndTime() != Time.UNDEFINED_TIME) {
					act.setEndTime(a.getEndTime() + getRandomDifference());
				} else if (a.getMaximumDuration() != Time.UNDEFINED_TIME) {
					act.setMaximumDuration(a.getMaximumDuration() + getRandomDifference());
				}
				
				p4.addActivity(act);
			}
			if (pe instanceof Leg) {
				// copy leg
				Leg leg = PopulationUtils.createLeg(TransportMode.pt);
				leg.setDepartureTime(Time.UNDEFINED_TIME);
				p4.addLeg(leg);
			}
		} 
		
	}

	private double getRandomDifference() {
		return ((this.random.nextDouble() * 2.0) - 1.0) * this.range;
	}
	
}
