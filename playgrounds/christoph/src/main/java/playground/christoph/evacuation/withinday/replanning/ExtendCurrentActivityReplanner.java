/* *********************************************************************** *
 * project: org.matsim.*
 * EndActivityAndEvacuateReplanner.java
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

package playground.christoph.evacuation.withinday.replanning;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayDuringActivityReplanner;

/*
 * Persons who use this replanner perform an activity at a "save" place
 * when the evacuation order is given. They extend their current activity
 * until the end of the day.
 * 
 */
public class ExtendCurrentActivityReplanner extends WithinDayDuringActivityReplanner {

	public ExtendCurrentActivityReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}
	
	@Override
	public boolean doReplanning(PersonAgent personAgent) {		
		
		// If we don't have a valid WithinDayPersonAgent
		if (personAgent == null) return false;
		
		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(personAgent instanceof WithinDayPersonAgent)) return false;
		else {
			withinDayPersonAgent = (WithinDayPersonAgent) personAgent;
		}
	
		PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		if (selectedPlan == null) return false;
		
		Activity currentActivity = withinDayPersonAgent.getCurrentActivity();
		
		// If we don't have a current Activity.
		if (currentActivity == null) return false;
		
		/*
		 * If it is already the last Activity that lasts until the end
		 * of the simulation the end time is Time.UNDEFINED_TIME. In that
		 * case we do not have to do any replanning anymore. 
		 */
		if (currentActivity.getEndTime() == Time.UNDEFINED_TIME) return true;
		
		/*
		 * Remove the endtime of the current Activity therefore the activity 
		 * will last until the end of the simulation.
		 */
		currentActivity.setEndTime(Time.UNDEFINED_TIME);
				
		// Remove all legs and activities after the current activity.
		int currentActivityIndex = selectedPlan.getActLegIndex(currentActivity);
		
		while (selectedPlan.getPlanElements().size() - 1 > currentActivityIndex) {
			selectedPlan.removeActivity(selectedPlan.getPlanElements().size() - 1);
		}
	
		/*
		 * Reschedule the currently performed Activity in the QSim - there
		 * the activityEndsList has to be updated.
		 */
		withinDayPersonAgent.rescheduleCurrentActivity(time);
		
//		PopulationFactory factory = scenario.getPopulation().getFactory();
//		
//		/*
//		 * Now we add a new Activity at the same facility as the current one.
//		 * We add no endtime therefore the activity will last until the end of
//		 * the simulation.
//		 */
//		Activity rescueWaitActivity = factory.createActivityFromLinkId("rescueWait", currentActivity.getLinkId());
//		((ActivityImpl)rescueWaitActivity).setFacilityId(currentActivity.getFacilityId());
//		((ActivityImpl)rescueWaitActivity).setCoord(currentActivity.getCoord());
//		rescueWaitActivity.setStartTime(currentActivity.getEndTime());
//		
//		// by default we use a car...
//		Leg legToRescueWait = factory.createLeg(TransportMode.car);
//			
//		// add new activity
//		selectedPlan.insertLegAct(selectedPlan.getActLegIndex(currentActivity) + 1, legToRescueWait, rescueWaitActivity);
//		
//		// calculate route for the legToRescueWait
//		new EditRoutes().replanFutureLegRoute(selectedPlan, legToRescueWait, planAlgorithm);
		
		return true;
	}

	@Override
	public ExtendCurrentActivityReplanner clone() {
		ExtendCurrentActivityReplanner clone = new ExtendCurrentActivityReplanner(this.id, this.scenario);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
	
}