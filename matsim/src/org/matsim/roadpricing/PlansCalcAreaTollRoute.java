/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcAreaTollRoute.java
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

package org.matsim.roadpricing;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.router.Dijkstra;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.misc.Time;

/**
 * A special router for complete plans that assigns the best routes to a plan
 * with respect to an area toll.
 *
 * @author mrieser
 */
public class PlansCalcAreaTollRoute extends PlansCalcRoute {

	private final RoadPricingScheme scheme;
	private final TravelTimeI timeCalculator;
	private final LeastCostPathCalculator tollRouter;

	/**
	 * Constructs a new Area-Toll Router.
	 *
	 * @param network
	 * @param costCalculator This must be a normal implementation of TravelCostI that does not take care of the area toll!
	 * @param timeCalculator
	 * @param scheme
	 */
	public PlansCalcAreaTollRoute(final NetworkLayer network, final TravelCostI costCalculator, final TravelTimeI timeCalculator, final RoadPricingScheme scheme) {
		super(network, costCalculator, timeCalculator);
		this.scheme = scheme;
		this.timeCalculator = timeCalculator;
		this.tollRouter = new Dijkstra(network, new TollTravelCostCalculator(costCalculator, scheme), timeCalculator); // TODO [MR] allow usage of other routing algorithms
	}

	@Override
	protected void handlePlan(final Plan plan) {
		// TODO [MR] currently, every leg is handled as a "car" leg.

		boolean agentPaysToll = false;

		ArrayList<?> actslegs = plan.getActsLegs();
		Act fromAct = (Act)actslegs.get(0);

		final int TOLL_INDEX = 0;
		final int NOTOLL_INDEX = 1;
		Route[][] routes = new Route[2][(actslegs.size()  - 1) / 2];
		double[][] depTimes = new double[2][(actslegs.size()  - 1) / 2];
		int routeIndex = 0;

		// start at endtime of first activity, this must be available according spec
		depTimes[TOLL_INDEX][routeIndex] = fromAct.getEndTime();
		depTimes[NOTOLL_INDEX][routeIndex] = fromAct.getEndTime();
		/* Loop over all act's and calculate two routes for each leg: one which
		 * could lead through the toll area, and one which should not.
		 * The variants are stored in the routes array.
		 */
		for (int i = 2, n = actslegs.size(); i < n; i += 2) {

			Act toAct = (Act)actslegs.get(i);

			LinkImpl fromLink = fromAct.getLink();
			LinkImpl toLink = toAct.getLink();
			if (fromLink == null) throw new RuntimeException("fromLink missing.");
			if (toLink == null) throw new RuntimeException("toLink missing.");

			Node startNode = fromLink.getToNode();	// start at the end of the "current" link
			Node endNode = toLink.getFromNode(); // the target is the start of the link

			Route tollRoute = null;
			Route noTollRoute = null;
			if (toLink != fromLink) {
				// do not drive/walk around, if we stay on the same link
				tollRoute = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTimes[TOLL_INDEX][routeIndex]);
				if (tollRoute == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			} else {
				tollRoute = new Route();
				tollRoute.setRoute(null, 0, 0.0);
			}

			boolean tollRouteInsideTollArea = routeOverlapsTollLinks(fromLink, tollRoute, toLink, depTimes[TOLL_INDEX][routeIndex]);
			boolean noTollRouteInsideTollArea = false;
			if (tollRouteInsideTollArea && !agentPaysToll) {
				/* The agent does not yet pay the toll, but the actual best route would
				 * lead into the tolling area. If the agent must pay the toll, this
				 * route may no longer be the best. Thus calculate a route that should
				 * not cross the tolling area to compare it with the toll-route.
				 */
				noTollRoute = this.tollRouter.calcLeastCostPath(startNode, endNode, depTimes[TOLL_INDEX][routeIndex]);
			}
			if (noTollRoute != null) {
				noTollRouteInsideTollArea = routeOverlapsTollLinks(fromLink, noTollRoute, toLink, depTimes[TOLL_INDEX][routeIndex]);
			}

			if (tollRouteInsideTollArea && noTollRouteInsideTollArea) {
				/* both routes lead through the tolling area, so it seems the agent
				 * can not avoid paying the toll.
				 */
				agentPaysToll = true;
			}

			routes[TOLL_INDEX][routeIndex] = tollRoute;
			if (noTollRoute == null) {
				// if there is no special no-toll route, use the toll route
				routes[NOTOLL_INDEX][routeIndex] = tollRoute;
			} else {
				routes[NOTOLL_INDEX][routeIndex] = noTollRoute;
			}
			int prevIndex = routeIndex;
			routeIndex++;

			if (routeIndex < routes[0].length) {
				// update time
				// first, add travel time
				depTimes[TOLL_INDEX][routeIndex] = depTimes[TOLL_INDEX][prevIndex] + routes[TOLL_INDEX][prevIndex].getTravTime();
				depTimes[NOTOLL_INDEX][routeIndex] = depTimes[NOTOLL_INDEX][prevIndex] + routes[NOTOLL_INDEX][prevIndex].getTravTime();
				// next, add activity duration or set endtime
				double endTime = toAct.getEndTime();
				double dur = toAct.getDur();

				if ((endTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
					double min = Math.min(endTime, depTimes[TOLL_INDEX][routeIndex] + dur);
					if (depTimes[TOLL_INDEX][routeIndex] < min) depTimes[TOLL_INDEX][routeIndex] = min;
					min = Math.min(endTime, depTimes[NOTOLL_INDEX][routeIndex] + dur);
					if (depTimes[NOTOLL_INDEX][routeIndex] < min) depTimes[NOTOLL_INDEX][routeIndex] = min;
				} else if (endTime != Time.UNDEFINED_TIME) {
					if (depTimes[TOLL_INDEX][routeIndex] < endTime) depTimes[TOLL_INDEX][routeIndex] = endTime;
					if (depTimes[NOTOLL_INDEX][routeIndex] < endTime) depTimes[NOTOLL_INDEX][routeIndex] = endTime;
				} else if (dur != Time.UNDEFINED_TIME) {
					depTimes[TOLL_INDEX][routeIndex] += dur;
					depTimes[NOTOLL_INDEX][routeIndex] += dur;
				} else if ((i+1) != actslegs.size()) {
					// if it's the last act on the plan, we don't care, otherwise exception
					throw new RuntimeException("act " + i + " has neither end-time nor duration.");
				}
			}

			fromAct = toAct;
		}

		/* Now decide if it is better for the agent to pay the toll or not, if the
		 * agent is not yet already forced to pay it.
		 * Compare for this the sum of all minimal costs plus the toll cost versus
		 * the costs of all no-toll routes.
		 */
		if (!agentPaysToll) {
			double cheapestCost = 0.0;
			double noTollCost = 0.0;
			for (int i = 0, n = routes[0].length; i < n; i++) {
				cheapestCost += Math.min(routes[TOLL_INDEX][i].getTravelCost(), routes[NOTOLL_INDEX][i].getTravelCost());
				noTollCost += routes[NOTOLL_INDEX][i].getTravelCost();
			}
			double tollAmount = this.scheme.getCostArray()[0].amount; // just take the amount of the first cost object. For the area toll, all costs' amounts should be the same.
			agentPaysToll = (cheapestCost + tollAmount) < noTollCost;
		}

		/* Assign the routes to the legs according to the agent paying the toll or not */
		if (agentPaysToll) {
			// when the agent pays the toll, just take the cheaper route of the two
			for (int i = 0, n = (actslegs.size() - 1) / 2; i < n; i++) {
				Leg leg = (Leg)actslegs.get(i*2+1);
				if (routes[TOLL_INDEX][i].getTravelCost() < routes[NOTOLL_INDEX][i].getTravelCost()) {
					leg.setRoute(routes[TOLL_INDEX][i]);
				} else {
					leg.setRoute(routes[NOTOLL_INDEX][i]);
				}
			}
		} else {
			// the agent does not pay the toll, always take the no-toll route
			for (int i = 0, n = (actslegs.size() - 1) / 2; i < n; i++) {
				Leg leg = (Leg)actslegs.get(i*2+1);
				leg.setRoute(routes[NOTOLL_INDEX][i]);
			}
		}

		/* That's it! ;-) */
	}

	/**
	 * Tests, whether the route from <code>startLink</code> along <code>route</code>
	 * to <code>endLink<code>, started at <code>depTime</code>, will likely lead
	 * over tolled links.
	 *
	 * @param startLink The link on which the agent starts.
	 * @param route The route to test.
	 * @param endLink The link on which the agent arrives.
	 * @param depTime The time at which the agent departs.
	 * @return true if the route leads into an active tolling area and an agent
	 * taking this route will likely have to pay the toll, false otherwise.
	 */
	private boolean routeOverlapsTollLinks(final Link startLink, final Route route, final LinkImpl endLink, final double depTime) {
		double time = depTime;

		// handle first link
		if (isLinkTolled(startLink, time)) return true;
		/* do not advance the time yet. The router starts at the endNode of the
		 * startLink, thus actually starts at the specified  time with the first
		 * link of the route.
		 */

		// handle following links
		for (Link link : route.getLinkRoute()) {
			if (isLinkTolled(link, time)) return true;
			time += this.timeCalculator.getLinkTravelTime(link, time);
		}

		// handle last link
		return isLinkTolled(endLink, time);
	}

	private boolean isLinkTolled(final Link link, final double time) {
		return this.scheme.getLinkCost(link.getId(), time) != null;
	}

}
