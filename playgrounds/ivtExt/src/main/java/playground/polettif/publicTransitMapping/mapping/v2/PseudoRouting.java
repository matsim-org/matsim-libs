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

/**
 * Created by polettif on 17.06.2016.
 */

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoGraph;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoRouteStop;
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
public class PseudoRouting extends Thread {

	protected static Logger log = Logger.getLogger(PseudoRouting.class);

	private static Counter counterPseudoRouting = new Counter("route # ");
	private static int artificialId = 0;

	private final PublicTransitMappingConfigGroup config;

	private final Map<String, Network> modeSeparatedNetworks;
	private final Map<String, Router> modeSeparatedRouters;
	private final LinkCandidateCreator linkCandidates;

	private List<TransitLine> queue = new ArrayList<>();
	private Set<Tuple<LinkCandidate, LinkCandidate>> artificialLinksToBeCreated = new HashSet<>();
	private PseudoSchedule threadPseudoSchedule = new PseudoScheduleImpl();

	private Map<String, LeastCostPathCalculator.Path> localStoredPaths = new HashMap<>();

	public PseudoRouting(PublicTransitMappingConfigGroup config, Map<String, Network> modeSeparatedNetworks, Map<String, Router> modeSeparatedRouters, LinkCandidateCreator linkCandidates) {
		this.config = config;
		this.modeSeparatedNetworks = modeSeparatedNetworks;
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
				Network modeNetwork = modeSeparatedNetworks.get(scheduleTransportMode);
				List<TransitRouteStop> routeStops = transitRoute.getStops();

				boolean mapScheduleModeArtificial = config.getModeRoutingAssignment().get(scheduleTransportMode).
						contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE);

				/** [4.1]
				 * Initiate pseudoGraph and Dijkstra algorithm for the current transitRoute.
				 *
				 * In the pseudoGraph, all link candidates are represented as nodes and the
				 * network paths between link candidates are reduced to a representation edge
				 * only storing the travel cost. With the pseudoGraph, the best linkCandidate
				 * sequence can be calculated (using Dijkstra). From this sequence, the actual
				 * path on the network can be routed later on.
				 */
				PseudoGraph pseudoGraph = new PseudoGraph(config);

				/** [4.2]
				 * Calculate the shortest paths between each pair of routeStops/ParentStopFacility
				 */
				for(int i = 0; i < routeStops.size() - 1; i++) {
					TransitStopFacility currentStopFacility = routeStops.get(i).getStopFacility();
					TransitStopFacility nextStopFacility = routeStops.get(i + 1).getStopFacility();
					Set<LinkCandidate> linkCandidatesCurrent = linkCandidates.getLinkCandidates(scheduleTransportMode, routeStops.get(i).getStopFacility());
					Set<LinkCandidate> linkCandidatesNext = linkCandidates.getLinkCandidates(scheduleTransportMode, routeStops.get(i + 1).getStopFacility());

					final double beelineDistance = CoordUtils.calcEuclideanDistance(currentStopFacility.getCoord(), nextStopFacility.getCoord());

					double maxAllowedPathCost;
					if(config.getTravelCostType().equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime)) {
						maxAllowedPathCost = (routeStops.get(i + 1).getArrivalOffset() - routeStops.get(i).getDepartureOffset()) * config.getBeelineDistanceMaxFactor();
					} else {
						maxAllowedPathCost = beelineDistance * config.getBeelineDistanceMaxFactor();
					}

					//Check if one of the two stops is outside the network
					boolean bothStopsWithLoopLinks = (!linkCandidates.stopFacilityOnlyHasLoopLink(currentStopFacility, scheduleTransportMode) && !linkCandidates.stopFacilityOnlyHasLoopLink(currentStopFacility, scheduleTransportMode));
//					boolean bothStopsInsideArea = !(!stopIsInArea.get(currentStopFacility) || !stopIsInArea.get(nextStopFacility));

					if(!mapScheduleModeArtificial && bothStopsWithLoopLinks) {
						/**
						 * Calculate the shortest path between all link candidates.
						 */
						for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
							for(LinkCandidate linkCandidateNext : linkCandidatesNext) {

								double pathCost;
								LeastCostPathCalculator.Path leastCostPath = null;
								Node nodeA = modeNetwork.getNodes().get(Id.createNodeId(linkCandidateCurrent.getToNodeIdStr()));
								Node nodeB = modeNetwork.getNodes().get(Id.createNodeId(linkCandidateNext.getFromNodeIdStr()));

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
									if(linkCandidateCurrent.getLinkIdStr().equals(linkCandidateNext.getLinkIdStr())) {
										pathCost *= 4;
									}
								} else {
									pathCost = maxAllowedPathCost * 2;
								}

								/**
								 * if the path between two link candidates is viable, add it to the pseudoGraph
								 */
								if(pathCost < maxAllowedPathCost) {
									PseudoRouteStop pseudoRouteStopCurrent = PseudoGraph.createPseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
									PseudoRouteStop pseudoRouteStopNext = PseudoGraph.createPseudoRouteStop(i + 1, routeStops.get(i + 1), linkCandidateNext);
									pseudoGraph.addPath(pseudoRouteStopCurrent, pseudoRouteStopNext, pathCost);
								}
								/**
								 * Use artificial path between two linkCandidates
								 *
								 * Note: the actual artificial link is not considered
								 * during subsequent shortest path searches since this would
								 * require reinitializing the router.
								 */
								else {
									artificialLinksToBeCreated.add(new Tuple<>(linkCandidateCurrent, linkCandidateNext));

									double length = CoordUtils.calcEuclideanDistance(linkCandidateCurrent.getToNodeCoord(), linkCandidateNext.getFromNodeCoord()) * config.getBeelineDistanceMaxFactor();
									double artificialPathCost = (config.getTravelCostType().equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime) ? length / 0.5 : length);

									PseudoRouteStop pseudoRouteStopCurrent = PseudoGraph.createPseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
									PseudoRouteStop pseudoRouteStopNext = PseudoGraph.createPseudoRouteStop(i + 1, routeStops.get(i + 1), linkCandidateNext);
									pseudoGraph.addPath(pseudoRouteStopCurrent, pseudoRouteStopNext, artificialPathCost);
								}
							}
						}
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
						for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
							for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
								artificialLinksToBeCreated.add(new Tuple<>(linkCandidateCurrent, linkCandidateNext));

								double length = CoordUtils.calcEuclideanDistance(linkCandidateCurrent.getToNodeCoord(), linkCandidateNext.getFromNodeCoord());
								double newPathWeight = (config.getTravelCostType().equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime) ? length / 0.5 : length);

								PseudoRouteStop pseudoRouteStopCurrent = PseudoGraph.createPseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
								PseudoRouteStop pseudoRouteStopNext = PseudoGraph.createPseudoRouteStop(i + 1, routeStops.get(i + 1), linkCandidateNext);
								pseudoGraph.addPath(pseudoRouteStopCurrent, pseudoRouteStopNext, newPathWeight);
							}
						}
					}
				} // - routeStop loop

				/** [4.3]
				 * Build pseudo network and find shortest path using dijkstra
				 */
				// add dummy path to pseudoGraph
				pseudoGraph.addSourceDummyPaths(0, routeStops.get(0), linkCandidates.getLinkCandidates(scheduleTransportMode, routeStops.get(0).getStopFacility()));
				int e = routeStops.size() - 1;
				pseudoGraph.addDestinationDummyPaths(e, routeStops.get(e), linkCandidates.getLinkCandidates(scheduleTransportMode, routeStops.get(e).getStopFacility()));

				// run Dijkstra
				pseudoGraph.runDijkstra();
				LinkedList<PseudoRouteStop> pseudoPath = pseudoGraph.getShortestPseudoPath();

				if(pseudoPath == null) {
					log.warn("PseudoGraph has no path from SOURCE to DESTIONATION for transit route " + transitRoute.getId() + " from \"" + routeStops.get(0).getStopFacility().getName() + "\" to \"" + routeStops.get(routeStops.size() - 1).getStopFacility().getName() + "\"");
				} else {
					threadPseudoSchedule.addPseudoRoute(transitLine, transitRoute, pseudoPath);
				}

				increaseCounter();
			}
		}
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
		Set<Tuple<String, String>> connections = new HashSet<>();
		for(Link l : network.getLinks().values()) {
			connections.add(new Tuple<>(l.getFromNode().getId().toString(), l.getToNode().getId().toString()));
		}

		for(Tuple<LinkCandidate, LinkCandidate> c : artificialLinksToBeCreated) {
			LinkCandidate fromLinkCandidate = c.getFirst();
			LinkCandidate toLinkCandidate = c.getSecond();

			Tuple<String, String> key = new Tuple<>(fromLinkCandidate.getToNodeIdStr(), toLinkCandidate.getFromNodeIdStr());

			if(!connections.contains(key)) {
				connections.add(key);
				String newLinkIdStr = config.getPrefixArtificial() + artificialId++;
				Id<Node> fromNodeId = Id.create(fromLinkCandidate.getFromNodeIdStr(), Node.class);
				Node fromNode;
				Id<Node> toNodeId = Id.create(toLinkCandidate.getToNodeIdStr(), Node.class);
				Node toNode;

				if(!network.getNodes().containsKey(fromNodeId)) {
					fromNode = network.getFactory().createNode(fromNodeId, fromLinkCandidate.getFromNodeCoord());
					network.addNode(fromNode);
				} else {
					fromNode = network.getNodes().get(fromNodeId);
				}
				if(!network.getNodes().containsKey(toNodeId)) {
					toNode = network.getFactory().createNode(toNodeId, toLinkCandidate.getToNodeCoord());
					network.addNode(toNode);
				} else {
					toNode = network.getNodes().get(toNodeId);
				}

				Link newLink = network.getFactory().createLink(Id.createLinkId(newLinkIdStr), fromNode, toNode);

				newLink.setAllowedModes(Collections.singleton(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE));
				double l = CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()) * config.getBeelineDistanceMaxFactor();
				newLink.setLength(l);
				newLink.setCapacity(9999);
				// needs to be set low so busses don't use those links during modeRouting.
				newLink.setFreespeed(0.5);
				network.addLink(newLink);
			}
		}
	}

	private synchronized void increaseCounter() {
		counterPseudoRouting.incCounter();
	}

}
