/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayPersonAgent.java
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

package playground.christoph.withinday.mobsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.ptproject.qsim.helpers.DefaultPersonDriverAgent;
import org.matsim.ptproject.qsim.interfaces.QSimI;

import playground.christoph.withinday.replanning.WithinDayReplanner;

public class WithinDayPersonAgent extends DefaultPersonDriverAgent {

	private static final Logger log = Logger.getLogger(WithinDayPersonAgent.class);

	private List<WithinDayReplanner> withinDayReplanner = new ArrayList<WithinDayReplanner>();
	private WithinDayQSim simulation;
	
	public WithinDayPersonAgent(Person p, QSimI simulation) {
		super(p, simulation);
		this.simulation = (WithinDayQSim) simulation;
	}

	/*
	 * Resets cached next Link. If a Person is in the Waiting Queue to leave a
	 * Link he/she may replan his/her Route so the cached Link would be wrong.
	 * 
	 * This should be more efficient that resetting it in chooseNextLink()
	 * because it can be called from the Replanning Module and isn't done for
	 * every Agent even it is not necessary.
	 */
	public void resetCachedNextLink() {
		super.cachedNextLinkId = null;
	}

	
	public void rescheduleCurrentActivity(double now) {
		this.simulation.rescheduleActivityEnd(now, this);
		
//		resetActivityDepartureTime(time);
//
//		simulation.getActivityEndsList().remove(this);
//		
//		/*
//		 * Check whether it is the last Activity. If true,
//		 * only remove it from the ActvityEndsList and decrease
//		 * the living counter.
//		 * Otherwise reschedule the Activity by adding it again
//		 * to the ActivityEndsList.
//		 */
//		Activity currentActivity = getCurrentActivity();
//		List<PlanElement> planElements = this.getPerson().getSelectedPlan().getPlanElements();
//		if (planElements.size() - 1 == planElements.indexOf(currentActivity)) {
//			// this is the last activity
//			simulation.getAgentCounter().decLiving();			
//		}
//		else {
//			simulation.getActivityEndsList().add(this);
//		}
	}
		
	/*
	 * Returns the currently performed Activity or the
	 * Activity that will be performed after the current
	 * Leg. 
	 */
	public Activity getCurrentActivity() {
		Activity currentActivity = null;
		
		// The Person is currently at an Activity and is going to leave it.
		// The Person's CurrentLeg should point to the leg that leads to that Activity...
		List<PlanElement> planElements = this.getPerson().getSelectedPlan().getPlanElements();
		
		Leg currentLeg = this.getCurrentLeg();
		
		// first Activity - there is no previous Leg
		if (currentLeg == null) {
			currentActivity = (Activity)planElements.get(0);
		}
		else {
			int index = planElements.indexOf(currentLeg);
			// If the Leg is part of the Person's Plan
			if (index >= 0) {
				currentActivity = (Activity)planElements.get(index + 1);
			}
		}
		
		if (currentActivity == null) {
			log.error("Could not find Activity!");
		}
		
		return currentActivity;
	}
	
	public boolean addWithinDayReplanner(WithinDayReplanner replanner) {
		return this.withinDayReplanner.add(replanner);
	}
	
	public boolean removeWithinDayReplanner(WithinDayReplanner replanner) {
		return this.withinDayReplanner.remove(replanner);
	}
	
	public List<WithinDayReplanner> getWithinDayReplanners() {
		return Collections.unmodifiableList(withinDayReplanner);
	}
}