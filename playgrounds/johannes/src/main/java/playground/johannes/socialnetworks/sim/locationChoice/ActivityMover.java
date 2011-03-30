/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityMover.java
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
package playground.johannes.socialnetworks.sim.locationChoice;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;

/**
 * @author illenberger
 *
 */
public class ActivityMover {

	private final PopulationFactory factory;
	
	private final LeastCostPathCalculator router;
	
	private final Network network;
	
	public ActivityMover(PopulationFactory factory, LeastCostPathCalculator router, Network network) {
		this.factory = factory;
		this.router = router;
		this.network = network;
	}
	
	public void moveActivity(Plan plan, int idx, Id newLink) {
		Activity act = (Activity) plan.getPlanElements().get(idx);
		
		Activity newAct = factory.createActivityFromLinkId(act.getType(), newLink);
		
		if(Double.isInfinite(act.getEndTime())) {
			act.setEndTime(act.getStartTime() + ((ActivityImpl) act).getMaximumDuration());
		}
		
		newAct.setEndTime(act.getEndTime());
		
		Activity prev = (Activity) plan.getPlanElements().get(idx - 2);
		Leg toLeg = (Leg)plan.getPlanElements().get(idx - 1);
		Leg fromLeg = (Leg)plan.getPlanElements().get(idx + 1);
		Activity next = (Activity) plan.getPlanElements().get(idx + 2);
		
		calcRoute(prev, newAct, toLeg);
		calcRoute(newAct, next, fromLeg);

		plan.getPlanElements().set(idx, newAct);
	}
	
	private double calcRoute(Activity prev, Activity next, Leg leg) {
		Id link1 = prev.getLinkId();
		Id link2 = next.getLinkId();

		Node node1 = network.getLinks().get(link1).getToNode();
		Node node2 = network.getLinks().get(link2).getToNode();
		
		Path path = router.calcLeastCostPath(node1, node2, prev.getEndTime());
		
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(link1, link2);
		route.setLinkIds(link1, NetworkUtils.getLinkIds(path.links), link2);
		leg.setRoute(route);
		leg.setTravelTime(path.travelTime);
		
		return path.travelTime;
	}
}
