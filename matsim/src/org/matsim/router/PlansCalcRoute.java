/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRoute.java
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

package org.matsim.router;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.misc.Time;

public class PlansCalcRoute extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	/**
	 * The routing algorithm to be used for finding routes on the network with actual travel times.
	 */
	protected final LeastCostPathCalculator routeAlgo;
	/**
	 * The routing algorithm to be used for finding routes in the empty network, with freeflow travel times.
	 */
	protected final LeastCostPathCalculator routeAlgoFreeflow;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcRoute(final NetworkLayer network, final TravelCost costCalculator, final TravelTime timeCalculator) {
		super();
		this.routeAlgo = new Dijkstra(network, costCalculator, timeCalculator);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		this.routeAlgoFreeflow = new Dijkstra(network, timeCostCalc, timeCostCalc);
	}

	/**
	 * @param router The routing algorithm to be used for finding routes on the network with actual travel times.
	 * @param routerFreeflow The routing algorithm to be used for finding routes in the empty network, with freeflow travel times.
	 */
	public PlansCalcRoute(final LeastCostPathCalculator router, final LeastCostPathCalculator routerFreeflow) {
		super();
		this.routeAlgo = router;
		this.routeAlgoFreeflow = routerFreeflow;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(final Plan plan) {
		handlePlan(plan);
	}

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	protected void handlePlan(final Plan plan) {
		ArrayList<?> actslegs = plan.getActsLegs();
		Act fromAct = (Act)actslegs.get(0);
		double now = 0;

		// loop over all <act>s
		for (int j = 2; j < actslegs.size(); j=j+2) {
			Act toAct = (Act)actslegs.get(j);
			Leg leg = (Leg)actslegs.get(j-1);

			double endTime = fromAct.getEndTime();
			double startTime = fromAct.getStartTime();
			double dur = fromAct.getDur();
			if (endTime != Time.UNDEFINED_TIME) {
				// use fromAct.endTime as time for routing
				now = endTime;
			} else if (startTime != Time.UNDEFINED_TIME && dur != Time.UNDEFINED_TIME) {
				// use fromAct.startTime + fromAct.duration as time for routing
				now = startTime + dur;
			} else if (dur != Time.UNDEFINED_TIME) {
				// use last used time + fromAct.duration as time for routing
				now += dur;
			} else {
				throw new RuntimeException("act " + j + " has neither end-time nor duration.");
			}

			handleLeg(leg, fromAct, toAct, now);

			fromAct = toAct;
		}
	}

	/**
	 * @param leg the leg to calculate the route for.
	 * @param fromAct the Act the leg starts
	 * @param toAct the Act the leg ends
	 * @param depTime the time (seconds from midnight) the leg starts
	 * @return the estimated travel time for this leg
	 */
	public double handleLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		BasicLeg.Mode legmode = leg.getMode();

		if (legmode == BasicLeg.Mode.car) {
			return handleCarLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == BasicLeg.Mode.ride) {
			return handleRideLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == BasicLeg.Mode.pt) {
			return handlePtLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == BasicLeg.Mode.walk) {
			return handleWalkLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == BasicLeg.Mode.bike) {
			return handleBikeLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == BasicLeg.Mode.undefined) {
			/* TODO balmermi: No clue how to handle legs with 'undef' mode
			 *                Therefore, handle it similar like bike mode with 50 km/h
			 *                and no route assigned  */
			return handleUndefLeg(leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legmode + "'.");
		}
	}

	protected double handleCarLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		double travTime = 0;
		Link fromLink = fromAct.getLink();
		Link toLink = toAct.getLink();
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		Route route = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			route = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime);
			if (route == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			leg.setRoute(route);
			travTime = route.getTravTime();
		} else {
			// create an empty route == staying on place if toLink == endLink
			route = new Route();
			route.setTravTime(0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

	private double handleRideLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		// handle a ride exactly the same was as a car
		// the simulation has to take care that this leg is not really simulated as a stand-alone driver
		return handleCarLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		// currently: calc route in empty street network, use twice the traveltime
		// TODO [MR] later: use special pt-router

		double travTime = 0;
		Link fromLink = fromAct.getLink();
		Link toLink = toAct.getLink();
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");


		Route route = null;
		if (toLink != fromLink) {
			Node startNode = fromLink.getToNode();	// start at the end of the "current" link
			Node endNode = toLink.getFromNode(); // the target is the start of the link
			// do not drive/walk around, if we stay on the same link
			route = this.routeAlgoFreeflow.calcLeastCostPath(startNode, endNode, depTime);
			if (route == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			// we're still missing the time on the final link, which the agent has to drive on in the java mobsim
			// so let's calculate the final part.
			double travelTimeLastLink = toLink.getFreespeedTravelTime(depTime + route.getTravTime());
			travTime = (route.getTravTime() + travelTimeLastLink) * 2.0;
			route.setTravTime(travTime);
			leg.setRoute(route);
		} else {
			// create an empty route == staying on place if toLink == endLink
			route = new Route();
			route.setTravTime(0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

	private double handleWalkLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		// make simple assumption about distance and walking speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 3.0 / 3.6; // 3.0 km/h --> m/s
		// create an empty route, but with realistic traveltime
		Route route = new Route();
		int travTime = (int)(dist / speed);
		route.setTravTime(travTime);
		leg.setRoute(route);
		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

	private double handleBikeLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		// make simple assumption about distance and cycling speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 15.0 / 3.6; // 15.0 km/h --> m/s
		// create an empty route, but with realistic traveltime
		Route route = new Route();
		int travTime = (int)(dist / speed);
		route.setTravTime(travTime);
		leg.setRoute(route);
		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

	private double handleUndefLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		// make simple assumption about distance and a dummy speed (50 km/h)
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 50.0 / 3.6; // 50.0 km/h --> m/s
		// create an empty route, but with realistic traveltime
		Route route = new Route();
		int travTime = (int)(dist / speed);
		route.setTravTime(travTime);
		leg.setRoute(route);
		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

}
