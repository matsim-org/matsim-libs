/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.mapping.v2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.pseudoRouter.*;
import playground.polettif.publicTransitMapping.mapping.router.Router;

import java.util.*;

/**
 * Generates and calculates the pseudoTransitRoutes for all the queued
 * transit lines. If no route on the network can be found (or the
 * scheduleTransportMode should not be mapped to the network), artificial
 * links between link candidates are stored to be created later.
 *
 * @author polettif
 */
public class PseudoRoutingImplArtificial2 extends Thread {

	protected static Logger log = Logger.getLogger(PseudoRoutingImplArtificial2.class);

	private static Counter counterPseudoRouting = new Counter("route # ");
	private static int artificialId = 0;
	private static boolean warnMinTravelCost =true;

	private final PublicTransitMappingConfigGroup config;

	private final Map<String, Router> modeSeparatedRouters;
	private final LinkCandidateCreator linkCandidates;

	private Set<ArtificialLink> artificialLinks = new HashSet<>();

	private List<TransitLine> queue = new ArrayList<>();
	private PseudoSchedule threadPseudoSchedule = new PseudoScheduleImpl();

	private Map<String, LeastCostPathCalculator.Path> localStoredPaths = new HashMap<>();

	public PseudoRoutingImplArtificial2(PublicTransitMappingConfigGroup config, Map<String, Router> modeSeparatedRouters, LinkCandidateCreator linkCandidates) {
		this.config = config;
		this.modeSeparatedRouters = modeSeparatedRouters;
		this.linkCandidates = linkCandidates;
	}

	public void addTransitLineToQueue(TransitLine transitLine) {
		queue.add(transitLine);
	}

	public void run() {
		for(TransitLine transitLine : queue) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {

				String scheduleTransportMode = transitRoute.getTransportMode();

				Router modeRouter = modeSeparatedRouters.get(scheduleTransportMode);
				Network modeNetwork = modeRouter.getNetwork();

				/** [4.1]
				 * Initiate pseudoGraph and Dijkstra algorithm for the current transitRoute.
				 *
				 * In the pseudoGraph, all link candidates are represented as nodes and the
				 * network paths between link candidates are reduced to a representation edge
				 * only storing the travel cost. With the pseudoGraph, the best linkCandidate
				 * sequence can be calculated (using Dijkstra). From this sequence, the actual
				 * path on the network can be routed later on.
				 */
				PseudoGraph pseudoGraph = new PseudoGraphImpl();

				/** [4.2]
				 * Calculate the shortest paths between each pair of routeStops/ParentStopFacility
				 */
				List<TransitRouteStop> routeStops = transitRoute.getStops();
				for(int i = 0; i < routeStops.size() - 1; i++) {
					Set<LinkCandidate> linkCandidatesCurrent = linkCandidates.getLinkCandidates(routeStops.get(i).getStopFacility(), scheduleTransportMode);
					Set<LinkCandidate> linkCandidatesNext = linkCandidates.getLinkCandidates(routeStops.get(i + 1).getStopFacility(), scheduleTransportMode);

					double minTravelCost = modeRouter.getMinimalTravelCost(routeStops.get(i), routeStops.get(i + 1));
					double maxAllowedTravelCost = minTravelCost * config.getMaxTravelCostFactor();

					if(minTravelCost == 0 && warnMinTravelCost) {
						log.warn("There are stop pairs where the minTravelCost is 0. This can happen if departure and arrival time of two subsequent stops are identical. This message is only given once.");
						warnMinTravelCost = false;
					}

					/**
					 * Calculate the shortest path between all link candidates.
					 */
					for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
						for(LinkCandidate linkCandidateNext : linkCandidatesNext) {

							boolean useExistingNetworkLinks = false;
							double pathCost = 2 * maxAllowedTravelCost;
							/**
							 * if both link candidates are loop links we don't have to search
							 * a least cost path on the network
							 */
							if(!linkCandidateCurrent.isLoopLink() && !linkCandidateNext.isLoopLink()) {
								LeastCostPathCalculator.Path leastCostPath = null;
								Node nodeA = modeNetwork.getNodes().get(linkCandidateCurrent.getToNodeId());
								Node nodeB = modeNetwork.getNodes().get(linkCandidateNext.getFromNodeId());

								if(nodeA != null && nodeB != null) {
									String key = scheduleTransportMode + "--" + nodeA.toString() + "--" + nodeB.toString();
									if(!localStoredPaths.containsKey(key)) {
										leastCostPath = modeRouter.calcLeastCostPath(nodeA, nodeB);
										localStoredPaths.put(key, leastCostPath);
									} else {
										leastCostPath = localStoredPaths.get(key);
									}
								}

								// path is null if links are on separate networks
								if(leastCostPath != null) {
									pathCost = leastCostPath.travelCost;
									// if both links are the same, cost should get higher
									if(linkCandidateCurrent.getLinkId().equals(linkCandidateNext.getLinkId())) {
										pathCost *= 4;
									}
								}

								useExistingNetworkLinks = pathCost < maxAllowedTravelCost;
							}

							if(useExistingNetworkLinks) {
								pseudoGraph.addEdge(i, routeStops.get(i), linkCandidateCurrent, routeStops.get(i+1), linkCandidateNext, pathCost);
							}
							/** [.]
							 * Create artificial links between two routeStops if:
							 * 	 - scheduleMode should use artificial links
							 *   - one of the two stops is outside the network area
							 *
							 * Artificial links are created between all LinkCandidates
							 * (usually this means between one dummy link for the stop
							 * facility and the other linkCandidates).
							 */
							else {
								double freespeed = modeRouter.getArtificialLinkFreeSpeed(maxAllowedTravelCost, linkCandidateCurrent, linkCandidateNext);
								double length = modeRouter.getArtificialLinkLength(maxAllowedTravelCost, linkCandidateCurrent, linkCandidateNext);
								ArtificialLink artificialLink = new ArtificialLinkImpl(linkCandidateCurrent, linkCandidateNext, freespeed, length);
								artificialLinks.add(artificialLink);

								double artificialEdgeWeight = maxAllowedTravelCost - 0.5 * linkCandidateCurrent.getLinkTravelCost() - 0.5 * linkCandidateNext.getLinkTravelCost();

								pseudoGraph.addEdge(i, routeStops.get(i), linkCandidateCurrent, routeStops.get(i+1), linkCandidateNext, artificialEdgeWeight);
							}
						}
					}
				} // - routeStop loop

				/** [4.3]
				 * Build pseudo network and find shortest path using dijkstra
				 */
				pseudoGraph.addDummyEdges(routeStops,
						linkCandidates.getLinkCandidates(routeStops.get(0).getStopFacility(), scheduleTransportMode),
						linkCandidates.getLinkCandidates(routeStops.get(routeStops.size() - 1).getStopFacility(), scheduleTransportMode));

				// run Dijkstra
				List<PseudoRouteStop> pseudoPath = pseudoGraph.getLeastCostStopSequence();

				if(pseudoPath == null) {
					log.warn("PseudoGraph has no path from SOURCE to DESTINATION for transit route " + transitRoute.getId() + " from \"" + routeStops.get(0).getStopFacility().getName() + "\" to \"" + routeStops.get(routeStops.size() - 1).getStopFacility().getName() + "\"");
				} else {
					threadPseudoSchedule.addPseudoRoute(transitLine, transitRoute, pseudoPath);
				}
				increaseCounter();
			}
		}
	}

	@Deprecated
	private Collection<ArtificialLink> getNecessaryArtificialLinks(Map<Tuple<Id<Node>, Id<Node>>, ArtificialLink> all, List<PseudoRouteStop> pseudoPath) {
		List<ArtificialLink> set = new ArrayList<>();
		for(int i = 0; i < pseudoPath.size() - 2; i++) {
			ArtificialLink val = all.get(new Tuple<>(pseudoPath.get(i).getLinkCandidate().getToNodeId(), pseudoPath.get(i + 1).getLinkCandidate().getFromNodeId()));
			if(val != null) set.add(val);
		}
		return set;
	}

	/**
	 * @return a pseudo schedule generated during run()
	 */
	public PseudoSchedule getPseudoSchedule() {
		return threadPseudoSchedule;
	}

	/**
	 * Adds the artificial links to the network. Not thread safe.
	 */
	public void addArtificialLinks(Network network) {
		Map<Tuple<Id<Node>, Id<Node>>, Link> existingLinks = new HashMap<>();
		for(Link l : network.getLinks().values()) {
			existingLinks.put(new Tuple<>(l.getFromNode().getId(), l.getToNode().getId()), l);
		}

		for(ArtificialLink a : artificialLinks) {
			Tuple<Id<Node>, Id<Node>> key = new Tuple<>(a.getToNodeId(), a.getFromNodeId());

			Link existingLink = existingLinks.get(key);

			if(existingLink == null || a.getFreespeed() > existingLink.getFreespeed() || a.getLength() > existingLink.getLength()) {
				String newLinkIdStr = config.getPrefixArtificial() + artificialId++;
				Id<Node> fromNodeId = a.getToNodeId();
				Node fromNode;
				Id<Node> toNodeId = a.getFromNodeId();
				Node toNode;

				if(!network.getNodes().containsKey(fromNodeId)) {
					fromNode = network.getFactory().createNode(fromNodeId, a.getFromNodeCoord());
					network.addNode(fromNode);
				} else {
					fromNode = network.getNodes().get(fromNodeId);
				}
				if(!network.getNodes().containsKey(toNodeId)) {
					toNode = network.getFactory().createNode(toNodeId, a.getToNodeCoord());
					network.addNode(toNode);
				} else {
					toNode = network.getNodes().get(toNodeId);
				}

				Link newLink = network.getFactory().createLink(Id.createLinkId(newLinkIdStr), fromNode, toNode);
				newLink.setAllowedModes(a.getAllowedModes());
				newLink.setLength(a.getLength());
				newLink.setFreespeed(a.getFreespeed());
				newLink.setCapacity(a.getCapacity());

				network.addLink(newLink);

//				if(existingLink != null && existingLink.getAllowedModes().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE))
//					network.removeLink(existingLink.getId());
			}
		}
	}

	private synchronized void increaseCounter() {
		counterPseudoRouting.incCounter();
	}

}
