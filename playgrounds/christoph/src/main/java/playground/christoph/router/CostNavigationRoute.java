/* *********************************************************************** *
 * project: org.matsim.*
 * CostNavigationRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

public class CostNavigationRoute extends WithinDayDuringLegReplanner {
	
	protected Network network;
	protected CostNavigationTravelTimeLogger travelTimeLogger;
	protected LeastCostPathCalculator leastCostPathCalculator;
	protected CostNavigationTravelTimeLogger costNavigationTravelTimeLogger;
	protected TravelCostCalculatorFactory travelCostFactory;
	protected PersonalizableTravelCost travelCost;
	protected PersonalizableTravelTime travelTime;
	
	
	/*package*/ CostNavigationRoute(Id id, Scenario scenario, Network network, CostNavigationTravelTimeLogger costNavigationTravelTimeLogger, 
			TravelCostCalculatorFactory travelCostFactory, PersonalizableTravelTime travelTime, LeastCostPathCalculatorFactory routerFactory) {
		super(id, scenario);
		
		this.network = network;
		this.costNavigationTravelTimeLogger = costNavigationTravelTimeLogger;
		this.travelCostFactory = travelCostFactory;
		this.travelTime = travelTime;
		this.travelCost = travelCostFactory.createTravelCostCalculator(travelTime, scenario.getConfig().planCalcScore());
		this.leastCostPathCalculator = routerFactory.createPathCalculator(network, travelCost, travelTime);
	}
	
	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {

		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;

		Plan executedPlan = withinDayAgent.getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		// If it is not a car Leg we don't replan it.
//		if (!currentLeg.getMode().equals(TransportMode.car)) return false;

		Id personId = withinDayAgent.getId();
		
		/*
		 * If the person trusts the navigation system replan its leg
		 */
		double gamma = costNavigationTravelTimeLogger.getTrust(personId);
		double phi = 1 - gamma;
		
		int currentLinkIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		Id currentLinkId = withinDayAgent.getCurrentLinkId();
		Link currentLink = network.getLinks().get(currentLinkId);
		Node nextNode = currentLink.getToNode();
		Link endLink = network.getLinks().get(withinDayAgent.getCurrentLeg().getRoute().getEndLinkId());
		Node endNode = endLink.getFromNode();
		
		Map<Id, ? extends Link> outLinksMap = nextNode.getOutLinks();
		Map<Id, Path> paths = new TreeMap<Id, Path>();	// outLinkId
		Map<Id, Double> costs = new TreeMap<Id, Double>();	// outLinkId
		Map<Id, Double> probabilities = new TreeMap<Id, Double>();	// outLinkId

		// If we have reached the endLink, we need no further replanning.
		if (currentLink.getId().equals(endLink.getId())) {
			return true;
		}
		
		// If one of the next links is the endLink, we also do no further replanning.
		if (currentLink.getToNode().getId().equals(endNode.getId())) {
			return true;
		}	
		
		// If one of the next links leads directly to the endLink, we also do no further replanning.
		for (Link outLink : outLinksMap.values()) {
			if (outLink.getToNode().getId().equals(endNode.getId())) {
				return true;
			}
		}	
		
		/*
		 * Calculate path costs for each outgoing link
		 */
		double leastCosts = Double.MAX_VALUE;
		Id leastCostLinkId = null;
		for (Link outLink : outLinksMap.values()) {
			Path path = leastCostPathCalculator.calcLeastCostPath(outLink.getToNode(), endNode, this.time);
			paths.put(outLink.getId(), path);
			costs.put(outLink.getId(), path.travelCost);
			if (path.travelCost < leastCosts) {
				leastCosts = path.travelCost;
				leastCostLinkId = outLink.getId();
			}
		}
			
		/*
		 * Calculate the probabilities for each path. We use inverse values to
		 * give short travel times a higher probability.
		 */
		double inverseSumLeastCosts = 0.0;
		for (Entry<Id, Double> entry : costs.entrySet()) {
			// if it is the least cost link
			if (entry.getKey().equals(leastCostLinkId)) inverseSumLeastCosts += 1 / (phi*entry.getValue());

			// else
			else inverseSumLeastCosts += 1 / entry.getValue();
		}
		for (Entry<Id, Double> entry : costs.entrySet()) {
			// if it is the least cost link
			if (entry.getKey().equals(leastCostLinkId)) probabilities.put(entry.getKey(), (1 / (phi*entry.getValue())) / inverseSumLeastCosts);

			// else
			else probabilities.put(entry.getKey(), (1 / entry.getValue()) / inverseSumLeastCosts);
		}
		
		double randomNumber = costNavigationTravelTimeLogger.getRandomNumber(personId);
				
		double sumProb = 0.0;
		Id nextLinkId = null;
		Path nextPath = null;
		for (Entry<Id, Double> entry : probabilities.entrySet()) {
			if (entry.getValue() + sumProb > randomNumber) {
				nextLinkId = entry.getKey();
				nextPath = paths.get(entry.getKey());
				break;
			} else {
				sumProb += entry.getValue();
			}
		}
				
		if (nextLinkId.equals(leastCostLinkId)) costNavigationTravelTimeLogger.setFollowed(personId, true);
		else {
			costNavigationTravelTimeLogger.setFollowed(personId, false);
			double c = travelCost.getLinkGeneralizedTravelCost(network.getLinks().get(nextLinkId), time);
			double expectedAlternativeCosts = c * leastCosts/nextPath.travelCost;
			costNavigationTravelTimeLogger.setExpectedAlternativeTravelTime(personId, expectedAlternativeCosts);
		}
			
		Leg leg = withinDayAgent.getCurrentLeg();
		Route route = leg.getRoute();

		// if the route type is not supported (e.g. because it is a walking agent)
		if (!(route instanceof NetworkRoute)) return false;
		NetworkRoute oldRoute = (NetworkRoute) route;

		// The linkIds of the new Route
		List<Id> linkIds = new ArrayList<Id>();
		
		/*
		 *  Get the Id of the current Link.
		 *  Create a List that contains all links of a route, including the Start- and EndLinks.
		 */
		List<Id> allLinkIds = getRouteLinkIds(oldRoute);

		/*
		 * Get those Links which have already been passed.
		 * allLinkIds contains also the startLinkId, which should not
		 * be part of the List - it is set separately. Therefore we start
		 * at index 1.
		 */
		if (currentLinkIndex > 0) {
			linkIds.addAll(allLinkIds.subList(1, currentLinkIndex + 1));
		}
		
		// add the next link
		linkIds.add(nextLinkId);
		
		// add the path from the next link to the destination
		for (Link link : nextPath.links) {
			linkIds.add(link.getId());
		}

		// Overwrite old Route
		oldRoute.setLinkIds(oldRoute.getStartLinkId(), linkIds, oldRoute.getEndLinkId());
			
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayAgent.resetCaches();

		return true;
	}

	private List<Id> getRouteLinkIds(Route route) {
		List<Id> linkIds = new ArrayList<Id>();

		if (route instanceof NetworkRoute) {
			NetworkRoute networkRoute = (NetworkRoute) route;
			linkIds.add(networkRoute.getStartLinkId());
			linkIds.addAll(networkRoute.getLinkIds());
			linkIds.add(networkRoute.getEndLinkId());
		} else {
			throw new RuntimeException("Currently only NetworkRoutes are supported for Within-Day Replanning!");
		}

		return linkIds;
	}
	
}