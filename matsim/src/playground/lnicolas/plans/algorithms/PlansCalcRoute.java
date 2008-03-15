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

package playground.lnicolas.plans.algorithms;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.router.util.LeastCostPathCalculator;

public class PlansCalcRoute extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final LeastCostPathCalculator router_m;
	private final LeastCostPathCalculator ptRouter_m;
	private final boolean calcMissingOnly_m;

	private final static Logger log = Logger.getLogger(PlansCalcRoute.class);
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcRoute(final LeastCostPathCalculator router) {
		this(router, false);
	}

	public PlansCalcRoute(final LeastCostPathCalculator router, final boolean calcMissingOnly) {
		this(router, null, calcMissingOnly);
	}

	public PlansCalcRoute(final LeastCostPathCalculator router, final LeastCostPathCalculator ptRouter,
			final boolean calcMissingOnly) {
		super();
		this.router_m = router;
		this.ptRouter_m = ptRouter;
		this.calcMissingOnly_m = calcMissingOnly;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			try {
				handlePlan(plan);
			} catch (Exception e) {
				log.warn("Skipping plan id="+planId + " of person id="
						+ person.getId() + " because of: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public void run(final Plan plan) {
		try {
			handlePlan(plan);
		} catch (Exception e) {
			log.warn("Skipping plan id=unknown of person id=unknown because of: "
					+ e.getMessage());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	public void handlePlan(final Plan plan) throws Exception {
		ArrayList actslegs = plan.getActsLegs();
		Act fromAct = (Act)actslegs.get(0);
		double depTime = 0;
		double travTime = 0;

		// loop over all <act>s
		for (int j = 2; j < actslegs.size(); j=j+2) {
			Act toAct = (Act)actslegs.get(j);
			Leg leg = (Leg)actslegs.get(j-1);
			String legmode = leg.getMode();

			if (fromAct.getEndTime() >= 0) {
				depTime = fromAct.getEndTime();
			} else if (fromAct.getDur() >= 0) {
				depTime = depTime + travTime + fromAct.getDur();
			} else {
				throw new Exception("act " + (j-2) + " has neither end-time nor duration.");
			}

			if (legmode.equals("car")) {
				travTime = handleCarLeg(leg, fromAct, toAct, depTime);
			} else if (legmode.equals("ride")) {
				travTime = handleRideLeg(leg, fromAct, toAct, depTime);
			} else if (legmode.equals("pt")) {
				travTime = handlePtLeg(leg, fromAct, toAct, depTime);
			} else if (legmode.equals("walk")) {
				travTime = handleWalkLeg(leg, fromAct, toAct, depTime);
			} else if (legmode.equals("bike")) {
				travTime = handleBikeLeg(leg, fromAct, toAct, depTime);
			} else {
				throw new Exception("cannot handle legmode '" + legmode + "'.");
			}

			fromAct = toAct;
		}
	}

	private double handleCarLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) throws Exception {
		Link fromLink = fromAct.getLink();
		Link toLink = toAct.getLink();
		if (fromLink == null) throw new Exception("fromLink missing.");
		if (toLink == null) throw new Exception("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		Route route = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			if (leg.getRoute() == null || !this.calcMissingOnly_m) {
				route = this.router_m.calcLeastCostPath(startNode, endNode, depTime);
				if (route == null) {
					route = this.router_m.calcLeastCostPath(startNode, endNode, depTime);
					throw new Exception("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
				}
				leg.setRoute(route);
			} else {
				route = leg.getRoute();
			}
		} else {
			// create an empty route == staying on place if toLink == endLink
			route = new Route();
			leg.setRoute(route);
		}

		leg.setDepTime(depTime);
		leg.setTravTime(route.getTravTime());
		leg.setArrTime(depTime + route.getTravTime());
		return route.getTravTime();
	}

	private double handleRideLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) throws Exception {
		// handle a ride exactly the same was as a car
		// the simulation has to take car that this leg is not really simulated as a stand-alone driver
		return handleCarLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) throws Exception {
		// currently: calc route in empty street network, use twice the traveltime
		// later: use special pt-router

		double travTime = 0;
		Link fromLink = fromAct.getLink();
		Link toLink = toAct.getLink();
		if (fromLink == null) throw new Exception("fromLink missing.");
		if (toLink == null) throw new Exception("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		Route route = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			if (leg.getRoute() == null || !this.calcMissingOnly_m) {
				route = this.ptRouter_m.calcLeastCostPath(startNode, endNode, depTime);
				if (route == null) throw new Exception("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
				travTime = route.getTravTime() * 2;
				route.setTravTime(travTime);
				leg.setRoute(route);
			} else {
				route = leg.getRoute();
			}
		} else {
			// create an empty route == staying on place if toLink == endLink
			route = new Route();
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
//	 create an empty route, but with realistic traveltime
		Route route = new Route();
		double travTime = dist / speed;
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
//	 create an empty route, but with realistic traveltime
		Route route = new Route();
		double travTime = dist / speed;
		route.setTravTime(travTime);
		leg.setRoute(route);
		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime + travTime);
		return travTime;
	}

	public String getRouterAlgorithmName() {
		return this.router_m.getClass().toString();
	}

}
