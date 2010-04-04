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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.ptproject.qsim.QSim;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.network.util.SubNetworkTools;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;

/*
 * The NextLegReplanner can be used while an Agent is performing an Activity. The
 * Replanner creates a new Route from the current Activity to the next one in
 * the Agent's Plan.
 */

public class NextLegReplanner extends WithinDayDuringActivityReplanner{
		
	private static final Logger log = Logger.getLogger(NextLegReplanner.class);

	public NextLegReplanner(Id id)
	{
		super(id);
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
	public boolean doReplanning(DriverAgent driverAgent)
	{	
		// If we don't have a valid Replanner.
		if (this.planAlgorithm == null) return false;
		
		// If we don't have a valid WithinDayPersonAgent
		if (driverAgent == null) return false;
		
		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(driverAgent instanceof WithinDayPersonAgent)) return false;
		else
		{
			withinDayPersonAgent = (WithinDayPersonAgent) driverAgent;
		}
		
		PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		if (selectedPlan == null) return false;
		
		Activity currentActivity = withinDayPersonAgent.getCurrentActivity();
		
		// If we don't have a current Activity
		if (currentActivity == null) return false;
		
		Leg nextLeg = selectedPlan.getNextLeg(currentActivity);
		Activity nextActivity = selectedPlan.getNextActivity(nextLeg);	
				
		// If it is not a car Leg we don't replan it.
		if (!nextLeg.getMode().equals(TransportMode.car)) return false;
		
		// Replace the EndTime with the current time - do we really need this?
//		fromAct.setEndTime(time);
		
		// Create new Plan and select it.
		// This Plan contains only the just ended and the next Activity.
		// -> That's the only Part of the Route that we want to replan.
		PlanImpl newPlan = new PlanImpl(person);
		person.addPlan(newPlan);
		person.setSelectedPlan(newPlan);
		
		// Here we are at the moment.
		newPlan.addActivity(currentActivity);
		
		Route originalRoute = nextLeg.getRoute();
			
		// Current Route between fromAct and toAct - this Route shall be replanned.
		newPlan.addLeg(nextLeg);
		
		// We still want to go there...
		newPlan.addActivity(nextActivity);
							
		// By doing the replanning the "betweenLeg" is replanned, so the changes are
		// included in the previously selected plan, too!
		planAlgorithm.run(newPlan);
		
		new SubNetworkTools().resetSubNetwork(person);
		
		// reactivate previously selected, replanned plan
		person.setSelectedPlan(selectedPlan);
		
		// remove previously added new Plan
		person.removePlan(newPlan);
		
		Route alternativeRoute = nextLeg.getRoute();

		// set VehicleId in original Route as well as in the alternative Route
		((NetworkRoute)originalRoute).setVehicleId(withinDayPersonAgent.getVehicle().getId());
		((NetworkRoute)alternativeRoute).setVehicleId(withinDayPersonAgent.getVehicle().getId());
		
//		if (alternativeRoute.getDistance() != originalRoute.getDistance())
//		{
//			log.info("Changed Route length! Id:" + this.person.getId() + " " + alternativeRoute.getDistance() + " vs. " + originalRoute.getDistance());
//		}
		
		// create ReplanningEvent
		QSim.getEvents().processEvent(new ExtendedAgentReplanEventImpl(time, person.getId(), (NetworkRoute)alternativeRoute, (NetworkRoute)originalRoute));
				
		return true;
	}
	
	public NextLegReplanner clone()
	{
		NextLegReplanner clone = new NextLegReplanner(this.id);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}