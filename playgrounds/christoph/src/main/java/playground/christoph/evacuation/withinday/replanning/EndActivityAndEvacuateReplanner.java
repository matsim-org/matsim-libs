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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.utils.EditRoutes;

/*
 * Needs to be registered as SimulationListener!!! 
 */
public class EndActivityAndEvacuateReplanner extends WithinDayDuringActivityReplanner {
	
	public EndActivityAndEvacuateReplanner(Id id, Scenario scenario) {
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
		 * If the agent is already at the end of his scheduled plan then
		 * the simulation counter has been decreased by one. We re-enable the
		 * agent so we have to increase the counter again.
		 */
		if (currentActivity.getEndTime() == Time.UNDEFINED_TIME) this.agentCounter.incLiving();
		
		// Set the end time of the current activity to the current time.
		currentActivity.setEndTime(this.time);
		
		// Remove all legs and activities after the current activity.
		int currentActivityIndex = selectedPlan.getActLegIndex(currentActivity);
		
		while (selectedPlan.getPlanElements().size() - 1 > currentActivityIndex) {
			selectedPlan.removeActivity(selectedPlan.getPlanElements().size() - 1);
		}
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		/*
		 * Now we add a new Activity at the rescue facility.
		 * We add no endtime therefore the activity will last until the end of
		 * the simulation.
		 */
		Activity rescueActivity = factory.createActivityFromLinkId("rescue", scenario.createId("rescueLink"));
		((ActivityImpl)rescueActivity).setFacilityId(scenario.createId("rescueFacility"));
		
		Coord rescueCoord = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(scenario.createId("rescueFacility")).getCoord();
		((ActivityImpl)rescueActivity).setCoord(rescueCoord);
		
		// by default we use a car...
		Leg legToRescue = factory.createLeg(TransportMode.car);
			
		// add new activity
		selectedPlan.insertLegAct(selectedPlan.getActLegIndex(currentActivity) + 1, legToRescue, rescueActivity);
			
		// calculate route for the leg to the rescue facility
		new EditRoutes().replanFutureLegRoute(selectedPlan, legToRescue, planAlgorithm);

		/*
		 * Reschedule the currently performed Activity in the QSim - there
		 * the activityEndsList has to be updated.
		 */
		withinDayPersonAgent.rescheduleCurrentActivity(time);
		
		return true;
	}

	@Override
	public EndActivityAndEvacuateReplanner clone() {
		EndActivityAndEvacuateReplanner clone = new EndActivityAndEvacuateReplanner(this.id, this.scenario);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
	
}
