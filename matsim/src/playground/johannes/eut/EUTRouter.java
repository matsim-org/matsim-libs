/* *********************************************************************** *
 * project: org.matsim.*
 * EUTRouter.java
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

/**
 * 
 */
package playground.johannes.eut;

import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelTime;

/**
 * @author illenberger
 * 
 */
public class EUTRouter implements LeastCostPathCalculator {

	private static final int searchPaths = 3;
	
	private static final CARAFunction indiffFunc = new CARAFunction(0);

	private EUTRouterAnalyzer analyzer;

	private KSPPenalty kspPenalty;

	private TravelTimeMemory ttKnowledge;

	private ArrowPrattRiskAversionI utilFunc;

	public EUTRouter(NetworkLayer network, TravelTimeMemory ttKnowledge,
			ArrowPrattRiskAversionI utilFunc) {
		this.ttKnowledge = ttKnowledge;
		this.utilFunc = utilFunc;
		kspPenalty = new KSPPenalty(network);

	}

	public void setAnalyzer(EUTRouterAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	public Route calcLeastCostPath(Node fromNode, Node toNode, double starttime) {
		return selectChoice(generateChoiceSet(fromNode, toNode, starttime),
				starttime);
	}

	protected List<Route> generateChoiceSet(Node departure, Node destination,
			double time) {
		return kspPenalty.getPaths(departure, destination, time, searchPaths,
				ttKnowledge.getMeanTravelTimes());
	}

	protected Route selectChoice(List<Route> routes, double starttime) {
		Route bestRoute = null;
		Route indiffRoute = null;
		double leastcost = Double.MAX_VALUE;
		double leastIndiffCost = Double.MAX_VALUE;
		
//		/*
//		 * We can expect the first route in the list to be the real best path.
//		 */
//		indiffRoute = routes.get(0);
		for (Route route : routes) {
			double totaltravelcosts = 0;
			double totalIndiffCosts = 0;
			
			for (TravelTime traveltimes : ttKnowledge.getTravelTimes()) {
				double traveltime = calcTravTime(traveltimes, route, starttime);
				double travelcosts = utilFunc.evaluate(traveltime);
				totaltravelcosts += travelcosts;
				totalIndiffCosts += indiffFunc.evaluate(traveltime);
			}

			double avrcosts = totaltravelcosts
					/ (double) ttKnowledge.getTravelTimes().size();

			if (avrcosts < leastcost) {
				leastcost = avrcosts;
				bestRoute = route;
			}
			
			double avrIndiffCost = totalIndiffCosts/(double) ttKnowledge.getTravelTimes().size();
			
			if(avrIndiffCost < leastIndiffCost) {
				leastIndiffCost = avrIndiffCost;
				indiffRoute = route;
			}
		}

		if(analyzer != null)
			analyzer.appendSnapshot(bestRoute, leastcost, indiffRoute);
		
		return bestRoute;
	}

	private double calcTravTime(TravelTime traveltimes, Route route,
			double starttime) {
		double totaltt = 0;
		for (Link link : route.getLinkRoute()) {
			totaltt += traveltimes.getLinkTravelTime(link, starttime + totaltt);
		}
		return totaltt;
	}
}
