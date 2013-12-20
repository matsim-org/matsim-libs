/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTransitRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.pt;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

import playground.christoph.evacuation.config.EvacuationConfig;

/**
 * @author cdobler
 */
public class EvacuationTransitRouterFactory implements TransitRouterFactory {

	static final Logger log = Logger.getLogger(EvacuationTransitRouterFactory.class);
	
	private final Config config;
	private final TravelTime walkTravelTime;
	private final TransitRouterConfig routerConfig;
	private final TransitRouterNetwork routerNetwork;
	private final EvacuationTransitRouterNetworkTravelTimeAndDisutility ttCalculator;
	private final PreparedTransitSchedule departureTimeCache;
	
	private final Coord center = EvacuationConfig.centerCoord;
	private final double innerRadius = EvacuationConfig.innerRadius;
	private final double outerRadius = EvacuationConfig.outerRadius;
	private final Collection<TransitRouterNetworkNode> exitNodes;
	
	public EvacuationTransitRouterFactory(final Config config, final TravelTime walkTravelTime, 
			final TransitRouterNetwork routerNetwork, final TransitRouterConfig routerConfig) {
		this.config = config;
		this.walkTravelTime = walkTravelTime;
		this.routerConfig = routerConfig;
		this.routerNetwork = routerNetwork;
		
		this.departureTimeCache = new PreparedTransitSchedule();
		this.ttCalculator = new EvacuationTransitRouterNetworkTravelTimeAndDisutility(this.routerConfig, this.departureTimeCache);
		this.exitNodes = new ArrayList<TransitRouterNetworkNode>();
		identifyExitNodes();
	}

	@Override
	public EvacuationTransitRouter createTransitRouter() {
		return new EvacuationTransitRouter(this.config, this.routerConfig, this.routerNetwork, this.ttCalculator, 
				this.ttCalculator, this.exitNodes, this.walkTravelTime);
	}
	
	/**
	 * All nodes in the router network which are located inside the ring
	 * with r >= innerRadius and r <= outerRadius are identified as exit
	 * nodes. From there, agents can reach the rescue facility immediately.
	 */
	private void identifyExitNodes() {
		
		for (Node node : routerNetwork.getNodes().values()) {
			double distance = CoordUtils.calcDistance(center, node.getCoord());
			
			if (distance >= innerRadius && distance <= outerRadius) exitNodes.add((TransitRouterNetworkNode) node);
		}
		log.info("Found " + exitNodes.size() + " exit nodes in TransitRouterNetwork.");
	}
}
