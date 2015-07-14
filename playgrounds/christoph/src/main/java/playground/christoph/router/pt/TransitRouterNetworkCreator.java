/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkCreator.java
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

package playground.christoph.router.pt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.christoph.evacuation.pt.TransitRouterNetworkWriter;

public class TransitRouterNetworkCreator {

	private final static Logger log = Logger.getLogger(TransitRouterNetwork.class);

	public static void main(String args[]) {

		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		
		config.transit().setVehiclesFile("../../matsim/mysimulations/matsim2030/input/uvek2005vehicles_final.xml");
		config.transit().setTransitScheduleFile("../../matsim/mysimulations/matsim2030/input/uvek2005schedule_adjusted.xml.gz");
//		config.transit().setVehiclesFile("../../matsim/mysimulations/census2000V2/input_10pct/OeV/transitVehicles.ch.xml.gz");
//		config.transit().setTransitScheduleFile("../../matsim/mysimulations/census2000V2/input_10pct/OeV/schedule.20120117.ch-edited.xml.gz");
		
//		config.transitRouter().setMaxBeelineWalkConnectionDistance(20000.0);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TransitRouterNetwork network = createFromSchedule(scenario.getTransitSchedule(), config.transitRouter().getMaxBeelineWalkConnectionDistance());
//		TransitRouterNetwork network = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), config.transitRouter().getMaxBeelineWalkConnectionDistance());
		
		new TransitRouterNetworkWriter(network).write("../../matsim/mysimulations/matsim2030/input/transitRouterNetwork.xml.gz");
		new NetworkWriter(network).write("../../matsim/mysimulations/matsim2030/input/transitRouterNetworkAsNetwork.xml.gz");
	}
	
	public static TransitRouterNetwork createFromSchedule(final TransitSchedule schedule, final double maxBeelineWalkConnectionDistance) {
		
		log.info("start creating transit network");
		final TransitRouterNetwork network = new TransitRouterNetwork();
		final Counter linkCounter = new Counter(" link #");
		final Counter nodeCounter = new Counter(" node #");
		// build nodes and links connecting the nodes according to the transit routes
		log.info("\tcreating nodes and links from transit lines");
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = network.createNode(stop, route, line);
					nodeCounter.incCounter();
					if (prevNode != null) {
						network.createLink(prevNode, node, route, line);
						linkCounter.incCounter();
					}
					prevNode = node;
				}
			}
		}
		log.info("\tdone");
		
		/*
		 * So far, use a very simple approach where only nodes with exact matching coordinates
		 * are identified. Instead, one could also try to find clustered nodes within a certain distance.
		 */
		int transferLinksCounter = 0;
		log.info("\tcreating transfer nodes");
		Map<Coord, List<TransitRouterNetworkNode>> nodesMap = new LinkedHashMap<Coord, List<TransitRouterNetworkNode>>();
		for (TransitRouterNetworkNode node : network.getNodes().values()) {
			List<TransitRouterNetworkNode> list = nodesMap.get(node.getCoord());
			if (list == null) {
				list = new ArrayList<TransitRouterNetworkNode>();
				nodesMap.put(node.getCoord(), list);
			}
			list.add(node);
		}
		log.info("\t\tfound " + nodesMap.size() + " different node coordinates");
		List<TransitRouterNetworkNode> transferNodes = new ArrayList<TransitRouterNetworkNode>();
		for (Coord coord : nodesMap.keySet()) {
			/*
			 * TODO: 
			 * Solve this: we would need another kind of node since TransitRouterNetworkNodes
			 * get their coordinate from their TransitRouteStop.
			 */
			TransitRouterNetworkNode node = network.createNode(new DummyTransitRouteStop(coord), 
					new DummyTransitRoute(), new DummyTransitLine());
			transferNodes.add(node);
			nodeCounter.incCounter();			
			
			// create walk links between transfer node and transit stops
			List<TransitRouterNetworkNode> nodes = nodesMap.get(coord);
			for (TransitRouterNetworkNode node2 : nodes) {
				network.createLink(node, node2, null, null);
				linkCounter.incCounter();
				transferLinksCounter++;
				
				network.createLink(node2, node, null, null);
				linkCounter.incCounter();
				transferLinksCounter++;
			}
		}
		QuadTree<TransitRouterNetworkNode> transferNodesQuadTree = createQuadTree(transferNodes);
		log.info("\tdone");
				
		network.finishInit(); // not nice to call "finishInit" here before we added all links...
		// in my view, it would be possible to completely do without finishInit: do the
		// additions to the central data structures as items come in, not near the end.  I would
		// prefer that because nobody could forget the "finishInit".  kai, apr'10
		// well, not really. finishInit creates the quadtree, for this, the extent must be known,
		// which is not at the very start, so the quadtree data structure cannot be updated as
		// links come in. mrieser, dec'10
		log.info("add transfer links");
		
		List<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>> toBeAdded = new LinkedList<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>>();
		// connect all transfer nodes with walking links if they're located less than beelineWalkConnectionDistance from each other
		for (TransitRouterNetworkNode fromNode : transferNodes) {
			Coord fromCoord = fromNode.getCoord();
			Collection<TransitRouterNetworkNode> nearestTransferNodes = transferNodesQuadTree.get(fromCoord.getX(), fromCoord.getY(), maxBeelineWalkConnectionDistance);
			for (TransitRouterNetworkNode toNode : nearestTransferNodes) {
				if (fromNode != toNode) {
					// do not yet add them to the network, as this would change in/out-links
					toBeAdded.add(new Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>(fromNode, toNode));
				}
			}
		}
		log.info(toBeAdded.size() + " transfer links to be added.");
		for (Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode> tuple : toBeAdded) {
			network.createLink(tuple.getFirst(), tuple.getSecond(), null, null);
			linkCounter.incCounter();
		}

		log.info("transit router network statistics:");
		log.info(" # nodes total: " + network.getNodes().size());
		log.info(" # links total:     " + network.getLinks().size());
		log.info(" # transfer nodes:  " + transferNodes.size());
		log.info(" # transfer links between transfer nodes:  " + toBeAdded.size());
		log.info(" # transfer links between nodes and transfer nodes:  " + transferLinksCounter);

		return network;
	}
	
	private static QuadTree<TransitRouterNetworkNode> createQuadTree(Collection<TransitRouterNetworkNode> nodes) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for (TransitRouterNetworkNode node : nodes) {
			Coord c = node.getCoord();
			if (c.getX() < minX) {
				minX = c.getX();
			}
			if (c.getY() < minY) {
				minY = c.getY();
			}
			if (c.getX() > maxX) {
				maxX = c.getX();
			}
			if (c.getY() > maxY) {
				maxY = c.getY();
			}
		}
		
		QuadTree<TransitRouterNetworkNode> quadTree = new QuadTree<TransitRouterNetworkNode>(minX, minY, maxX, maxY);
		for (TransitRouterNetworkNode node : nodes) {
			Coord c = node.getCoord();
			quadTree.put(c.getX(), c.getY(), node);
		}
		return quadTree;
	}
	
	/*
	 * These four classes are just a workaround since TransitNetworkNodes get their coordinate
	 * always from their TransitRouteStop which gets it from its TransitStopFacility.
	 */
	private static class DummyTransitRouteStop implements TransitRouteStop {

		private final DummyTransitStopFacility dummyTransitStopFacility;
		
		public DummyTransitRouteStop(Coord coord) {
			this.dummyTransitStopFacility = new DummyTransitStopFacility(coord);
		}
		
		@Override
		public TransitStopFacility getStopFacility() {
			return this.dummyTransitStopFacility;
		}

		@Override
		public void setStopFacility(TransitStopFacility stopFacility) {
		}

		@Override
		public double getDepartureOffset() {
			return 0;
		}

		@Override
		public double getArrivalOffset() {
			return 0;
		}

		@Override
		public void setAwaitDepartureTime(boolean awaitDepartureTime) {
		}

		@Override
		public boolean isAwaitDepartureTime() {
			return false;
		}
	}
	
	private static class DummyTransitStopFacility implements TransitStopFacility {

		private static int nextInt = 0;
		
		private final Coord coord;
		private final Id<TransitStopFacility> id;
		
		public DummyTransitStopFacility(Coord coord) {
			this.coord = coord;
			this.id = Id.create("DummyTransitStopFacility_" + nextInt++, TransitStopFacility.class);
		}
		
		@Override
		public Id<Link> getLinkId() {
			return null;
		}

		@Override
		public Coord getCoord() {
			return this.coord;
		}

		@Override
		public Id<TransitStopFacility> getId() {
			return this.id;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return null;
		}

		@Override
		public boolean getIsBlockingLane() {
			return false;
		}

		@Override
		public void setLinkId(Id<Link> linkId) {
		}

		@Override
		public void setName(String name) {
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getStopPostAreaId() {
			return null;
		}

		@Override
		public void setStopPostAreaId(String stopPostAreaId) {
		}
	}
	
	private static class DummyTransitRoute implements TransitRoute {

		private static int nextInt = 0;
		
		private final Id<TransitRoute> id;
		
		public DummyTransitRoute() {
			this.id = Id.create("DummyTransitRoute_" + nextInt++, TransitRoute.class);
		}
		
		@Override
		public Id<TransitRoute> getId() {
			return this.id;
		}

		@Override
		public void setDescription(String description) {
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void setTransportMode(String mode) {
		}

		@Override
		public String getTransportMode() {
			return null;
		}

		@Override
		public void addDeparture(Departure departure) {
		}

		@Override
		public boolean removeDeparture(Departure departure) {
			return false;
		}

		@Override
		public Map<Id<Departure>, Departure> getDepartures() {
			return null;
		}

		@Override
		public NetworkRoute getRoute() {
			return null;
		}

		@Override
		public void setRoute(NetworkRoute route) {
		}

		@Override
		public List<TransitRouteStop> getStops() {
			return null;
		}

		@Override
		public TransitRouteStop getStop(TransitStopFacility stop) {
			return null;
		}
	}
	
	private static class DummyTransitLine implements TransitLine {

		private static int nextInt = 0;
		
		private final Id<TransitLine> id;
		
		public DummyTransitLine() {
			this.id = Id.create("DummyTransitLine_" + nextInt++, TransitLine.class);
		}
		
		@Override
		public Id<TransitLine> getId() {
			return this.id;
		}

		@Override
		public void addRoute(TransitRoute transitRoute) {
		}

		@Override
		public Map<Id<TransitRoute>, TransitRoute> getRoutes() {
			return null;
		}

		@Override
		public boolean removeRoute(TransitRoute route) {
			return false;
		}

		@Override
		public void setName(String name) {
		}

		@Override
		public String getName() {
			return null;
		}
	}
}
