/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.multiModalMap.mapping;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.*;
import playground.polettif.multiModalMap.mapping.router.FastAStarRouter;
import playground.polettif.multiModalMap.mapping.router.ModeDependentRouter;
import playground.polettif.multiModalMap.mapping.router.LinkFilterMode;
import playground.polettif.multiModalMap.mapping.router.Router;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.*;

/**
 * References an unmapped transit schedule to a  b network. Combines routing and referencing of stopFacilities. Creates additional
 * stop facilities if a stopFacility has more than one plausible link. @see main()
 * <p/>
 * Creates pseudo paths via all link candidates, chooses the path with the lowest travel time.
 * <p/>
 *
 * @author polettif
 */
public class PTMapperModesFilterAndMerge extends PTMapper {

	int artificialId = 0;
	private Map<Tuple<LinkCandidate, LinkCandidate>, Link> artificialLinks = new HashMap<>();
	private Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoTransitRoutes = new HashMap<>();
	private Map<String, Double> alreadyCalculated = new HashMap<>();


	/**
	 * Constructor.
	 * The given schedule is modified via {@link #mapScheduleToNetwork(Network)}
	 */
	public PTMapperModesFilterAndMerge(TransitSchedule schedule, PublicTransportMapConfigGroup config) {
		super(schedule, config);
	}

	public PTMapperModesFilterAndMerge(String configPath) {
		super(configPath);
	}

	/**
	 * Routes the unmapped MATSim Transit Schedule file to the network given by file. Writes the resulting
	 * schedule and network to xml files.<p/>
	 * <p/>
	 *
	 * @param args <br/>[0] unmapped MATSim Transit Schedule file<br/>
	 *             [1] MATSim network file<br/>
	 *             [2] output schedule file path<br/>
	 *             [3] output network file path
	 */
	public static void main(String[] args) {
		if(args.length == 1) {
			log.info("Mapping files from config...");
			new PTMapperModesFilterAndMerge(args[0]).mapFilesFromConfig();
		} else if(args.length != 4) {
			System.out.println("Incorrect number of arguments\n[0] unmapped schedule file\n[1] network file\n[2] output schedule path\n[3]output network path");
		} else {
			mapFromFiles(args[0], args[1], args[2], args[3]);
		}
	}

	@Override
	public void mapFilesFromConfig() {
		mapFromFiles(config.getScheduleFile(), config.getNetworkFile(), config.getOutputScheduleFile(), config.getOutputNetworkFile());
	}

	/**
	 * Routes the unmapped MATSim Transit Schedule file to the network given by file. Writes the resulting
	 * schedule and network to xml files.<p/>
	 *
	 * @param matsimTransitScheduleFile unmapped MATSim Transit Schedule (unmapped: stopFacilities are not referenced to links and routes do not have a network route (linkSequence) yet.
	 * @param networkFile               MATSim network file
	 * @param outputScheduleFile        the resulting MATSim Transit Schedule
	 * @param outputNetworkFile         the resulting MATSim network (might have some additional links added)
	 */
	public static void mapFromFiles(String matsimTransitScheduleFile, String networkFile, String outputScheduleFile, String outputNetworkFile) {
		log.info("Reading schedule and network file...");
		Scenario mainScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network mainNetwork = mainScenario.getNetwork();
		new TransitScheduleReader(mainScenario).readFile(matsimTransitScheduleFile);
		new MatsimNetworkReader(mainNetwork).readFile(networkFile);
		TransitSchedule mainSchedule = mainScenario.getTransitSchedule();

		log.info("Mapping transit schedule to network...");
		new PTMapperModesFilterAndMerge(mainSchedule, new PublicTransportMapConfigGroup()).mapScheduleToNetwork(mainNetwork);

		log.info("Writing schedule and network to file...");
		new TransitScheduleWriter(mainSchedule).writeFile(outputScheduleFile);
		new NetworkWriter(mainNetwork).write(outputNetworkFile);

		log.info("Mapping public transit to network successful!");
	}

	@Override
	public void mapScheduleToNetwork(Network network) {
		this.networkFactory = network.getFactory();

		/**
		 * Store some statistics
		 */
		int nStopFacilities = schedule.getFacilities().size();
		int nTransitRoutes = 0;
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			nTransitRoutes += transitLine.getRoutes().size();
		}

		Set<String> noRoutingWarning = new HashSet<>();

		log.info("Mapping transit schedule to network...");

		/** [1]
		 * Create separate network for all schedule modes and
		 * initiate routers.
		 */
		Map<String, Network> networks = new HashMap<>();
		Map<String, Router> routers = new HashMap<>();

		for(Map.Entry<String, Set<String>> modeAssignment : config.getModeRoutingAssignment().entrySet()) {
			log.info("Creating network for " + modeAssignment.getValue());
			NetworkFilterManager filter = new NetworkFilterManager(network);
			filter.addLinkFilter(new LinkFilterMode(modeAssignment.getValue()));
			Network filteredNetwork = filter.applyFilters();

			networks.put(modeAssignment.getKey(), filter.applyFilters());
			routers.put(modeAssignment.getKey(), new FastAStarRouter(filteredNetwork));
		}


		/** [1]
		 * Preload the closest links, create LinkCandidates and child
		 * StopFacilities. If a stop facility is already referenced
		 * (manually beforehand for example) no child facilities are
		 * created. StopFacilities with no links within search radius
		 * are given a dummy loop link right on their coordinates.
		 */
		Map<String, Map<TransitStopFacility, Set<LinkCandidate>>> linkCandidates = PTMapperUtils.generateModeLinkCandidates(schedule, network, config);


		/**
		 * Get network extent to speed up routing outside of network area.
		 */
		Coord[] totalNetworkExtent = NetworkTools.getExtent(network);
		Map<TransitStopFacility, Boolean> stopIsInArea = NetworkTools.getStopsInAreaBool(schedule, totalNetworkExtent);
		final double initialMaxTravelCostValue = CoordUtils.calcEuclideanDistance(totalNetworkExtent[0], totalNetworkExtent[1]);

		/** [3]
		 * Generating and calculating the pseudoTransitRoutes for all transitRoutes.
		 * If no route on the network can be found (or the scheduelTransportMode
		 * should not be mapped to the network), artificial links between link
		 * candidates are created.
		 */
		log.info("Calculating pseudoTransitRoutes...");
		Counter counterLine = new Counter("route # ");
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				String scheduleTransportMode = transitRoute.getTransportMode().toUpperCase();

				if(!config.getModeRoutingAssignment().containsKey(scheduleTransportMode)) {
					if(noRoutingWarning.add(scheduleTransportMode)) {
						log.warn("No routing assignment found for schedule transport mode " + scheduleTransportMode +
								". Transit routes using this mode are removed from the schedule.");
					}
				} else {

					boolean mapScheduleModeArtificial = config.getModeRoutingAssignment().get(scheduleTransportMode).
							contains(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE);

					Router modeRouter = routers.get(scheduleTransportMode);
					Network modeNetwork = networks.get(scheduleTransportMode);
					List<TransitRouteStop> routeStops = transitRoute.getStops();

					counterLine.incCounter();
					/*
					log.debug("transitRoute " + transitRoute.getId() + " " +
							"from \""+ routeStops.get(0).getStopFacility().getName() +
							"\" to \"" + routeStops.get(routeStops.size()-1).getStopFacility().getName() +
							"\" [" + routeStops.size() + " stops]");
					*/

					/**
					 * Initiate pseudoGraph and Dijkstra algorithm for the current transitRoute.
					 */
					PseudoGraph pseudoGraph = new PseudoGraph();
					DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(pseudoGraph);

					/** [3.1]
					 * Calculate the shortest paths between each pair of routeStops/ParentStopFacility
					 */
					for(int i = 0; i < routeStops.size() - 1; i++) {
						TransitStopFacility currentStopFacility = routeStops.get(i).getStopFacility();
						TransitStopFacility nextStopFacility = routeStops.get(i + 1).getStopFacility();

						double beelineDistance = CoordUtils.calcEuclideanDistance(currentStopFacility.getCoord(), nextStopFacility.getCoord());
						double maxAllowedTravelCost = beelineDistance * config.getBeelineDistanceMaxFactor();
						double minTravelCost = Double.MAX_VALUE;

						Set<LinkCandidate> linkCandidatesCurrent = linkCandidates.get(scheduleTransportMode).get(routeStops.get(i).getStopFacility());
						Set<LinkCandidate> linkCandidatesNext = linkCandidates.get(scheduleTransportMode).get(routeStops.get(i + 1).getStopFacility());

						//Check if one of the two stops is outside the network extend.
						boolean bothStopsInsideArea = true;
						if(!stopIsInArea.get(currentStopFacility) || !stopIsInArea.get(nextStopFacility)) {
							bothStopsInsideArea = false;
						}

						if(!mapScheduleModeArtificial && bothStopsInsideArea) {
							// Calculate the shortes path between all link candidates.
							for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
								for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
									String key = scheduleTransportMode + "." + linkCandidateCurrent.getId() + "." + linkCandidateNext.getId();
									double travelCost = initialMaxTravelCostValue;
									if(alreadyCalculated.containsKey(key)) {
										travelCost = alreadyCalculated.get(key);
									} else {
										Node nodeA = modeNetwork.getNodes().get(linkCandidateCurrent.getToNodeId());
										Node nodeB = modeNetwork.getNodes().get(linkCandidateNext.getFromNodeId());

										LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(nodeA, nodeB);

										// path is null if links are on separate networks
										if(leastCostPath != null) {
											travelCost = leastCostPath.travelCost;

											// if both links are the same, travel time should get higher since
											if(linkCandidateCurrent.getLinkId().equals(linkCandidateNext.getLinkId())) {
												travelCost *= 10;
											}

											if(travelCost < minTravelCost) { minTravelCost = travelCost; }
										}
										alreadyCalculated.put(key, travelCost);
									}
									PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
									PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(i + 1, routeStops.get(i + 1), linkCandidateNext);
									pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, travelCost), (i == 0), (i == routeStops.size() - 2));
								}
							}
						}

						/** [.]
						 * Create artificial links between two routeStops if:
						 * 	 - scheduleMode should use artificial links
						 *   - one of the two stops is outside the network area
						 *   - the distance of the route found between the two
						 *     stops exceeds the maximum allowed travel costs (this
						 *     applies also if no route could be found).
						 *
						 * Artificial links are created between all LinkCandidates
						 * (usually this means between one dummy link for the stop
						 * facility and the other linkCandidates).
						 *
						 * Note: the artificial link is not considered in further
						 * during pseudoTransitRoute creation since this would
						 * require reinitializing router.
						 */
						if(mapScheduleModeArtificial ||
								!bothStopsInsideArea ||
								minTravelCost > maxAllowedTravelCost) {
							for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
								for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
									createArtificialLink(network, linkCandidateCurrent, linkCandidateNext, config.getBeelineDistanceMaxFactor());

									PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
									PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(i + 1, routeStops.get(i + 1), linkCandidateNext);

									pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, maxAllowedTravelCost), (i == 0), (i == routeStops.size() - 2));
								}
							}
						}
					} // - routeStop loop

					/** [3.2]
					 * build pseudo network and find shortest path => List<LinkCandidate>
					 */
					dijkstra.run();
					List<PseudoRouteStop> pseudoStopSequence = MapUtils.getList(transitRoute, MapUtils.getMap(transitLine, pseudoTransitRoutes));
					pseudoStopSequence.addAll(dijkstra.getShortesPseudoPath());
				}
			} // - transitRoute loop
		} // - line loop

		/** [4]
		 * Replace the parent stop facilities in the transitRoute routeProfiles
		 * with child StopFacilities. Add the new transitRoutes to the schedule.
		 */
		log.info("Replacing parent StopFacilities with child StopFacilities...");
		PTMapperUtils.replaceFacilities(schedule, pseudoTransitRoutes);

		/**
		 * Merge all networks
		 */
		NetworkTools.mergeNetworks(network, networks.values());

		/** [5]
		 * route all routes with the new referenced links. Routers have to be initialized again
		 * since the network has changed (artificial links).
		 */
		Map<String, Router> finalRouters = getModeRouters(network, config.getModeRoutingAssignment());
		PTMapperUtils.routeSchedule(schedule, network, finalRouters);

		/** [6]
		 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
		 * and all nodes which are non-linked to any link after the above cleaning...
		 * Clean also the allowed modes for only the modes, no line-number any more...
		 */
		log.info("Clean Stations and Network...");
		PTMapperUtils.removeTransitRoutesWithoutLinkSequences(schedule);
		PTMapperUtils.removeNonUsedStopFacilities(schedule);
		PTMapperUtils.removeNotUsedTransitLinks(schedule, network, config.getModesToKeepOnCleanUp());

		PTMapperUtils.assignScheduleModesToLinks(schedule, network);
//		PTMapperUtils.replaceNonCarModesWithPT(schedule, network);
//		PTMapperUtils.addPTModeToNetwork(schedule, network);
		log.info("Clean Stations and Network... done.");

		log.info("=== Mapping transit schedule to network... done.");

		log.info("    Mapping statistics:\tinput\toutput\tdifference\tfactor");
		log.info("    Stop Facilities\t\t"+nStopFacilities+"\t\t\t"+schedule.getFacilities().size()+"\t"+(schedule.getFacilities().size()-nStopFacilities)+"\t\t"+(schedule.getFacilities().size()/((double) nStopFacilities)));
	}


	/**
	 * Create ModeDependentRouter for every scheduleTransportMode defined in the config.
	 * Networks are filtered.
	 *
	 * @return A map with the routers (key: scheduleTransportMode).
	 */
	private Map<String, Router> getModeRouters(Network network, Map<String, Set<String>> modesAssignment) {
		Map<String, Router> routers = new HashMap<>();    // key: ScheduleTransportMode
		for(Map.Entry<String, Set<String>> modeAssignment : modesAssignment.entrySet()) {
			log.info("Creating Router for " + modeAssignment.getKey() + " ...");
			routers.put(modeAssignment.getKey(), new ModeDependentRouter(network, modeAssignment.getValue()));
		}
		routers.put(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE, new ModeDependentRouter(network, Collections.singleton(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE)));
		return routers;
	}

	/**
	 * Creates a new link between two linkCandidates (or
	 * more precisely a link connecting the to- and fromNode
	 * of both linkCandidates).
	 *
	 * @param lengthFactor the euclidian distance
	 *                     between the two nodes is multiplied by this factor
	 * @return the new link
	 */
	private Link createArtificialLink(Network network, final LinkCandidate fromLinkCandidate, final LinkCandidate toLinkCandidate, double lengthFactor) {
		Tuple<LinkCandidate, LinkCandidate> key = new Tuple<>(fromLinkCandidate, toLinkCandidate);

		Link newLink;

		if(artificialLinks.containsKey(key)) {
			newLink = artificialLinks.get(key);
		} else {
			String newLinkIdStr = config.getPrefixArtificial() + artificialId++;
			Id<Node> fromNodeId = Id.create(fromLinkCandidate.getFromNodeId().toString(), Node.class);
			Id<Node> toNodeId = Id.create(toLinkCandidate.getToNodeId().toString(), Node.class);

			Node fromNode;
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

			newLink = network.getFactory().createLink(Id.createLinkId(newLinkIdStr),
					fromNode,
					toNode);

			newLink.setAllowedModes(Collections.singleton(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE));
			newLink.setLength(lengthFactor * CoordUtils.calcEuclideanDistance(fromLinkCandidate.getToNodeCoord(), toLinkCandidate.getFromNodeCoord()));
			network.addLink(newLink);

			artificialLinks.put(key, newLink);
		}
		return newLink;
	}
}
