/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityRandomizer.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
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
public class ActivityRandomizer {

	private final List<Link> links;
	
	private final Random random;
	
	private final PopulationFactory factory;
	
	private final LeastCostPathCalculator router;
	
	private final Network network;
	
	public ActivityRandomizer(Network network, Random random, PopulationFactory factory, LeastCostPathCalculator router) {
		this.random = random;
		this.network = network;
		this.factory = factory;
		this.router = router;
		links = new ArrayList<Link>(network.getLinks().values());
	}
	
	public void randomize(Population population) {
		int n = 0;
		int l = 0;
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				if (plan.getPlanElements().size() > 2) {
					for (int i = 2; i < plan.getPlanElements().size(); i += 2) {
						Activity act = (Activity) plan.getPlanElements().get(i);
						if (act.getType().startsWith("l")) {
							if(Math.random() < 0.33) {
							Link newDest = links.get(random.nextInt(links.size()));
							
							Activity newAct = factory.createActivityFromLinkId(act.getType(), newDest.getId());
							if(Double.isInfinite(act.getEndTime())) {
								act.setEndTime(act.getStartTime() + ((ActivityImpl) act).getMaximumDuration());
							}
							newAct.setEndTime(act.getEndTime());
							
							Activity prev = (Activity) plan.getPlanElements().get(i - 2);
							Leg toLeg = (Leg)plan.getPlanElements().get(i - 1);
							Leg fromLeg = (Leg)plan.getPlanElements().get(i + 1);
							Activity next = (Activity) plan.getPlanElements().get(i + 2);
							
							
							calcRoute(prev, newAct, toLeg);
							calcRoute(newAct, next, fromLeg);

							plan.getPlanElements().set(i, newAct);
							
							l++;
							}
						}
					}
				}
			}
			n++;
			if(n % 100 == 0) {
				System.out.println(String.format("Processed %1$s plans, randomized %2$s leisure acts...", n, l));
			}
		}
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
