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

import java.util.LinkedList;
import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelTimeI;

/**
 * @author illenberger
 *
 */
public class EUTRouter implements LeastCostPathCalculator {

	public static int riskCount = 0;
	
	private KSPPenalty kspPenalty;
	
	private TravelTimeMemory ttProvider;
	
	private ArrowPrattRiskAversionI utilFunc;
	
	private FreespeedTravelTimeCost freeTravTimes = new FreespeedTravelTimeCost();
	
	public EUTRouter(NetworkLayer network, TravelTimeMemory provider, ArrowPrattRiskAversionI utilFunc) {
		ttProvider = provider;
		this.utilFunc = utilFunc;
		kspPenalty = new KSPPenalty(network);
		
		safeRoute = new Route();
		safeRoute.setRoute("2 3 5");
		riskyRoute = new Route();
		riskyRoute.setRoute("2 4 5");
		
	}
	
	public Route calcLeastCostPath(Node fromNode, Node toNode, double starttime) {
		return selectChoice(generateChoiceSet(fromNode, toNode, starttime), starttime);
	}

	protected List<Route> generateChoiceSet(Node departure, Node destination, double time) {
		return kspPenalty.getPaths(departure, destination, time, 2, freeTravTimes);
//		return kspPenalty.getPaths(departure, destination, time, 3, ttProvider.requestLinkCost());
	}
	
	protected Route selectChoice(List<Route> routes, double starttime) {
		Route bestroute = null;
		Route shortestroute = null;
		double leastcost = Double.MAX_VALUE;
		double shortesttravtime = Double.MAX_VALUE;
		
		for(Route route : routes) {
			double totaltraveltime = 0;
			double totaltravelcosts = 0;
			
			for(TravelTimeI traveltimes : ttProvider.getTravelTimes()) {
				double traveltime = calcTravTime(traveltimes, route, starttime);
				double travelcosts = utilFunc.evaluate(traveltime);
				totaltraveltime += traveltime;
				totaltravelcosts += travelcosts;
			}
			
			double avrcosts = totaltravelcosts / (double)ttProvider.getTravelTimes().size();
			double avrtravtime  = totaltraveltime / (double)ttProvider.getTravelTimes().size();
			
			if(avrcosts < leastcost) {
				leastcost = avrcosts;
				bestroute = route;
			}
			
			
			if(avrtravtime < shortesttravtime) {
				shortesttravtime = avrtravtime;
				shortestroute = route;
			}
			
			logcosts(route, avrcosts, avrtravtime, utilFunc.getTravelTime(avrcosts));
		}
		
		if(!shortestroute.getRoute().equals(bestroute.getRoute()))
			riskCount++;
		
		return bestroute;
	}
	
	private double calcTravTime(TravelTimeI traveltimes, Route route, double starttime) {
		double totaltt = 0;
		for(Link link : route.getLinkRoute()) {
				totaltt += traveltimes.getLinkTravelTime(link, starttime + totaltt); 
		}
		return totaltt;
	}
	
	private static Route safeRoute;
	
	private static Route riskyRoute;
	
	public static List<Double> safeCostsAvr = new LinkedList<Double>();
	
	public static List<Double> riskyCostsAvr = new LinkedList<Double>();
	
	public static List<Double> riskyTravTimeAvr = new LinkedList<Double>();
	
	public static List<Double> safeTravTimeAvr = new LinkedList<Double>();
	
	public static List<Double> safeCE = new LinkedList<Double>();
	
	public static List<Double> riskyCE = new LinkedList<Double>();
	
	synchronized static private void logcosts(Route route, double avrcost, double avrtt, double ce) { 
		if(route.getRoute().equals(safeRoute.getRoute())) {
			safeCostsAvr.add(avrcost);
			safeTravTimeAvr.add(avrtt);
			safeCE.add(ce);
		} else if(route.getRoute().equals(riskyRoute.getRoute())) {
			riskyCostsAvr.add(avrcost);
			riskyTravTimeAvr.add(avrtt);
			riskyCE.add(ce);
		}
	}
}
