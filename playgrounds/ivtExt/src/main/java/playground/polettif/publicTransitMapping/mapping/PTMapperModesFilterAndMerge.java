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

package playground.polettif.publicTransitMapping.mapping;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.pt.utils.TransitScheduleValidator;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.*;
import playground.polettif.publicTransitMapping.mapping.router.FastAStarRouter;
import playground.polettif.publicTransitMapping.mapping.router.LinkFilterMode;
import playground.polettif.publicTransitMapping.mapping.router.ModeDependentRouter;
import playground.polettif.publicTransitMapping.mapping.router.Router;
import playground.polettif.publicTransitMapping.tools.CoordTools;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.*;

/**
 * References an unmapped transit schedule to a network. Combines routing of transit routes
 * and referencing stopFacilities. Creates additional stop facilities if a stopFacility has
 * more than one plausible link. Artificial links are added to the network if no route can
 * be found.
 * <p/>
 * Creates pseudo paths via all link candidates, chooses the path with the lowest travel time.
 * <p/>
 *
 * @author polettif
 */
public class PTMapperModesFilterAndMerge extends PTMapper {

	private static final int SAME_LINK_PUNISHMENT = 5;

	int artificialId = 0;
	private Map<Tuple<LinkCandidate, LinkCandidate>, Link> artificialLinks = new HashMap<>();
	private Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoTransitRoutes = new HashMap<>();
	private Map<String, Double> alreadyCalculatedWeight = new HashMap<>();


	/**
	 * Constructor.
	 * The given schedule is modified via {@link #mapScheduleToNetwork(Network)}
	 */
	public PTMapperModesFilterAndMerge(TransitSchedule schedule, PublicTransitMappingConfigGroup config) {
		super(schedule, config);
	}

	public PTMapperModesFilterAndMerge(String configPath) {
		super(configPath);
	}

	/**
	 * Routes the unmapped MATSim Transit Schedule to the network using the file
	 * paths specified in the config. Writes the resulting schedule and network to xml files.<p/>
	 *
	 * @param args <br/>[0] PublicTransportMap config file<br/>
	 */
	public static void main(String[] args) {
		if(args.length == 1) {
			new PTMapperModesFilterAndMerge(args[0]).mapFilesFromConfig();
		} else {
			System.out.println("Incorrect number of arguments\n[0] config file");
		}
	}

	/**
	 * Routes the unmapped MATSim Transit Schedule to the network using the file
	 * paths specified in the config. Writes the resulting schedule and network to xml files.<p/>
	 * @param configFile the PublicTransitMapping config file
	 */
	public static void run(String configFile) {
		new PTMapperModesFilterAndMerge(configFile).mapFilesFromConfig();
	}

	@Override
	public void mapFilesFromConfig() {
		if(config.getScheduleFile() == null || config.getNetworkFile() == null) {
			log.error("Not all input files defined in config.");
		} else {
			log.info("Mapping files from config...");
			log.info("Reading schedule and network file...");
			Scenario mainScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			Network network = mainScenario.getNetwork();
			new TransitScheduleReader(mainScenario).readFile(config.getScheduleFile());
			new MatsimNetworkReader(network).readFile(config.getNetworkFile());
			TransitSchedule mainSchedule = mainScenario.getTransitSchedule();

			new PTMapperModesFilterAndMerge(mainSchedule, config).mapScheduleToNetwork(network);

			if(config.getOutputNetworkFile() != null && config.getOutputScheduleFile() != null) {
				log.info("Writing schedule and network to file...");
				new TransitScheduleWriter(mainSchedule).writeFile(config.getOutputScheduleFile());
				new NetworkWriter(network).write(config.getOutputNetworkFile());
				if(config.getOutputStreetNetworkFile() != null) {
					NetworkFilterManager filterManager = new NetworkFilterManager(network);
					filterManager.addLinkFilter(new LinkFilterMode(Collections.singleton(TransportMode.car)));
					NetworkTools.writeNetwork(filterManager.applyFilters(), config.getOutputStreetNetworkFile());
				}
			} else {
				log.info("");
				log.info("No output paths defined, schedule and network are not written to files.");
			}

		}
	}

	@Override
	public void mapScheduleToNetwork(Network network) {
		this.networkFactory = network.getFactory();

		log.info("Mapping transit schedule to network...");
		int nStopFacilities = schedule.getFacilities().size();
		Set<String> noRoutingWarning = new HashSet<>();

		/** [1]
		 * Create a separate network for all schedule modes and
		 * initiate routers.
		 */
		Map<String, Network> networks = new HashMap<>();
		Map<String, Router> routers = new HashMap<>();
		for(Map.Entry<String, Set<String>> modeAssignment : config.getModeRoutingAssignment().entrySet()) {
			if(!modeAssignment.getValue().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
				log.info("================================================");
				log.info("Creating network and router for " + modeAssignment.getValue());
				NetworkFilterManager filter = new NetworkFilterManager(network);
				filter.addLinkFilter(new LinkFilterMode(modeAssignment.getValue()));
				Network filteredNetwork = filter.applyFilters();

				networks.put(modeAssignment.getKey(), filter.applyFilters());
				routers.put(modeAssignment.getKey(), new FastAStarRouter(filteredNetwork, config.getPseudoRouteWeightType()));
			}
		}


		/** [2]
		 * Preload the closest links and create LinkCandidates. StopFacilities
		 * with no links within search radius are given a dummy loop link right
		 * on their coordinates.
		 */
		Map<String, Map<TransitStopFacility, Set<LinkCandidate>>> linkCandidates = PTMapperUtils.generateModeLinkCandidates(schedule, network, config);
		PTMapperUtils.setSuffixChildStopFacilities(config.getSuffixChildStopFacilities(), config.getSuffixChildStopFacilitiesRegex());

		/** [3]
		 * Get network extent to speed up routing outside of network area.
		 */
		Coord[] totalNetworkExtent = CoordTools.getExtent(network);
		Map<TransitStopFacility, Boolean> stopIsInArea = CoordTools.getStopsInAreaBool(schedule, totalNetworkExtent);
		double maxExtent = CoordUtils.calcEuclideanDistance(totalNetworkExtent[0], totalNetworkExtent[1]);
		final double initialMaxPathCost = (config.getPseudoRouteWeightType().equals(PublicTransitMappingConfigGroup.PseudoRouteWeightType.travelTime) ?
				maxExtent * config.getBeelineDistanceMaxFactor() / config.getBeelineFreespeed() :
				maxExtent * config.getBeelineDistanceMaxFactor());

		/** [4]
		 * Generating and calculating the pseudoTransitRoutes for all transitRoutes.
		 * If no route on the network can be found (or the scheduelTransportMode
		 * should not be mapped to the network), artificial links between link
		 * candidates are created.
		 */
		log.info("================================================");
		log.info("Calculating pseudoTransitRoutes...");
		Counter counterLine = new Counter("route # ");
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				String scheduleTransportMode = transitRoute.getTransportMode();

				if(!config.getModeRoutingAssignment().containsKey(scheduleTransportMode)) {
					if(noRoutingWarning.add(scheduleTransportMode)) {
						log.warn("No routing assignment found for schedule transport mode " + scheduleTransportMode +
								". Transit routes using this mode are removed from the schedule.");
					}
				} else {
					counterLine.incCounter();

					Router modeRouter = routers.get(scheduleTransportMode);
					Network modeNetwork = networks.get(scheduleTransportMode);
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
					DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(pseudoGraph);

					/** [4.2]
					 * Calculate the shortest paths between each pair of routeStops/ParentStopFacility
					 */
					for(int i = 0; i < routeStops.size() - 1; i++) {
						TransitStopFacility currentStopFacility = routeStops.get(i).getStopFacility();
						TransitStopFacility nextStopFacility = routeStops.get(i + 1).getStopFacility();
						Set<LinkCandidate> linkCandidatesCurrent = linkCandidates.get(scheduleTransportMode).get(routeStops.get(i).getStopFacility());
						Set<LinkCandidate> linkCandidatesNext = linkCandidates.get(scheduleTransportMode).get(routeStops.get(i + 1).getStopFacility());

						// This block would prevent that the same link is used for both stops,
						// the link is used for the nearer stop facility. Is not needed however and
						// performance decreases significantly if used
						/* using field: private Map<Tuple<TransitStopFacility, TransitStopFacility>, Tuple<Set<LinkCandidate>, Set<LinkCandidate>>> separatedLinkCandidates = new HashMap<>();
						if(!separatedLinkCandidates.containsKey(new Tuple<>(currentStopFacility, nextStopFacility))) {
							int currentBefore = linkCandidatesCurrent.size();
							int nextBefore = linkCandidatesNext.size();
							PTMapperUtils.separateLinkCandidates(linkCandidatesCurrent, linkCandidatesNext);
							separatedLinkCandidates.put(new Tuple<>(currentStopFacility, nextStopFacility), new Tuple<>(linkCandidatesCurrent, linkCandidatesNext));
						} else {
							Tuple<Set<LinkCandidate>, Set<LinkCandidate>> tmpTuple = separatedLinkCandidates.get(new Tuple<>(currentStopFacility, nextStopFacility));
							linkCandidatesCurrent = tmpTuple.getFirst();
							linkCandidatesNext = tmpTuple.getSecond();
						}

						if(linkCandidatesCurrent.size() == 0) {	log.error("no link candidates left!"); }
						if(linkCandidatesNext.size() == 0) { log.error("no link candidates left!"); }
						*/

						final double beelineDistance = CoordUtils.calcEuclideanDistance(currentStopFacility.getCoord(), nextStopFacility.getCoord());
						final double maxAllowedPathCost = beelineDistance * config.getBeelineDistanceMaxFactor();

						//Check if one of the two stops is outside the network
						boolean bothStopsInsideArea = !(!stopIsInArea.get(currentStopFacility) || !stopIsInArea.get(nextStopFacility));
						boolean pseudoPathFound = false;

						if(!mapScheduleModeArtificial && bothStopsInsideArea) {
							/**
							 * Calculate the shortest path between all link candidates.
 							 */
							for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
								for(LinkCandidate linkCandidateNext : linkCandidatesNext) {

									String key = scheduleTransportMode + "." + linkCandidateCurrent.getId() + "." + linkCandidateNext.getId();
									double pathCost = initialMaxPathCost;

									if(alreadyCalculatedWeight.containsKey(key)) {
										pathCost = alreadyCalculatedWeight.get(key);
									} else {
										Node nodeA = modeNetwork.getNodes().get(Id.createNodeId(linkCandidateCurrent.getToNodeIdStr()));
										Node nodeB = modeNetwork.getNodes().get(Id.createNodeId(linkCandidateNext.getFromNodeIdStr()));

										LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(nodeA, nodeB);

										// path is null if links are on separate networks
										if(leastCostPath != null) {
											pathCost = leastCostPath.travelCost;

											// if both links are the same, travel time should get higher
											if(linkCandidateCurrent.getLinkIdStr().equals(linkCandidateNext.getLinkIdStr())) {
												pathCost *= SAME_LINK_PUNISHMENT;
											}
										}
										alreadyCalculatedWeight.put(key, pathCost);
									}

									/**
									 * if the path between two link candidates is viable, add it to the pseudoGraph
									 */
									if(pathCost < maxAllowedPathCost) {
										pseudoPathFound = true;
										PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
										PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(i + 1, routeStops.get(i + 1), linkCandidateNext);
										pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, pathCost), (i == 0), (i == routeStops.size() - 2));
									}
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
						 * Note: the actual artificial link is not considered
						 * during pseudoTransitRoute creation since this would
						 * require reinitializing the router.
						 */
						if(mapScheduleModeArtificial ||
								!bothStopsInsideArea ||
								!pseudoPathFound) {
							for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
								for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
									Link newLink = createArtificialLink(network, linkCandidateCurrent, linkCandidateNext);
									double newPathWeight = (config.getPseudoRouteWeightType().equals(PublicTransitMappingConfigGroup.PseudoRouteWeightType.travelTime) ? newLink.getLength()/newLink.getFreespeed() : newLink.getLength());

									PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
									PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(i + 1, routeStops.get(i + 1), linkCandidateNext);
									pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, newPathWeight), (i == 0), (i == routeStops.size() - 2));
								}
							}
						}
					} // - routeStop loop

					/** [4.3]
					 * Build pseudo network and find shortest path using dijkstra
					 */
					dijkstra.run();
					List<PseudoRouteStop> pseudoStopSequence = MapUtils.getList(transitRoute, MapUtils.getMap(transitLine, pseudoTransitRoutes));
					LinkedList<PseudoRouteStop> pseudoPath = dijkstra.getShortesPseudoPath();
					if(pseudoPath == null) {
						log.warn("PseudoRouting could not find a shortest path for transit route " + transitRoute.getId() + " from \"" +routeStops.get(0).getStopFacility().getName()+ "\" to \""+routeStops.get(routeStops.size()-1).getStopFacility().getName()+"\"");
					} else {
						pseudoStopSequence.addAll(pseudoPath);
					}
				} // - if correct schedule mode
			} // - transitRoute loop
		} // - line loop

		/** [5]
		 * Merge all networks so artificial links appear on the base network.
		 */
		NetworkTools.mergeNetworks(network, networks.values());

		/** [6]
		 * Replace the parent stop facilities in the transitRoute routeProfiles
		 * with child StopFacilities. Add the new transitRoutes to the schedule.
		 */
		PTMapperUtils.createAndReplaceFacilities(schedule, pseudoTransitRoutes, config.getSuffixChildStopFacilities());

		/** [7]
		 * The final routing should be done on the merged network, so a mode dependent
		 * router for each schedule mode is initialized using the same merged network.
		 */
		Map<String, Router> finalRouters = new HashMap<>();    // key: ScheduleTransportMode
		for(Map.Entry<String, Set<String>> modeAssignment : config.getModeRoutingAssignment().entrySet()) {
			log.info("Creating Router for " + modeAssignment.getKey() + " ...");
			finalRouters.put(modeAssignment.getKey(), new ModeDependentRouter(network, modeAssignment.getValue()));
		}

		/** [8]
		 * Route all transitRoutes with the new referenced links. The shortest path
		 * between child stopFacilities is calculated and added to the schedule.
		 */
		ScheduleTools.routeSchedule(schedule, network, finalRouters);

		/** [9]
		 * Now that all lines have been routed, it is possible that a route passes
		 * a link closer to a stop facility than its referenced link.
		 */
		PTMapperUtils.concentrateStopFacilities(schedule, network);

		/** [10]
		 * After all lines created, clean the schedule and network. Removing
		 * not used transit links includes removing artificial links that
		 * needed to be added to the network for routing purposes.
		 *
		 */
		log.info("================================================");
		log.info("Clean schedule and network...");
		NetworkTools.setFreeSpeedOfLinks(network, PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE, config.getFreespeedArtificial());
		int routesRemoved = ScheduleCleaner.removeTransitRoutesWithoutLinkSequences(schedule);
		ScheduleCleaner.removeNotUsedTransitLinks(schedule, network, config.getModesToKeepOnCleanUp());
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleTools.assignScheduleModesToLinks(schedule, network);
		if(config.getCombinePtModes()) {
			ScheduleTools.replaceNonCarModesWithPT(network);
		} else {
			ScheduleTools.addPTModeToNetwork(schedule, network);
		}
		log.info("Clean schedule and network... done.");

		log.info("Validating schedule and network...");
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(schedule, network);
		if (validationResult.isValid()) { log.info("Schedule appears valid!");
		} else { log.info("Schedule is NOT valid!"); }
		if (validationResult.getErrors().size() > 0) { log.info("Validation errors:");
			for (String e : validationResult.getErrors()) { log.info(e); } }
		if (validationResult.getWarnings().size() > 0) { log.info("Validation warnings:");
			for (String w : validationResult.getWarnings()) { log.info(w); } }


		/**
		 * Statistics
		 */
		int artificialLinks = 0;
		for(Link l : network.getLinks().values()) {
			if(l.getAllowedModes().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
				artificialLinks++;
			}
		}

		log.info("================================================");
		log.info("=== Mapping transit schedule to network... done.");
		log.info("================================================");
		log.info("    Stop Facilities statistics:");
		log.info("       input    "+nStopFacilities);
		log.info("       output   "+schedule.getFacilities().size());
		log.info("       diff.    "+(schedule.getFacilities().size()-nStopFacilities));
		log.info("       factor   "+(schedule.getFacilities().size()/((double) nStopFacilities)));
		log.info("    Transit Route statistics:");
		log.info("       removed  " + routesRemoved);
		log.info("    Artificial Links:");
		log.info("       created  " + artificialLinks);
	}

	/**
	 * Creates a new link between two linkCandidates (or
	 * more precisely a link connecting the to- and fromNode
	 * of both linkCandidates). The freespeed of the link is set
	 * via config.
	 * @return the new link
	 */
	private Link createArtificialLink(Network network, final LinkCandidate fromLinkCandidate, final LinkCandidate toLinkCandidate) {
		Tuple<LinkCandidate, LinkCandidate> key = new Tuple<>(fromLinkCandidate, toLinkCandidate);

		Link newLink;

		if(artificialLinks.containsKey(key)) {
			newLink = artificialLinks.get(key);
		} else {
			String newLinkIdStr = config.getPrefixArtificial() + artificialId++;
			Id<Node> fromNodeId = Id.create(fromLinkCandidate.getFromNodeIdStr(), Node.class);
			Id<Node> toNodeId = Id.create(toLinkCandidate.getToNodeIdStr(), Node.class);

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

			newLink = network.getFactory().createLink(Id.createLinkId(newLinkIdStr), fromNode,	toNode);

			newLink.setAllowedModes(Collections.singleton(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE));
			newLink.setLength(CoordUtils.calcEuclideanDistance(fromLinkCandidate.getToNodeCoord(), toLinkCandidate.getFromNodeCoord()));
			newLink.setFreespeed(0.5); // needs to be set low so busses don't use those links during modeRouting
			network.addLink(newLink);

			artificialLinks.put(key, newLink);
		}
		return newLink;
	}
}
