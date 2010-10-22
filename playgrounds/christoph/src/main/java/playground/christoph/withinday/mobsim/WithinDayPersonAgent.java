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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.ptproject.qsim.helpers.DefaultPersonDriverAgent;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

import playground.christoph.withinday.replanning.WithinDayReplanner;

public class WithinDayPersonAgent extends DefaultPersonDriverAgent {
	private static final Logger log = Logger.getLogger("dummy");

	private List<WithinDayReplanner> withinDayReplanner = new ArrayList<WithinDayReplanner>();
	private Mobsim simulation;
	
	public WithinDayPersonAgent(Person p, Mobsim simulation) {
		super(p, simulation);
		this.simulation = simulation;
	}

	/*
	 * Resets cached next Link. If a Person is in the Waiting Queue to leave a
	 * Link he/she may replan his/her Route so the cached Link would be wrong.
	 * 
	 * This should be more efficient that resetting it in chooseNextLink()
	 * because it can be called from the Replanning Module and isn't done for
	 * every Agent even it is not necessary.
	 */
	// yyyy never used. Instead resetCaches() could be used. christoph, oct'10
//	public void resetCachedNextLink() {
//		super.cachedNextLinkId = null;
//	}
	
	public void rescheduleCurrentActivity() {
//		this.simulation.rescheduleActivityEnd(this);
		
		/*
		 * - Remove Agent from the ActivityEndsList.
		 * - Recalculate the DepartureTime from the currently performed Activity
		 *   (>= now). The EndTime of the Activity must have been already adapted.
		 * - Add Agent to the ActivityEndsList. This will ensure that the Agent
		 *   is placed at the correct place because the List is a PriorityQueue ordered
		 *   by the DepartureTimes.
		 */
//		public void rescheduleActivityEnd(WithinDayPersonAgent withinDayPersonAgent) {
//			boolean removed = this.getActivityEndsList().remove(withinDayPersonAgent);
			boolean found = this.simulation.getActivityEndsList().contains( this ) ;

			// If the agent is not in the activityEndsList return without doing anything else.
//			if (!removed) return;
			if ( !found ) return ;
			
			double oldTime = this.getDepartureTime() ;
			
			PlanElement planElement = this.getCurrentPlanElement();
			if (planElement instanceof Activity) {
				ActivityImpl act = (ActivityImpl) planElement;
				
				this.calculateDepartureTime(act);
			} 
			// yyyy can this situation really occur (Agent is in the ActivityEndsList but not performing an Activity)? christoph, oct'10
			else log.warn("Cannot reset Activity Departure Time - Agent is currently performing a Leg. " + this.getPerson().getId());
			
			/*
			 * Check whether it is the last Activity. If true, only remove it 
			 * from the ActivityEndsList and decrease the living counter.
			 * Otherwise reschedule the Activity by adding it again to the ActivityEndsList.
			 */
//			Activity currentActivity = withinDayPersonAgent.getCurrentActivity();		
//			List<PlanElement> planElements = withinDayPersonAgent.getPerson().getSelectedPlan().getPlanElements();
//			if (planElements.size() - 1 == planElements.indexOf(currentActivity)) {
//				// This is the last activity, therefore remove the agent from the simulation
//				this.getAgentCounter().decLiving();
//			}
//			else {
//				this.getActivityEndsList().add(withinDayPersonAgent);
//			}
			this.simulation.rescheduleActivityEnd(this, oldTime, this.getDepartureTime() ) ;

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
		
	/**
	 * Returns the currently performed Activity or the Activity that 
	 * will be performed after the current Leg.
	 * @return the currently performed Activity or null if a Leg is performed
	 */
	public Activity getCurrentActivity() {

		// yyyy simplified the code. code that call this method now has to be able to handle null as return value. christoph, oct'10
		PlanElement planElement = super.getCurrentPlanElement();
		if (planElement instanceof Activity) return (Activity)planElement;
		else return null;
		
		// yyyy given that there is getCurrentPlanElement(), does this method have to be so complicated?  kai, oct'10

		// yyyy what if leg and activity are not alternating (as is the current matsim design specification)?  kai, oct'10
			
//		Activity currentActivity = null;
//		
//		// The Person is currently at an Activity and is going to leave it.
//		// The Person's CurrentLeg should point to the leg that leads to that Activity...
//		List<PlanElement> planElements = this.getPerson().getSelectedPlan().getPlanElements();
//		
//		Leg currentLeg = this.getCurrentLeg();
//		
//		// first Activity - there is no previous Leg
//		if (currentLeg == null) {
//			currentActivity = (Activity)planElements.get(0);
//		}
//		else {
//			int index = planElements.indexOf(currentLeg);
//			// If the Leg is part of the Person's Plan
//			if (index >= 0) {
//				currentActivity = (Activity)planElements.get(index + 1);
//			}
//		}
//		
//		if (currentActivity == null) {
//			log.error("Could not find Activity!");
//		}
//		
//		return currentActivity;
	}
	
	
	// If I am understanding this correctly, the replanners that are added in the following are not actively used as instances,
	// but they are used in order to identify those agents that possess those replanners.  And only those are submitted to 
	// the replanning process.
	
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