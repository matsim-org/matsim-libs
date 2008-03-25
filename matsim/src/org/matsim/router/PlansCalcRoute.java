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

import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.misc.Time;

public class PlansCalcRoute extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

//	private final NetworkLayer network;
	protected final LeastCostPathCalculator routeAlgo;
	protected final LeastCostPathCalculator dijkstraEmpty;
	protected final boolean calcMissingOnly;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcRoute(NetworkLayer network, TravelCostI costCalculator,
			TravelTimeI timeCalculator) {
		this(network, costCalculator, timeCalculator, false);
	}

	public PlansCalcRoute(NetworkLayer network, TravelCostI costCalculator,
			TravelTimeI timeCalculator, boolean calcMissingOnly) {
		super();
//		this.network = network;
		this.routeAlgo = new Dijkstra(network, costCalculator, timeCalculator);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		this.dijkstraEmpty = new Dijkstra(network, timeCostCalc, timeCostCalc);
		this.calcMissingOnly = calcMissingOnly;
	}

	public PlansCalcRoute(NetworkLayer network, TravelCostI costCalculator, TravelTimeI timeCalculator,
			boolean calcMissingOnly, LeastCostPathCalculator router, LeastCostPathCalculator routerEmpty) {
		super();
//		this.network = network;
		this.routeAlgo = router;
		this.dijkstraEmpty = routerEmpty;
		this.calcMissingOnly = calcMissingOnly;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(Plan plan) {
		handlePlan(plan);
	}

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	protected void handlePlan(Plan plan) {
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

	protected double handleLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		String legmode = leg.getMode();

		if (legmode == "car") {
			return handleCarLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "ride") {
			return handleRideLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "pt") {
			return handlePtLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "walk") {
			return handleWalkLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "bike") {
			return handleBikeLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "undef") {
			// TODO balmermi: No clue how to handle legs with 'undef' mode
			//                Therefore, handle it similar like bike mode with 50 km/h
			//                and no route assigned
			return handleUndefLeg(leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legmode + "'.");
		}
	}

	private double handleCarLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		double travTime = 0;
		LinkImpl fromLink = fromAct.getLink();
		LinkImpl toLink = toAct.getLink();
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		Route route = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			if (leg.getRoute() == null || !this.calcMissingOnly) {
				route = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime);
				if (route == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
				leg.setRoute(route);
				travTime = route.getTravTime();
			} else {
				route = leg.getRoute();
				travTime = leg.getTravTime();
				if (travTime == Time.UNDEFINED_TIME) {
					travTime = route.getTravTime();
				}
				if (travTime == Time.UNDEFINED_TIME) {
					travTime = 0;
				}
			}
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

	private double handleRideLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// handle a ride exactly the same was as a car
		// the simulation has to take car that this leg is not really simulated as a stand-alone driver
		return handleCarLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// currently: calc route in empty street network, use twice the traveltime
		// TODO [MR] later: use special pt-router

		double travTime = 0;
		LinkImpl fromLink = fromAct.getLink();
		LinkImpl toLink = toAct.getLink();
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		Route route = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			if (leg.getRoute() == null || !this.calcMissingOnly) {
				route = this.dijkstraEmpty.calcLeastCostPath(startNode, endNode, depTime);
				if (route == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
				travTime = route.getTravTime() * 2;
				route.setTravTime(travTime);
				leg.setRoute(route);
			} else {
				route = leg.getRoute();
				travTime = leg.getTravTime();
				if (travTime == Time.UNDEFINED_TIME) {
					travTime = route.getTravTime();
				}
				if (travTime == Time.UNDEFINED_TIME) {
					travTime = 0;
				}
			}
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

	private double handleWalkLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// make simple assumption about distance and walking speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 3.0 / 3.6; // 3.0 km/h --> m/s
//	 create an empty route, but with realistic traveltime
		Route route = new Route();
		int travTime = (int)(dist / speed);
		route.setTravTime(travTime);
		leg.setRoute(route);
		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

	private double handleBikeLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// make simple assumption about distance and cycling speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 15.0 / 3.6; // 15.0 km/h --> m/s
//	 create an empty route, but with realistic traveltime
		Route route = new Route();
		int travTime = (int)(dist / speed);
		route.setTravTime(travTime);
		leg.setRoute(route);
		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

	private double handleUndefLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// make simple assumption about distance and a dummy speed (50 km/h)
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 50.0 / 3.6; // 50.0 km/h --> m/s
//	 create an empty route, but with realistic traveltime
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
