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
import org.matsim.plans.Route;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelTimeI;

/**
 * @author illenberger
 *
 */
public class EUTRouter implements LeastCostPathCalculator {

	private KSPPenalty kspPenalty;
	
	private KStateLinkCostProvider ttProvider;
	
	private ArrowPrattRiskAversionI utilFunc;
	
	public EUTRouter(NetworkLayer network, KStateLinkCostProvider provider, ArrowPrattRiskAversionI utilFunc) {
		ttProvider = provider;
		this.utilFunc = utilFunc;
		kspPenalty = new KSPPenalty(network);
	}
	
	public Route calcLeastCostPath(Node fromNode, Node toNode, double starttime) {
		return selectChoice(generateChoiceSet(fromNode, toNode, starttime), starttime);
	}

	protected List<Route> generateChoiceSet(Node departure, Node destination, double time) {
		return kspPenalty.getPaths(departure, destination, time, 3, ttProvider.requestLinkCost());
	}
	
	protected Route selectChoice(List<Route> routes, double starttime) {
		Route bestroute = null;
		double leastcost = Double.MAX_VALUE;
		
		for(Route route : routes) {
			double totalcosts = 0;
			for(int state = 0; state < ttProvider.size(); state++) {
				TravelTimeI traveltimes = ttProvider.requestLinkCost(state);
				double totaltt = 0;
				for(Link link : route.getLinkRoute()) {
					totaltt += traveltimes.getLinkTravelTime(link, starttime + totaltt); 
				}
				totalcosts += utilFunc.evaluate(totaltt);
			}
			double avrcosts = totalcosts / (double)ttProvider.size();
			if(avrcosts < leastcost) {
				leastcost = avrcosts;
				bestroute = route;
			}
		}
		
		return bestroute;
	}
}
