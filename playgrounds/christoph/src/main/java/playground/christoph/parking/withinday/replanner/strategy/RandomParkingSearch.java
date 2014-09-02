/* *********************************************************************** *
 * project: org.matsim.*
 * RandomParkingSearch.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.withinday.replanner.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;

public class RandomParkingSearch implements ParkingSearchStrategy {

	private static final Logger log = Logger.getLogger(RandomParkingSearch.class);
	
	private final Random random;
	private final Network network;
	
	public RandomParkingSearch(Network network) {
		this.network = network;
		this.random = MatsimRandom.getLocalInstance();
	}
	
	@Override
	public void applySearchStrategy(MobsimAgent agent, double time) {
		
		/*
		 * Set seed in random number generator to create deterministic results.
		 * We need the time here since otherwise an agent would always make
		 * the same decision at an intersection.
		 */
		Id currentLinkId = agent.getCurrentLinkId();
		
		Leg leg = WithinDayAgentUtils.getModifiableCurrentLeg(agent);

		Link currentLink = this.network.getLinks().get(currentLinkId);

		int routeIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);
		
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		
		Id<Link> startLink = route.getStartLinkId();
		List<Id<Link>> links = null;
		Id<Link> endLink = route.getEndLinkId();
			
		// check whether the car is at the route's start link
		if (routeIndex == 0) {
			
			// if the route ends at the same link
			if (startLink.equals(endLink)) {
				Link l = randomNextLink(currentLink, agent, time);
				links  = new ArrayList<Id<Link>>(route.getLinkIds()); // create a copy that can be modified
				links.add(l.getId());
				
				log.warn("Car trip ends as the same link as it started - this should not happen since " + 
						"such trips should be replaced by walk trips!");
			} else {
				// nothing to do here since more links available in the route
			}
		}
		// end link
		else if (routeIndex == route.getLinkIds().size() + 1) {
			links  = new ArrayList<Id<Link>>(route.getLinkIds()); // create a copy that can be modified
			links.add(endLink);
			endLink = randomNextLink(currentLink, agent, time).getId();
		}
		// link in between
		else {
			// nothing to do here since more links available in the route
			return;
		}
		
		// update agent's route
		route.setLinkIds(startLink, links, endLink);
	}

	private Link randomNextLink(Link link, MobsimAgent agent, double time) {
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());

		random.setSeed(link.getId().hashCode() + agent.getId().hashCode() + (long) time);
		int i = random.nextInt(links.size());
		return links.get(i);
	}

}