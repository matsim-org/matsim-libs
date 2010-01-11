/* *********************************************************************** *
 * project: org.matsim.*
 * LeaveLinkReplanner.java
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

package playground.christoph.events.algorithms;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.mobsim.WithinDayPersonAgent;

/*
 * As the ActEndReplanner the LeaveLinkReplanner should be called when a
 * LeaveLinkEvent is thrown. At the moment this does not work because
 * such an Event is thrown AFTER a Person left a link and entered a new link.
 * 
 * The current solution is to call the method by hand in the MyQueueNode class 
 * when a Person is at the End of a Link but before leaving the link.
 * 
 * MATSim Routers use Plan as Input Data. To be able to use them, we have to create
 * a new Plan from the current Position to the location of the next Activity.
 * 
 * This Replanner is called, if a person is somewhere on a Route between two Activities.
 * First the current Route is splitted into two parts - the already passed links and
 * the ones which are still to go. 
 * Next a new Plan is created with an Activity at the current Position and an Endposition
 * that is equal to the one from the original plan.
 * This Plan is handed over to the Router and finally the new route is merged with the
 * Links that already have been passed by the Person.
 */

public class LeaveLinkReplanner {

	protected ActivityImpl nextAct;
	protected LegImpl leg;
	protected double time;
	protected PersonAgent personAgent;
	protected PersonImpl person;
	protected PlanImpl plan;
	protected QueueVehicle vehicle;
	
	protected PlanAlgorithm replanner; 
	
	private static final Logger log = Logger.getLogger(LeaveLinkReplanner.class);
	
	public LeaveLinkReplanner(QueueVehicle vehicle, double time, PlanAlgorithm replanner)
	{
		this.time = time;
		this.vehicle = vehicle;
		this.personAgent = (PersonAgent) vehicle.getDriver();
		this.person = (PersonImpl) vehicle.getDriver().getPerson();
		this.replanner = replanner;
		
		init();
	}
	
	public LeaveLinkReplanner(QueueVehicle vehicle, double time)
	{
		this.time = time;
		this.vehicle = vehicle;
		this.personAgent = (PersonAgent) vehicle.getDriver();
		this.person = (PersonImpl) vehicle.getDriver().getPerson();

		replanner = (PlanAlgorithm)person.getCustomAttributes().get("Replanner");
		if (replanner == null) log.error("No Replanner found in Person!");
		
		init();
		
	}	// Replanner(...)

	protected void init()
	{
		Plan plan = person.getSelectedPlan();

		leg = (LegImpl) personAgent.getCurrentLeg();

		nextAct = (ActivityImpl)((PlanImpl) plan).getNextActivity(leg);	
		
		// if there is a next Activity...
		if(nextAct != null)
		{
//			log.info("Id: " + person.getId());
			routing();
		}
		else
		{
			log.error("An agents next activity is null - this should not happen! Id: " + person.getId());
		}
	}
	
	/*
	 * Replan Route every time the End of a Link is reached.
	 *
	 * Idea:
	 * - create a new Activity at the current Location
	 * - create a new Route from the current Location to the Destination
	 * - merge already passed parts of the current Route with the new created Route
	 */
	protected void routing()
	{	
		// If it is not a car Leg we don't replan it.
		if (!leg.getMode().equals(TransportMode.car)) return;
		
		/*
		 * Get the index and the currently next Node on the route.
		 * Entries with a lower index have already been visited!
		 */ 
		int currentNodeIndex = this.personAgent.getCurrentNodeIndex();
		NetworkRouteWRefs route = (NetworkRouteWRefs) this.leg.getRoute();
		
		// set VehicleId - seems that it currently null?
		route.setVehicleId(this.vehicle.getId());
		
		// create dummy data for the "new" activities
		String type = "w";
		// This would be the "correct" Type - but it is slower and is not necessary
		//String type = this.plan.getPreviousActivity(leg).getType();
		
		ActivityImpl newFromAct = new ActivityImpl(type, this.vehicle.getCurrentLink().getToNode().getCoord(), (LinkImpl) this.vehicle.getCurrentLink());

		newFromAct.setStartTime(time);
		newFromAct.setEndTime(time);

		NodeNetworkRouteImpl subRoute = new NodeNetworkRouteImpl(this.vehicle.getCurrentLink(), route.getEndLink());
		/*
		 * This can be used for debugging purposes. It copies the current
		 * non-driven Parts of the Route to the new SubRoute which is
		 * replanned afterwards. By doing this we can skip the replanning
		 * and still have a correct SubRoute. In normal usage we don't need
		 * this because the SubRoute is created new anyway.
		 */
		//subRoute.setNodes(this.vehicle.getCurrentLink(), route.getNodes().subList(currentNodeIndex - 1, route.getNodes().size()), route.getEndLink());
		
		// put the new route in a new leg
		LegImpl newLeg = new LegImpl(leg.getMode());
		newLeg.setDepartureTime(leg.getDepartureTime());
		newLeg.setTravelTime(leg.getTravelTime());
		newLeg.setArrivalTime(leg.getArrivalTime());
		newLeg.setRoute(subRoute);
				
		// create new plan and select it
		PlanImpl newPlan = new PlanImpl(person);
			
		// here we are at the moment
		newPlan.addActivity(newFromAct);
		
		// Route from the current position to the next destination.
		newPlan.addLeg(newLeg);
		
		// next Activity
		newPlan.addActivity(nextAct);
		
		replanner.run(newPlan);	

		// get new calculated Route
		NetworkRouteWRefs newRoute = (NetworkRouteWRefs) newLeg.getRoute();
		
		// use VehicleId from existing Route
		newRoute.setVehicleId(this.vehicle.getId());
		
		// get Nodes from the current Route
		List<Node> nodesBuffer = route.getNodes();
		
		// remove Nodes after the current Position in the Route
		nodesBuffer.subList(currentNodeIndex - 1, nodesBuffer.size()).clear();
				
		// Merge already driven parts of the Route with the new routed parts.
		// The changes will be made in the original list, so they will be directly in the Route.
		nodesBuffer.addAll(newRoute.getNodes());
		
		// finally reset the cached next Link of the PersonAgent - it may have changed!
		((WithinDayPersonAgent)this.personAgent).ResetCachedNextLink();
		
		// create ReplanningEvent
		QueueSimulation.getEvents().processEvent(new ExtendedAgentReplanEventImpl(time, person.getId(), newRoute, route));
	}
		
	/*
	 * Checks, whether a new created Route is valid or not.
	 */
	protected boolean checkRoute(NetworkRouteWRefs route)
	{
		List<Node> nodes = route.getNodes();
		
		if(nodes.size() == 0) return true;
	
		Node currentNode;
		Node nextNode;
		
		for (int i = 1; i < nodes.size() - 1; i++)
		{
			currentNode = nodes.get(i);
			nextNode = nodes.get(i + 1);

			boolean foundLink = false;
			
			for (Link link : currentNode.getOutLinks().values())
			{
				if (link.getToNode() == nextNode)
				{
					foundLink = true;
					break;
				}
			}
			
			if (!foundLink) return false;
		}
		
		return true;
	}
}