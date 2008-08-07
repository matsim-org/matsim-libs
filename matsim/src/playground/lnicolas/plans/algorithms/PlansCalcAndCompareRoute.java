/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcAndCompareRoute.java
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
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.router.util.LeastCostPathCalculator;

public class PlansCalcAndCompareRoute extends AbstractPersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final LeastCostPathCalculator router_m;

	private double maxRelativeDistanceError = 0;
	private Route maxRelativeDistanceErrorRoute = null;
	private Route maxRelativeDistanceErrorReferenceRoute = null;
	private double avgRelativeDistanceError = 0;
	private double avgDistance = 0;
	private double avgReferenceDistance = 0;

	private double maxRelativeTravelTimeError = 0;
	private Route maxRelativeTravelTimeErrorRoute = null;
	private Route maxRelativeTravelTimeErrorReferenceRoute = null;
	private double avgRelativeTravelTimeError = 0;
	private double avgTravelTime = 0;
	private double avgReferenceTravelTime = 0;

	private int compareCount = 0;

	private final static Logger log = Logger.getLogger(PlansCalcAndCompareRoute.class);
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcAndCompareRoute(final LeastCostPathCalculator router) {
		super();
		this.router_m = router;
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
			} else {
				throw new Exception("cannot handle legmode '" + legmode + "'.");
			}
//				if (legmode.equals("ride")) {
//				travTime = handleRideLeg(leg, fromAct, toAct, depTime);
//			} else if (legmode.equals("pt")) {
//				travTime = handlePtLeg(leg, fromAct, toAct, depTime);
//			} else if (legmode.equals("walk")) {
//				travTime = handleWalkLeg(leg, fromAct, toAct, depTime);
//			} else if (legmode.equals("bike")) {
//				travTime = handleBikeLeg(leg, fromAct, toAct, depTime);
//			} else {
//				throw new Exception("cannot handle legmode '" + legmode + "'.");
//			}

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
			route = this.router_m.calcLeastCostPath(startNode, endNode, depTime);
			if (route == null) {
				throw new Exception("No route found from node "
						+ startNode.getId() + " to node " + endNode.getId() + ".");
			}
			Route legRoute = leg.getRoute();
			if (legRoute != null) {
				route.setDist(calcDistance(route.getRoute()));
				Double dist = legRoute.getDist();
				if (dist.isInfinite() || dist.isNaN() || dist <= 0) {
					dist = calcDistance(legRoute.getRoute());
					legRoute.setDist(dist);
				}
				compareRoutes(legRoute, route);
				if (legRoute.getTravTime() != route.getTravTime()) {
					System.out.println("Distance of route from Node "
							+ startNode.getId()
							+ " to node "
							+ endNode.getId()
							+ " has length " + route.getDist() + " (travel time "
							+ route.getTravTime() + "). Should be "
							+ legRoute.getDist() + " (travel time "
							+ legRoute.getTravTime() + ")");
				}
//				leg.setRoute(route);
			}
		} else {
			// create an empty route == staying on place if toLink == endLink
			route = new Route();
//			leg.setRoute(route);
		}

		leg.setDepTime(depTime);
		leg.setTravTime(route.getTravTime());
		leg.setArrTime(depTime + route.getTravTime());
		return route.getTravTime();
	}

	private void compareRoutes(final Route referenceRoute, final Route route2) {
		if (referenceRoute.getDist() == 0 || referenceRoute.getTravTime() == 0) {
			return;
		}
		double relativeTravelTimeError
			= (route2.getTravTime() - referenceRoute.getTravTime())
				/ referenceRoute.getTravTime();
		double relativeDistanceError = (referenceRoute.getDist() - route2.getDist())
			/ referenceRoute.getDist();

		if (relativeDistanceError > this.maxRelativeDistanceError ) {
			this.maxRelativeDistanceError = relativeDistanceError;
			this.maxRelativeDistanceErrorRoute = route2;
			this.maxRelativeDistanceErrorReferenceRoute = referenceRoute;
		}
		if (relativeTravelTimeError > this.maxRelativeTravelTimeError) {
			this.maxRelativeTravelTimeError = relativeTravelTimeError;
			this.maxRelativeTravelTimeErrorRoute = route2;
			this.maxRelativeTravelTimeErrorReferenceRoute = referenceRoute;
		}

		this.avgRelativeDistanceError = ((this.avgRelativeDistanceError * this.compareCount)
				+ relativeDistanceError) / (this.compareCount + 1);
		this.avgDistance = ((this.avgDistance * this.compareCount) + route2.getDist())
			/ (this.compareCount + 1);
		this.avgReferenceDistance = ((this.avgReferenceDistance * this.compareCount)
				+ referenceRoute.getDist()) / (this.compareCount + 1);

		this.avgRelativeTravelTimeError = ((this.avgRelativeTravelTimeError * this.compareCount)
				+ relativeTravelTimeError) / (this.compareCount + 1);
		this.avgTravelTime = ((this.avgTravelTime * this.compareCount) + route2.getTravTime())
			/ (this.compareCount + 1);
		this.avgReferenceTravelTime = ((this.avgReferenceTravelTime * this.compareCount)
				+ referenceRoute.getTravTime()) / (this.compareCount + 1);

		this.compareCount++;
	}

	private int routeCount() {
		return this.compareCount;
	}

	private double avgRelativeDistanceError() {
		return this.avgRelativeDistanceError;
	}

	private double maxRelativeDistanceError() {
		return this.maxRelativeDistanceError;
	}

	/**
	 * @return the maxRelativeDistanceErrorRoute
	 */
	public Route getMaxRelativeDistanceErrorRoute() {
		return this.maxRelativeDistanceErrorRoute;
	}

	/**
	 * @return the maxRelativeDistanceErrorReferenceRoute
	 */
	public Route getMaxRelativeDistanceErrorReferenceRoute() {
		return this.maxRelativeDistanceErrorReferenceRoute;
	}

	private double calcDistance(final ArrayList<Node> nodes) {
		double dist = 0.0;
		for (int i=0; i<nodes.size()-1; i++) {
			Node from = nodes.get(i);
			Node to = nodes.get(i+1);

			for (Link currlink : from.getOutLinks().values()) {
				if (currlink.getToNode().equals(to)) {
					dist += currlink.getLength();
					break;
				}
			}
		}
		return dist;
	}

	public String getRouterAlgorithmName() {
		return this.router_m.getClass().toString();
	}

	public void printSummary() {
		System.out.println("Number of routes: " + this.routeCount());
		System.out.println("Avg distance per route: " + this.avgDistance());
		System.out.println("Avg reference distance per route: " + this.avgReferenceDistance);
		System.out.println("Avg relative distance deviation per route: "
				+ this.avgRelativeDistanceError());
		System.out.println("Max relative distance deviation: "
				+ this.maxRelativeDistanceError());

		System.out.println("Avg travel time per route: " + this.avgTravelTime());
		System.out.println("Avg best travel time per route: "
				+ this.avgReferenceTravelTime);
		System.out.println("Avg relative travel time error per route: "
				+ this.avgRelativeTravelTimeError());
		System.out.println("Max relative travel time error: "
				+ this.maxRelativeTravelTimeError());
		System.out.println("");
	}

	private double maxRelativeTravelTimeError() {
		return this.maxRelativeTravelTimeError;
	}

	private double avgRelativeTravelTimeError() {
		return this.avgRelativeTravelTimeError;
	}

	private double avgTravelTime() {
		return this.avgTravelTime;
	}

	private double avgDistance() {
		return this.avgDistance;
	}

	/**
	 * @return the avgReferenceDistance
	 */
	public double getAvgReferenceDistance() {
		return this.avgReferenceDistance;
	}

	/**
	 * @return the avgReferenceTravelTime
	 */
	public double getAvgReferenceTravelTime() {
		return this.avgReferenceTravelTime;
	}


}
