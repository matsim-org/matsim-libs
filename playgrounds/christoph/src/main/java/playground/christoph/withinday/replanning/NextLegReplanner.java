/* *********************************************************************** *
 * project: org.matsim.*
 * NextLegReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.utils.EditRoutes;

/*
 * The NextLegReplanner can be used while an Agent is performing an Activity. The
 * Replanner creates a new Route from the current Activity to the next one in
 * the Agent's Plan.
 */

public class NextLegReplanner extends WithinDayDuringActivityReplanner {
		
	private static final Logger log = Logger.getLogger(NextLegReplanner.class);
	private EventsManager events;

	public NextLegReplanner(Id id, Scenario scenario, EventsManager events) {
		super(id, scenario);
		this.events = events;
	}
	
	/*
	 * Idea:
	 * MATSim Routers create Routes for complete plans.
	 * We just want to replan the Route from one Activity to another one, so we create
	 * a new Plan that contains only this Route. This Plan is replanned by sending it
	 * to the Router.
	 *
	 * Attention! The Replanner is started when the Activity of a Person ends and the Vehicle 
	 * is added to the Waiting List of its QueueLink. That means that a Person replans 
	 * his Route at time A but enters the QueueLink at time B.
	 * A short example: If all Persons of a network end their Activities at the same time 
	 * and have the same Start- and Endpoints of their Routes they will all use the same
	 * route (if they use the same router). If they would replan their routes when they really
	 * Enter the QueueLink this would not happen because the enter times would be different
	 * due to the limited number of possible vehicles on a link at a time. An implementation
	 * of such a functionality would be a problem due to the structure of MATSim...
	 */
	@Override
	public boolean doReplanning(PersonAgent personAgent) {		
		// If we don't have a valid Replanner.
		if (this.planAlgorithm == null) return false;
		
		// If we don't have a valid WithinDayPersonAgent
		if (personAgent == null) return false;
		
		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(personAgent instanceof WithinDayPersonAgent)) return false;
		else
		{
			withinDayPersonAgent = (WithinDayPersonAgent) personAgent;
		}
		
		PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		if (selectedPlan == null) return false;
		
		Activity currentActivity = withinDayPersonAgent.getCurrentActivity();
		
		// If we don't have a current Activity
		if (currentActivity == null) return false;
		
		Leg nextLeg = selectedPlan.getNextLeg(currentActivity);
//		Activity nextActivity = selectedPlan.getNextActivity(nextLeg);	
				
		// If it is not a car Leg we don't replan it.
//		if (!nextLeg.getMode().equals(TransportMode.car)) return false;
		
		// new Route for next Leg
		new EditRoutes().replanFutureLegRoute(selectedPlan, nextLeg, planAlgorithm);

//		// create ReplanningEvent
//		QSim.getEvents().processEvent(new ExtendedAgentReplanEventImpl(time, person.getId(), (NetworkRouteWRefs)alternativeRoute, (NetworkRouteWRefs)originalRoute));
				
		return true;
	}
	
	@Override
	public NextLegReplanner clone() {
		NextLegReplanner clone = new NextLegReplanner(this.id, this.scenario, this.events);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}