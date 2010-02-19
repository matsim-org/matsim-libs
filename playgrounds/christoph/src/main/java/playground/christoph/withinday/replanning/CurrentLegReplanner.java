/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegReplanner.java
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

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QVehicle;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;

/*
 * The CurrentLegReplanner can be used while an Agent travels from
 * one Activity to another one.
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

public class CurrentLegReplanner extends WithinDayDuringLegReplanner{

	private Network network;

	private static final Logger log = Logger.getLogger(CurrentLegReplanner.class);

	public CurrentLegReplanner(Id id, Network network)
	{
		super(id);
		this.network = network;
	}

	/*
	 * Replan Route every time the End of a Link is reached.
	 *
	 * Idea:
	 * - create a new Activity at the current Location
	 * - create a new Route from the current Location to the Destination
	 * - merge already passed parts of the current Route with the new created Route
	 */
	@Override
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

		Leg currentLeg = driverAgent.getCurrentLeg();
		Activity nextActivity = selectedPlan.getNextActivity(currentLeg);

		// If it is not a car Leg we don't replan it.
		if (!currentLeg.getMode().equals(TransportMode.car)) return false;

		/*
		 * Get the index and the currently next Node on the route.
		 * Entries with a lower index have already been visited!
		 */
		int currentNodeIndex = withinDayPersonAgent.getCurrentNodeIndex();
		NetworkRoute route = (NetworkRoute) currentLeg.getRoute();

		QVehicle vehicle = withinDayPersonAgent.getVehicle();

		/*
		 * If the Vehicle is already on the destination Link we don't need any
		 * further Replanning.
		 */
		if (vehicle.getCurrentLink().getId().equals(route.getEndLinkId())) return true;

		// set VehicleId - seems that it currently null?
		route.setVehicleId(vehicle.getId());

		// create dummy data for the "new" activities
		String type = "w";
		// This would be the "correct" Type - but it is slower and is not necessary
		//String type = this.plan.getPreviousActivity(leg).getType();

		ActivityImpl newFromAct = new ActivityImpl(type, vehicle.getCurrentLink().getToNode().getCoord(), vehicle.getCurrentLink().getId());

		newFromAct.setStartTime(time);
		newFromAct.setEndTime(time);

		LinkNetworkRouteImpl subRoute = new LinkNetworkRouteImpl(vehicle.getCurrentLink().getId(), route.getEndLinkId(), this.network);

		// put the new route in a new leg
		Leg newLeg = new LegImpl(currentLeg.getMode());
		newLeg.setDepartureTime(currentLeg.getDepartureTime());
		newLeg.setTravelTime(currentLeg.getTravelTime());
		newLeg.setRoute(subRoute);

		// create new plan and select it
		PlanImpl newPlan = new PlanImpl(person);

		// here we are at the moment
		newPlan.addActivity(newFromAct);

		// Route from the current position to the next destination.
		newPlan.addLeg(newLeg);

		// next Activity
		newPlan.addActivity(nextActivity);

		this.planAlgorithm.run(newPlan);

		// get new calculated Route
		NetworkRoute newRoute = (NetworkRoute) newLeg.getRoute();

		// use VehicleId from existing Route
		newRoute.setVehicleId(vehicle.getId());

		// get Nodes from the current Route
		List<Node> nodesBuffer = RouteUtils.getNodes(route, this.network);

		// remove Nodes after the current Position in the Route
		nodesBuffer.subList(currentNodeIndex - 1, nodesBuffer.size()).clear();

		// Merge already driven parts of the Route with the new routed parts.
		nodesBuffer.addAll(RouteUtils.getNodes(newRoute, this.network));

		// Update Route by replacing the Nodes.
		route.setLinkIds(route.getStartLinkId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(nodesBuffer)), route.getEndLinkId());

		// Update Distance
		double distance = 0.0;
		for (Id id : route.getLinkIds())
		{
			distance = distance + this.network.getLinks().get(id).getLength();
		}
		distance = distance + this.network.getLinks().get(route.getEndLinkId()).getLength();
		route.setDistance(distance);

		// finally reset the cached next Link of the PersonAgent - it may have changed!
		withinDayPersonAgent.resetCachedNextLink();

		// create ReplanningEvent
		QSim.getEvents().processEvent(new ExtendedAgentReplanEventImpl(time, person.getId(), newRoute, route));

		return true;
	}

	@Override
	public CurrentLegReplanner clone()
	{
		CurrentLegReplanner clone = new CurrentLegReplanner(this.id, this.network);

		super.cloneBasicData(clone);

		return clone;
	}
}