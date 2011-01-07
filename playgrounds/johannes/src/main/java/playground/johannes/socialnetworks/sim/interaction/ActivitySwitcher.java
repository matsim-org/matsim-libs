/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySwitcher.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;

/**
 * @author illenberger
 *
 */
public class ActivitySwitcher {

	private Random random = new Random();
	
	private List<String> allowedTypes;

	private Network network;
	
	private LeastCostPathCalculator router;
	
	public ActivitySwitcher(final TravelTime travelTime) {
		allowedTypes = new ArrayList<String>(3);
		allowedTypes.add("home");
		allowedTypes.add("shop");
		allowedTypes.add("leisure");
		
		router = new Dijkstra(network, new TravelCost() {
			
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return travelTime.getLinkTravelTime(link, time);
			}
		}, travelTime);
	}
	
	private boolean isAllowedType(String type) {
		for(String aType : allowedTypes) {
			if(type.equalsIgnoreCase(aType))
				return true;
		}
		return false;
	}
	
	public boolean switchActivities(Plan plan) {
		int numActs = Math.max(0, (plan.getPlanElements().size() - 1) / 2);
		if (numActs > 1) {
			int idx1 = random.nextInt(numActs - 1);
			int idx2 = random.nextInt(numActs - 1);
			
			if(idx1 == idx2)
				return false;
			
			if(idx1 == 0 || idx2 == 0)
				return false;
			
			if(idx1 == numActs-1 || idx2 == numActs-1)
				return false;
			
			idx1 = idx1 * 2;
			idx2 = idx2 * 2;
			
			Activity act1 = (Activity) plan.getPlanElements().get(idx1);
			Activity act2 = (Activity) plan.getPlanElements().get(idx2);
			
			if(!isAllowedType(act1.getType()) || !isAllowedType(act2.getType()))
				return false;
				
			double endTime1 = act1.getEndTime();
			double entTime2 = act2.getEndTime();
			/*
			 * first act
			 */
			plan.getPlanElements().set(idx1, act2);
			act2.setEndTime(endTime1);
			/*
			 * toLeg
			 */
			calcRoute((Activity)plan.getPlanElements().get(idx1-2), act2, (Leg)plan.getPlanElements().get(idx1-1));
			/*
			 * fromLeg
			 */
			calcRoute(act2, (Activity)plan.getPlanElements().get(idx1+2), (Leg)plan.getPlanElements().get(idx1+1));
			/*
			 * second Act
			 */
			plan.getPlanElements().set(idx2, act1);
			act1.setEndTime(entTime2);
			/*
			 * toLeg
			 */
			if(idx2 != idx1+2) {
				calcRoute((Activity)plan.getPlanElements().get(idx2-2), act1, (Leg)plan.getPlanElements().get(idx2-1));
			}
			/*
			 * fromLeg
			 */
			calcRoute(act1, (Activity)plan.getPlanElements().get(idx2+2), (Leg)plan.getPlanElements().get(idx2+1));
		}
		return true;
	}
	
	private void calcRoute(Activity prev, Activity next, Leg leg) {
		Id link1 = prev.getLinkId();
		Id link2 = next.getLinkId();

		Node node1 = network.getLinks().get(link1).getFromNode();
		Node node2 = network.getLinks().get(link2).getToNode();
		
		Path path = router.calcLeastCostPath(node1, node2, prev.getEndTime());
		
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(link1, link2);
		route.setLinkIds(link1, NetworkUtils.getLinkIds(path.links), link2);
		leg.setRoute(route);		
	}
}
