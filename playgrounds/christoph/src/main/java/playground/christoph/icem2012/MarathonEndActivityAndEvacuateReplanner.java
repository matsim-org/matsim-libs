/* *********************************************************************** *
 * project: org.matsim.*
 * MarathonEndActivityAndEvacuateReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;

import playground.christoph.evacuation.withinday.replanning.replanners.old.EndActivityAndEvacuateReplanner;

/*
 * Switches the mode of walk evacuation legs to walk2d.
 */
public class MarathonEndActivityAndEvacuateReplanner extends WithinDayDuringActivityReplanner {
		
	private final EndActivityAndEvacuateReplanner replanner;
	
	/*package*/ MarathonEndActivityAndEvacuateReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface, 
			EndActivityAndEvacuateReplanner replanner) {
		super(id, scenario, internalInterface);
		this.replanner = replanner;
	}
		
	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {		
		
		/*
		 * This is ugly because we perform it for every agent instead of once per time step.
		 * However, setTime(...) is final, therefore we cannot override it.
		 */
		this.replanner.setTime(this.time);
		
		boolean replanned = replanner.doReplanning(withinDayAgent);
		
		if (!replanned) return replanned;

		Plan plan = ((PlanAgent) withinDayAgent).getCurrentPlan(); 
		
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			PlanElement planElement = plan.getPlanElements().get(i);
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (activity.getType().equals("rescue")) {
					Leg legToRescue = (Leg) plan.getPlanElements().get(i - 1);

					// use walk2d instead of walk
					if (legToRescue.getMode().equals(TransportMode.walk)) {
						legToRescue.setMode("walk2d");
						
						Activity currentActivity = (Activity) ((PlanAgent) withinDayAgent).getCurrentPlanElement();
						/*
						 * Adapt the activity's coordinate to a location close to the end node of the link
						 * where it is performed at.
						 */
						Coord coord = scenario.getNetwork().getLinks().get(currentActivity.getLinkId()).getToNode().getCoord();
						XORShiftRandom xor = new XORShiftRandom((long) withinDayAgent.getId().hashCode());
						double x = coord.getX() + xor.nextDouble() - 1.0;	// +/- 0.5m
						double y = coord.getY() + xor.nextDouble() - 1.0;	// +/- 0.5m
						((ActivityImpl) currentActivity).setCoord(new Coord(x, y));
					}
				}
			}
		}
		
		return true;
	}
}
