/* *********************************************************************** *
 * project: org.matsim.*
 * NextLegReplanner.java
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

package playground.christoph.withinday.replanning.replanners;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PlanImpl;

import playground.christoph.withinday.replanning.replanners.NextLegReplanner;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.utils.EditRoutes;

/*
 * The NextLegReplanner can be used while an Agent is performing an Activity. The
 * Replanner creates a new Route from the current Activity to the next one in
 * the Agent's Plan.
 */

public class NextLegReplanner extends WithinDayDuringActivityReplanner {
		
	/*package*/ NextLegReplanner(Id id, Scenario scenario) {
		super(id, scenario);
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
		if (this.routeAlgo == null) return false;
		
		// If we don't have a valid personAgent
		if (personAgent == null) return false;
				
		Person person = personAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		if (selectedPlan == null) return false;
		
		Activity currentActivity;
		/*
		 *  Get the current PlanElement and check if it is an Activity
		 */
		PlanElement currentPlanElement = personAgent.getCurrentPlanElement();
		if (currentPlanElement instanceof Activity) {
			currentActivity = (Activity) currentPlanElement;
		} else return false;
		
		Leg nextLeg = selectedPlan.getNextLeg(currentActivity);
				
		// If it is not a car Leg we don't replan it.
//		if (!nextLeg.getMode().equals(TransportMode.car)) return false;
		
		// new Route for next Leg
		new EditRoutes().replanFutureLegRoute(selectedPlan, nextLeg, routeAlgo);

//		// create ReplanningEvent
//		QSim.getEvents().processEvent(new ExtendedAgentReplanEventImpl(time, person.getId(), (NetworkRouteWRefs)alternativeRoute, (NetworkRouteWRefs)originalRoute));
				
		return true;
	}

}