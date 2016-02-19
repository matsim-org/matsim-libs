/* *********************************************************************** *
 * project: org.matsim.*
 * ThinTransitRouterNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.matsim2030.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a "thin" transit router from a public transport schedule.
 * Based on code by Christoph Dobler (with a lot of copy-paste...)
 *
 * @author thibautd
 */
public class ThinTransitRouterNetwork {
	private static final Logger log =
		Logger.getLogger(ThinTransitRouterNetwork.class);

	public static void main(final String[] args) {
		final String configFile = args[ 0 ];
		final String outputFile = args[ 1 ];

		final Config config = ConfigUtils.loadConfig( configFile );
		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.network().setInputFile(null);
		config.transit().setUseTransit(true);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
	
		final TransitRouterConfig transitRouterConfig =
			new TransitRouterConfig(
					config.planCalcScore(),
					config.plansCalcRoute(),
					config.transitRouter(),
					config.vspExperimental());
		final TransitRouterNetwork network =
			TransitRouterNetwork.createFromSchedule(
					scenario.getTransitSchedule(), 
					transitRouterConfig.getBeelineWalkConnectionDistance());
	
		new RemoveRedundantLinks().run( network );
		new RemoveRedundantDistanceLinks().run( network );

		new TransitRouterNetworkWriter( network ).write( outputFile );
	}

	/**
	 * When a transit router network is created, walk links are added to allow
	 * agents to walk from one stop to another. However, if several transit lines
	 * stop at the same location, several links with length 0.0 are created.
	 * Most of them can be removed since it is not necessary that every node is
	 * connected to every other node. When removing links it is ensured that full
	 * connectivity is preserved. Moreover, some links might be added so be able
	 * to simplify the structure afterwards.
	 * 
	 * E.g.
	 * 		n
	 *     /|\
	 *    / | \
	 *   n--+--n
	 *    \ | /
	 *     \|/
	 *      n
	 * can be converted to:
	 *		n
	 *     /|\
	 *    / | \
	 *   n  |  n
	 *      |
	 *      |
	 *      n
	 * 
	 * The created route should have the same travel time since the travel time on
	 * the links should be 0.0 anyway. However, the created routes will be a bit different
	 * and the simulation might produce longer travel times since moving over a link
	 * costs at least 1.0 second.
	 */
	public static class RemoveRedundantLinks implements NetworkRunnable {

		private AtomicInteger nextLinkId;
		private Counter connectorLinks = new Counter("# created connector links: "); 
		
		@Override
		public void run(Network nw) {
			
			nextLinkId = new AtomicInteger(nw.getLinks().size());
			connectorLinks.reset();
			
			if (!(nw instanceof TransitRouterNetwork)) return;
			log.info("Removing redundant links with a length of 0.0...");
			
			TransitRouterNetwork network = (TransitRouterNetwork) nw;
			
			Counter nodesCounter = new Counter("# handled nodes: ");
			Counter linksCounter = new Counter("# removed links: ");
			Set<Id<Node>> handledNodes = new HashSet<>();
			for (TransitRouterNetworkNode node : network.getNodes().values()) {
				nodesCounter.incCounter();
				if (handledNodes.contains(node.getId())) continue;
								
				/*
				 * - Get all nodes that have the same coordinate.
				 * - Check whether connector links exist. If not, skip nodes.
				 * - Ensure that they are really full connected.
				 * - Remove all their in- and out links with length 0.0 except they connect them to the current node.
				 */
				Collection<TransitRouterNetworkNode> nodes = network.getNearestNodes(node.getCoord(), 0.0);
				
				boolean hasConnectorLinks = hasConnectorLinks(nodes);
				if (!hasConnectorLinks) {
					for (Node n : nodes) handledNodes.add(n.getId());
					continue;
				}
				
				ensureFullConnectivity(nw, nodes);
				
				for (TransitRouterNetworkNode node2 : nodes) {
					if (node2.getId().equals(node.getId())) continue;
					if (handledNodes.contains(node2.getId())) continue;
					
					Set<Link> linksToRemove = new HashSet<Link>();
					for (Link link : node2.getInLinks().values()) {
						if (link.getLength() == 0.0 && !link.getFromNode().getId().equals(node.getId())) {
							linksToRemove.add(link);
						}
					}
					for (Link link : node2.getOutLinks().values()) {
						if (link.getLength() == 0.0 && !link.getToNode().getId().equals(node.getId())) {
							linksToRemove.add(link);
						}
					}
					
					for (Link link : linksToRemove) {
						// do not filter links which belong to the transit network
						if (((TransitRouterNetworkLink) link).getLine() != null) continue;
						
						link.getToNode().getInLinks().remove(link.getId());
						link.getFromNode().getOutLinks().remove(link.getId());
						network.getLinks().remove(link.getId());
						linksCounter.incCounter();
					}

					handledNodes.add(node2.getId());
				}
				
				handledNodes.add(node.getId());
			}
			nodesCounter.printCounter();
			linksCounter.printCounter();
			connectorLinks.printCounter();
			log.info("done.");
		}
		
		private boolean hasConnectorLinks(Collection<TransitRouterNetworkNode> nodes) {
			
			for (Node node : nodes) {
				for (Link link : node.getInLinks().values()) {
					// if the link does not belong to the transit network, it is a connector link
					if (((TransitRouterNetworkLink) link).getLine() == null) return true;
				}
				for (Link link : node.getOutLinks().values()) {
					// if the link does not belong to the transit network, it is a connector link
					if (((TransitRouterNetworkLink) link).getLine() == null) return true;
				}
			}
			return false;
		}
		
		@SuppressWarnings("unchecked")
		private void ensureFullConnectivity(Network network, Collection<TransitRouterNetworkNode> nodes) {
			
			for (TransitRouterNetworkNode node : nodes) {
				for (TransitRouterNetworkNode otherNode : nodes) {
					if (node.getId().equals(otherNode.getId())) continue;

					boolean linkExists;

					// check in-links
					linkExists = false;
					for (Link inLink : node.getInLinks().values()) {
						// ignore links which belong to the transit network
						if (((TransitRouterNetworkLink) inLink).getLine() != null) continue;
						
						if (inLink.getFromNode().getId().equals(otherNode.getId())) {
							linkExists = true;
							break;
						}
					}
					if (!linkExists) {
						int nextId = nextLinkId.getAndIncrement();
						Id<Link> linkId = Id.create(nextId, Link.class);
						TransitRouterNetworkLink link = new TransitRouterNetworkLink(linkId, otherNode, node, null, null, 0.0);
						
						((Map<Id<Link>, TransitRouterNetworkLink>) network.getLinks()).put(linkId, link);
						((Map<Id<Link>, TransitRouterNetworkLink>) node.getInLinks()).put(link.getId(), link);
						((Map<Id<Link>, TransitRouterNetworkLink>) otherNode.getOutLinks()).put(link.getId(), link);
						connectorLinks.incCounter();
					}
					
					// check out-links
					linkExists = false;
					for (Link outLink : node.getOutLinks().values()) {
						// ignore links which belong to the transit network
						if (((TransitRouterNetworkLink) outLink).getLine() != null) continue;
						
						if (outLink.getToNode().getId().equals(otherNode.getId())) {
							linkExists = true;
							break;
						}
					}
					if (!linkExists) {
						int nextId = nextLinkId.getAndIncrement();
						Id<Link> linkId = Id.create(nextId, Link.class);
						TransitRouterNetworkLink link = new TransitRouterNetworkLink(linkId, node, otherNode, null, null, 0.0);
						
						((Map<Id<Link>, TransitRouterNetworkLink>) network.getLinks()).put(linkId, link);
						((Map<Id<Link>, TransitRouterNetworkLink>) node.getOutLinks()).put(link.getId(), link);
						((Map<Id<Link>, TransitRouterNetworkLink>) otherNode.getInLinks()).put(link.getId(), link);
						connectorLinks.incCounter();
					}
				}
			}
		}
	}
		
	/**
	 * In a transit router network several nodes might have identical coordinates.
	 * Those links are (or at least should be!) connected by links with length 0.0.
	 * 
	 * If another link is located close to them, it is connected to each of them with
	 * walk links which allow passengers to walk from one transit stop to another one.
	 * 
	 * Since only one of them is required, all except one are removed from the network.
	 */
	public static class RemoveRedundantDistanceLinks implements NetworkRunnable {

		@Override
		public void run(Network nw) {
			if (!(nw instanceof TransitRouterNetwork)) return;
			log.info("Removing redundant links with a length > 0.0...");
			
			Counter removedLinksCounter = new Counter("# removed links: ");
			Counter processedLinksCounter = new Counter("# processed links: ");
			
			Set<CoordinatePair> linksToKeep = new HashSet<CoordinatePair>();
			TreeSet<Link> walkLinks = new TreeSet<Link>(new LinkComparator());
			for (Link link : nw.getLinks().values()) { 
				
				/*
				 * - do not filter links with length 0.0 - they have already been processed!
				 * - do not filter links which belong to the transit network
				 */
				if (link.getLength() > 0.0 && ((TransitRouterNetworkLink) link).getLine() == null) walkLinks.add(link);			
				else processedLinksCounter.incCounter();
			}
			processedLinksCounter.printCounter();
			
			Iterator<? extends Link> iter = walkLinks.iterator();
			while (iter.hasNext()) {
				Link link = iter.next();
				
				Coord fromCoord = link.getFromNode().getCoord();
				Coord toCoord = link.getToNode().getCoord();
				CoordinatePair cp = new CoordinatePair(fromCoord, toCoord);
				
				if (!linksToKeep.add(cp)) {
					nw.getLinks().remove(link.getId());
					link.getToNode().getInLinks().remove(link.getId());
					link.getFromNode().getOutLinks().remove(link.getId());
					removedLinksCounter.incCounter();
				}
				processedLinksCounter.incCounter();
			}			
			removedLinksCounter.printCounter();
			processedLinksCounter.printCounter();
			log.info("done.");
		}
		
		private static class CoordinatePair {

			private final Coord from;
			private final Coord to;
			
			public CoordinatePair(Coord from, Coord to) {
				this.from = from;
				this.to = to;
			}
			
			@Override
			public boolean equals(Object object) {
				if (!(object instanceof CoordinatePair)) return false;
				else {
					CoordinatePair cp = (CoordinatePair) object;
					if (cp.from.equals(from) && cp.to.equals(to)) return true;
					else return false;
				}
			}
			
			@Override
			public int hashCode() {
				return (int)(((((long) from.hashCode()) << 32) | to.hashCode()) % Integer.MAX_VALUE);
			}
		}
		
		private static class LinkComparator implements Comparator<Link> {

			@Override
			public int compare(Link l1, Link l2) {
				Id<Node> from1 = l1.getFromNode().getId();
				Id<Node> from2 = l2.getFromNode().getId();
				
				int c = from1.compareTo(from2);
				if (c == 0) {
					Id<Node> to1 = l1.getToNode().getId();
					Id<Node> to2 = l2.getToNode().getId();
					return to1.compareTo(to2);
				} else {
					return c;
				}
			}
		}
	}

}

