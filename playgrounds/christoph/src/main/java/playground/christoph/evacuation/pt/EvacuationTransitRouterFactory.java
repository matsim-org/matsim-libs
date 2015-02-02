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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.FastTransitMultiNodeDijkstra;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitTravelDisutilityWrapper;
import org.matsim.pt.router.util.FastTransitDijkstraFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.christoph.evacuation.config.EvacuationConfig;

/**
 * @author cdobler
 */
public class EvacuationTransitRouterFactory implements TransitRouterFactory {

	static final Logger log = Logger.getLogger(EvacuationTransitRouterFactory.class);
	
	private final TravelTime walkTravelTime;
	private final TransitRouterConfig routerConfig;
	private final TransitRouterNetwork routerNetwork;
	private final EvacuationTransitRouterNetworkTravelTimeAndDisutility ttCalculator;
	private final PreparedTransitSchedule departureTimeCache;
	private final FastTransitDijkstraFactory dijkstraFactory;
	
	private final Coord center = EvacuationConfig.centerCoord;
	private final double innerRadius = EvacuationConfig.innerRadius;
	private final double outerRadius = EvacuationConfig.outerRadius;
	private final Collection<TransitRouterNetworkNode> exitNodes;
	
	public EvacuationTransitRouterFactory(final Config config, final TravelTime walkTravelTime, final TransitSchedule transitSchedule,
			final TransitRouterNetwork routerNetwork, final TransitRouterConfig routerConfig) {
		this.walkTravelTime = walkTravelTime;
		this.routerConfig = routerConfig;
		this.routerNetwork = routerNetwork;
		this.dijkstraFactory = new FastTransitDijkstraFactory();
		this.departureTimeCache = new PreparedTransitSchedule(transitSchedule);
		
		double beelineDistanceFactor =
                config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor() ;
//				config.plansCalcRoute().getBeelineDistanceFactor();
		this.ttCalculator = new EvacuationTransitRouterNetworkTravelTimeAndDisutility(this.routerConfig, this.departureTimeCache, walkTravelTime,
				beelineDistanceFactor);
		this.exitNodes = new ArrayList<TransitRouterNetworkNode>();
		identifyExitNodes();
		
		/*
		 * We have to create an EvacuationTransitRouter here. This triggers the creation of 
		 * the RoutingNetwork inside the dijkstraFactory, which is not thread-safe. Later,
		 * createTransitRouter() is called concurrently from parallel running threads. Therefore,
		 * we ensure that the RoutingNetwork is already created and re-used later.
		 */
		this.createTransitRouter();
	}

	@Override
	public EvacuationTransitRouter createTransitRouter() {
		TransitTravelDisutilityWrapper wrapper = new TransitTravelDisutilityWrapper(this.ttCalculator);
		FastTransitMultiNodeDijkstra dijkstra = (FastTransitMultiNodeDijkstra) dijkstraFactory.createPathCalculator(this.routerNetwork, 
				wrapper, this.ttCalculator);
		
		return new EvacuationTransitRouter(this.routerConfig, this.routerNetwork, this.ttCalculator, this.exitNodes, dijkstra);
	}
	
	/**
	 * All nodes in the router network which are located inside the ring
	 * with r >= innerRadius and r <= outerRadius are identified as exit
	 * nodes. From there, agents can reach the rescue facility immediately.
	 */
	private void identifyExitNodes() {
		
		for (Node node : this.routerNetwork.getNodes().values()) {
			double distance = CoordUtils.calcDistance(center, node.getCoord());
			
			if (distance >= innerRadius && distance <= outerRadius) exitNodes.add((TransitRouterNetworkNode) node);
		}
		log.info("Found " + exitNodes.size() + " exit nodes in TransitRouterNetwork.");
	}
}
