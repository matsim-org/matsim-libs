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

import org.matsim.basic.v01.BasicLeg;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.utils.misc.Time;

/**
 * A special router for complete plans that assigns the best routes to a plan
 * with respect to an area toll. Uses internally the {@link AStarLandmarks} routing algorithm.
 *
 * @author mrieser
 */
public class PlansCalcAreaTollRoute extends PlansCalcRouteLandmarks {

	private final RoadPricingScheme scheme;
	private final TravelTime timeCalculator;
	private final LeastCostPathCalculator tollRouter;

	/**
	 * Constructs a new Area-Toll Router.
	 *
	 * @param network
	 * @param preProcessData common data for the A*-Landmarks algorithm
	 * @param costCalculator This must be a normal implementation of TravelCost that does not take care of the area toll!
	 * @param timeCalculator
	 * @param scheme
	 */
	public PlansCalcAreaTollRoute(final NetworkLayer network, final PreProcessLandmarks preProcessData, final TravelCost costCalculator, final TravelTime timeCalculator, final RoadPricingScheme scheme) {
		super(network, preProcessData, costCalculator, timeCalculator);
		this.scheme = scheme;
		this.timeCalculator = timeCalculator;
		this.tollRouter = new AStarLandmarks(network, preProcessData, new TollTravelCostCalculator(costCalculator, scheme), timeCalculator);
	}

	@Override
	protected void handlePlan(final Plan plan) {

		boolean agentPaysToll = false;

		ArrayList<?> actslegs = plan.getActsLegs();
		Act fromAct = (Act)actslegs.get(0);

		final int TOLL_INDEX = 0;
		final int NOTOLL_INDEX = 1;
		final int nofLegs = (actslegs.size() - 1) / 2;
		CarRoute[][] routes = new CarRoute[2][nofLegs];
		double[][] depTimes = new double[2][nofLegs];
		boolean[] isCarLeg = new boolean[nofLegs];

		int routeIndex = 0; // counter for the legs

		// start at endtime of first activity, this must be available according spec
		depTimes[TOLL_INDEX][routeIndex] = fromAct.getEndTime();
		depTimes[NOTOLL_INDEX][routeIndex] = fromAct.getEndTime();
		/* Loop over all act's and calculate two routes for each leg: one which
		 * could lead through the toll area, and one which should not.
		 * The variants are stored in the routes array.
		 */
		for (int i = 2, n = actslegs.size(); i < n; i += 2) {

			Leg leg = (Leg)actslegs.get(i-1);
			Act toAct = (Act)actslegs.get(i);
			isCarLeg[routeIndex] = BasicLeg.Mode.car.equals(leg.getMode());
			if (!isCarLeg[routeIndex]) {
				super.handleLeg(leg, fromAct, toAct, depTimes[NOTOLL_INDEX][routeIndex]);
			} else {
				// it is a car leg...

				// # init some values before searching for routes:
				Link fromLink = fromAct.getLink();
				Link toLink = toAct.getLink();
				if (fromLink == null) {
					throw new RuntimeException("fromLink missing.");
				}
				if (toLink == null) {
					throw new RuntimeException("toLink missing.");
				}
				Node startNode = fromLink.getToNode();	// start at the end of the "current" link
				Node endNode = toLink.getFromNode(); // the target is the start of the link

				CarRoute tollRoute = new NodeCarRoute();
				tollRoute.setStartLink(fromLink);
				tollRoute.setEndLink(toLink);
				CarRoute noTollRoute = null;

				// # start searching a route where agent may pay the toll
				boolean tollRouteInsideTollArea = false;
				if (toLink != fromLink) {
					Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTimes[TOLL_INDEX][routeIndex]);
					if (path == null) {
						throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
					}
					tollRouteInsideTollArea = routeOverlapsTollLinks(fromLink, path, toLink, depTimes[TOLL_INDEX][routeIndex]);
					tollRoute.setNodes(path.nodes, (int) path.travelTime, path.travelCost); 
				} else {
					// do not drive/walk around, if we stay on the same link
					tollRoute.setDist(0.0);
					tollRoute.setTravelTime(0.0);
					// if we don't drive around, it doesn't matter  if we're in or out the toll area, so use "false"
				}

				// # start searching a route where agent avoids paying the toll
				if (tollRouteInsideTollArea && !agentPaysToll) {
					/* The agent does not yet pay the toll, but the actual best route would
					 * lead into the tolling area. If the agent must pay the toll, this
					 * route may no longer be the best. Thus calculate a route that should
					 * not cross the tolling area to compare it with the toll-route.
					 * this.tollRouter has very high costs on links in the toll area. So if
					 * it is possible for the agent to drive around, it will, otherwise there
					 * will still be a route returned.
					 */
					Path path = this.tollRouter.calcLeastCostPath(startNode, endNode, depTimes[TOLL_INDEX][routeIndex]);
					noTollRoute = new NodeCarRoute();
					noTollRoute.setStartLink(fromLink);
					noTollRoute.setEndLink(toLink);
					noTollRoute.setNodes(path.nodes, (int) path.travelTime, path.travelCost);
//					noTollRoute.setTravelTime(path.travelTime);

					if (routeOverlapsTollLinks(fromLink, path, toLink, depTimes[TOLL_INDEX][routeIndex])) {
						/* the no-toll route leads also through the tolling area, so it seems the agent
						 * can not avoid paying the toll. */
						agentPaysToll = true;
						noTollRoute = null; // delete this route again, as the tollRoute will likely be the better one with "normal" list costs.
					}
				}

				// # store the routes we calculated
				routes[TOLL_INDEX][routeIndex] = tollRoute;
				if (noTollRoute == null) {
					// if there is no special no-toll route, use the toll route
					routes[NOTOLL_INDEX][routeIndex] = tollRoute;
				} else {
					routes[NOTOLL_INDEX][routeIndex] = noTollRoute;
				}

				int nextIndex = routeIndex + 1;

				if (nextIndex < routes[0].length) {
					// update time
					// first, add travel time
					depTimes[TOLL_INDEX][nextIndex] = depTimes[TOLL_INDEX][routeIndex] + routes[TOLL_INDEX][routeIndex].getTravelTime();
					depTimes[NOTOLL_INDEX][nextIndex] = depTimes[NOTOLL_INDEX][routeIndex] + routes[NOTOLL_INDEX][routeIndex].getTravelTime();
					// next, add activity duration or set endtime
					double endTime = toAct.getEndTime();
					double dur = toAct.getDuration();

					if ((endTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
						double min = Math.min(endTime, depTimes[TOLL_INDEX][nextIndex] + dur);
						if (depTimes[TOLL_INDEX][nextIndex] < min) depTimes[TOLL_INDEX][nextIndex] = min;
						min = Math.min(endTime, depTimes[NOTOLL_INDEX][nextIndex] + dur);
						if (depTimes[NOTOLL_INDEX][nextIndex] < min) depTimes[NOTOLL_INDEX][nextIndex] = min;
					} else if (endTime != Time.UNDEFINED_TIME) {
						if (depTimes[TOLL_INDEX][nextIndex] < endTime) depTimes[TOLL_INDEX][nextIndex] = endTime;
						if (depTimes[NOTOLL_INDEX][nextIndex] < endTime) depTimes[NOTOLL_INDEX][nextIndex] = endTime;
					} else if (dur != Time.UNDEFINED_TIME) {
						depTimes[TOLL_INDEX][nextIndex] += dur;
						depTimes[NOTOLL_INDEX][nextIndex] += dur;
					} else if ((i+1) != actslegs.size()) {
						// if it's the last act on the plan, we don't care, otherwise exception
						throw new RuntimeException("act " + i + " has neither end-time nor duration.");
					}
				}
			}
			routeIndex++;
			fromAct = toAct;
		}
		// # okay, we're through the plan once ...

		/* Now decide if it is better for the agent to pay the toll or not, if the
		 * agent is not yet already forced to pay it.
		 * Compare for this the sum of all minimal costs plus the toll cost versus
		 * the costs of all no-toll routes.
		 */
		if (!agentPaysToll) {
			double cheapestCost = 0.0;
			double noTollCost = 0.0;
			for (int i = 0, n = routes[0].length; i < n; i++) {
				if (isCarLeg[i]) {
					cheapestCost += Math.min(routes[TOLL_INDEX][i].getTravelCost(), routes[NOTOLL_INDEX][i].getTravelCost());
					noTollCost += routes[NOTOLL_INDEX][i].getTravelCost();
				}
			}
			double tollAmount = this.scheme.getCostArray()[0].amount; // just take the amount of the first cost object. For the area toll, all costs' amounts should be the same.
			agentPaysToll = (cheapestCost + tollAmount) < noTollCost;
		}

		/* Assign the routes to the legs according to the agent paying the toll or not */
		if (agentPaysToll) {
			// when the agent pays the toll, just take the cheaper route of the two
			for (int i = 0; i < nofLegs; i++) {
				if (isCarLeg[i]) {
					Leg leg = (Leg)actslegs.get(i*2+1);
					if (routes[TOLL_INDEX][i].getTravelCost() < routes[NOTOLL_INDEX][i].getTravelCost()) {
						leg.setRoute(routes[TOLL_INDEX][i]);
					} else {
						leg.setRoute(routes[NOTOLL_INDEX][i]);
					}
				}
			}
		} else {
			// the agent does not pay the toll, always take the no-toll route
			for (int i = 0; i < nofLegs; i++) {
				if (isCarLeg[i]) {
					// only set the route if its a leg with mode="car", otherwise it already should be handled.
					Leg leg = (Leg)actslegs.get(i*2+1);
					leg.setRoute(routes[NOTOLL_INDEX][i]);
				}
			}
		}

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
	private boolean routeOverlapsTollLinks(final Link startLink, final Path route, final Link endLink, final double depTime) {
		double time = depTime;

		// handle first link
		if (isLinkTolled(startLink, time)) {
			return true;
		}
		/* do not advance the time yet. The router starts at the endNode of the
		 * startLink, thus actually starts at the specified  time with the first
		 * link of the route.
		 */

		// handle following links
		for (Link link : route.links) {
			if (isLinkTolled(link, time)) {
				return true;
			}
			time += this.timeCalculator.getLinkTravelTime(link, time);
		}

		// handle last link
		return isLinkTolled(endLink, time);
	}

	private boolean isLinkTolled(final Link link, final double time) {
		return this.scheme.getLinkCost(link.getId(), time) != null;
	}

}
