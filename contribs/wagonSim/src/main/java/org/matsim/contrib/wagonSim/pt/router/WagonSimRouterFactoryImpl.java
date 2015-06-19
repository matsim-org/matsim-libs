/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.wagonSim.pt.router;

import org.apache.log4j.Logger;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.*;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.ObjectAttributes;

import javax.inject.Provider;
import java.util.LinkedList;
import java.util.List;

/**
 * @author droeder
 *
 */
public final class WagonSimRouterFactoryImpl implements Provider<TransitRouter> {

	private static final Logger log = Logger
			.getLogger(WagonSimRouterFactoryImpl.class);
	private WagonSimVehicleLoadListener vehLoad;
	private TransitRouterConfig config;
	private TransitSchedule schedule;
	private TransitRouterNetwork routerNetwork;
	private PreparedTransitSchedule preparedTransitSchedule;
	private ObjectAttributes locomotiveAttribs;
	private ObjectAttributes wagonAttribs;

	public WagonSimRouterFactoryImpl(WagonSimVehicleLoadListener vehLoad, 
				TransitSchedule schedule, 
				TransitRouterConfig config,
				ObjectAttributes wagonAttribs,
				ObjectAttributes locomotiveAttribs) {
		this.vehLoad = vehLoad;
		this.schedule = schedule;
		this.config = config;
		this.routerNetwork = createRouterNetwork(this.schedule, this.config.getBeelineWalkConnectionDistance());
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
		this.locomotiveAttribs = locomotiveAttribs;
		this.wagonAttribs = wagonAttribs;
	}

	@Override
	public TransitRouter get() {
		WagonSimRouterNetworkTravelDistutilityAndTravelTime tt = 
				new WagonSimRouterNetworkTravelDistutilityAndTravelTime(
						config, 
						preparedTransitSchedule, 
						vehLoad.getLoadOfLastIter(),
						this.locomotiveAttribs,
						this.wagonAttribs);
		return new TransitRouterImpl(config, preparedTransitSchedule, routerNetwork, tt, tt);
	}
	
	/**
	 * This is c&p from {@link TransitRouterNetwork#createFromSchedule(TransitSchedule, double)}. However,
	 * this method creates a {@link TransitRouterNetworkLink} per Departure (not only per route).
	 * 
	 * @param schedule
	 * @param maxBeelineWalkConnectionDistance
	 * @return
	 */
	public static TransitRouterNetwork createRouterNetwork(final TransitSchedule schedule, final double maxBeelineWalkConnectionDistance) {
		log.info("start creating transit network");
		final TransitRouterNetwork network = new TransitRouterNetwork();
		final Counter linkCounter = new Counter(" link #");
		final Counter nodeCounter = new Counter(" node #");
		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for(Departure d: route.getDepartures().values()){
					TransitRouterNetworkNode prevNode = null;
					TransitRoute r = schedule.getFactory().createTransitRoute(route.getId(), route.getRoute(), route.getStops(), route.getTransportMode());
					r.addDeparture(d);
					for (TransitRouteStop stop : route.getStops()) {
						TransitRouterNetworkNode node = network.createNode(stop, r, line);
						nodeCounter.incCounter();
						if (prevNode != null) {
							network.createLink(prevNode, node, r, line);
							linkCounter.incCounter();
						}
						prevNode = node;
					}
				}
			}
		}
		network.finishInit(); // not nice to call "finishInit" here before we added all links...
		// in my view, it would be possible to completely do without finishInit: do the
		// additions to the central data structures as items come in, not near the end.  I would
		// prefer that because nobody could forget the "finishInit".  kai, apr'10
		// well, not really. finishInit creates the quadtree, for this, the extent must be known,
		// which is not at the very start, so the quadtree data structure cannot be updated as
		// links come in. mrieser, dec'10
		log.info("add transfer links");

		List<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>> toBeAdded = new LinkedList<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>>();
		// connect all stops with walking links if they're located less than beelineWalkConnectionDistance from each other
		for (TransitRouterNetworkNode node : network.getNodes().values()) {
			if (node.getInLinks().size() > 0) { // only add links from this node to other nodes if agents actually can arrive here
				for (TransitRouterNetworkNode node2 : network.getNearestNodes(node.stop.getStopFacility().getCoord(), maxBeelineWalkConnectionDistance)) {
					if ((node != node2) && (node2.getOutLinks().size() > 0)) { // only add links to other nodes when agents can depart there
						if ((node.line != node2.line) || (node.stop.getStopFacility() != node2.stop.getStopFacility())) {
							// do not yet add them to the network, as this would change in/out-links
							toBeAdded.add(new Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>(node, node2));
						}
					}
				}
			}
		}
		log.info(toBeAdded.size() + " transfer links to be added.");
		for (Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode> tuple : toBeAdded) {
			network.createLink(tuple.getFirst(), tuple.getSecond(), null, null);
			linkCounter.incCounter();
		}

		log.info("transit router network statistics:");
		log.info(" # nodes: " + network.getNodes().size());
		log.info(" # links total:     " + network.getLinks().size());
		log.info(" # transfer links:  " + toBeAdded.size());

		return network;
	}
}

