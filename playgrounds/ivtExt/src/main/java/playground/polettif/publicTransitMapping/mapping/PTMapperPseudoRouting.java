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

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoGraph;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.PseudoRouteStop;
import playground.polettif.publicTransitMapping.mapping.router.FastAStarRouter;
import playground.polettif.publicTransitMapping.mapping.router.Router;
import playground.polettif.publicTransitMapping.plausibility.StopFacilityHistogram;
import playground.polettif.publicTransitMapping.tools.CoordTools;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

/**
 * References an unmapped transit schedule to a network. Combines
 * routing of transit routes and referencing stopFacilities. Additional
 * stop facilities are created if a stopFacility has more than one
 * plausible link. Artificial links are added to the network if no
 * route can be found.
 *
 * @author polettif
 */
public class PTMapperPseudoRouting extends PTMapper {

	// pseudoRouting
	private static Counter counterPseudoRouting = new Counter("route # ");
	private Map<String, Network> modeSeparatedNetworks = new HashMap<>();
	private Map<String, Router> modeSeparatedRouters = new HashMap<>();
	private Map<String, Map<TransitStopFacility, Set<LinkCandidate>>> linkCandidates;

	// helper variables
	private int artificialId = 0;
	private Map<Tuple<LinkCandidate, LinkCandidate>, Link> artificialLinks = new HashMap<>();
	private Map<TransitStopFacility, Boolean> stopIsInArea;

	protected PTMapperPseudoRouting(PublicTransitMappingConfigGroup config, TransitSchedule schedule, Network network) {
		super(config, schedule, network);
	}

	public PTMapperPseudoRouting(String configPath) {
		super(configPath);
	}

	@Override
	public void run() {
		if(schedule == null) {
			throw new RuntimeException("No schedule defined!");
		}
		else if(network == null) {
			throw new RuntimeException("No network defined!");
		}

		setLogLevels();
		try {
			if(config.getOutputScheduleFile() != null) log.addAppender(new FileAppender(new SimpleLayout(), config.getOutputScheduleFile()+".log"));
		} catch (IOException e) { e.printStackTrace(); }

		log.info("Mapping transit schedule to network...");
		int nStopFacilities = schedule.getFacilities().size();

		/** [1]
		 * Create a separate network for all schedule modes and
		 * initiate routers.
		 */
		log.info("===========================================");
		log.info("Creating mode separated network and routers");
		int targetNumThreads = config.getThreads();
		Map<String, Integer> scheduleModeToThreadAssignment = new HashMap<>();
		int threadCount=1;
		FastAStarRouter.setPseudoRouteWeightType(config.getPseudoRouteWeightType());
		for(Map.Entry<String, Set<String>> modeAssignment : config.getModeRoutingAssignment().entrySet()) {
			if(!modeAssignment.getValue().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
				log.info("Initiating network and router for " + modeAssignment.getValue());
				Network filteredNetwork = NetworkTools.filterNetworkByLinkMode(network, modeAssignment.getValue());

				modeSeparatedNetworks.put(modeAssignment.getKey(), filteredNetwork);
				modeSeparatedRouters.put(modeAssignment.getKey(), new FastAStarRouter(filteredNetwork));

				scheduleModeToThreadAssignment.put(modeAssignment.getKey(), threadCount);
				threadCount = threadCount + targetNumThreads;
			} else {
				scheduleModeToThreadAssignment.put(modeAssignment.getKey(), 0);
			}
		}

		/** [2]
		 * Preload the closest links and create LinkCandidates. StopFacilities
		 * with no links within search radius are given a dummy loop link right
		 * on their coordinates. Each Link Candidate is a possible new stop facility
		 * after PseudoRouting.
		 */
		PTMapperUtils.setSuffixChildStopFacilities(config.getSuffixChildStopFacilities(), config.getSuffixRegexEscaped());
		linkCandidates = PTMapperUtils.generateModeLinkCandidates(schedule, network, config);

		/** [3]
		 * Get network extent to speed up routing outside of network area.
		 */
		Coord[] totalNetworkExtent = CoordTools.getExtent(network);
		stopIsInArea = CoordTools.getStopsInAreaBool(schedule, totalNetworkExtent);

		/** [4]
		 * PseudoRouting
		 * Initiate and start threads, calculate PseudoTransitRoutes
		 * for all transit routes.
		 */
		log.info("==================================");
		log.info("Calculating pseudoTransitRoutes...");

		// initiate router
		Map<Integer, Integer> thr = new HashMap<>();
		PseudoRouting[] pseudoRoutingThreads = new PseudoRouting[threadCount];

		for(int i = 0; i < threadCount; i++) {
			pseudoRoutingThreads[i] = new PseudoRouting();
		}

		// spread transit routes on threads
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(!config.getModeRoutingAssignment().containsKey(transitRoute.getTransportMode())) {
					throw new RuntimeException("No routing assignment defined for schedule transport mode " + transitRoute.getTransportMode());
				}

				if(targetNumThreads == 0) {
					pseudoRoutingThreads[0].add(transitLine, transitRoute);
				} else {
					int t = MapUtils.getInteger(scheduleModeToThreadAssignment.get(transitRoute.getTransportMode()), thr, 0);
					thr.put(scheduleModeToThreadAssignment.get(transitRoute.getTransportMode()), ++t);
					int index = scheduleModeToThreadAssignment.get(transitRoute.getTransportMode()) + (t % targetNumThreads);
					pseudoRoutingThreads[index].add(transitLine, transitRoute);
				}
			}
		}

		// start pseudoRouting
		for(PseudoRouting thread : pseudoRoutingThreads) {
			thread.start();
		}
		for(PseudoRouting thread : pseudoRoutingThreads) {
			try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
		}


		/** [5]
		 * Add artificial links to network (have been defined during pseudoRouting)
		 */
		log.info("=====================================");
		log.info("Adding artificial links to network...");
		final Set<Tuple<LinkCandidate, LinkCandidate>> artificialLinksToBeCreated = new HashSet<>();
		for(PseudoRouting thread : pseudoRoutingThreads) {
			artificialLinksToBeCreated.addAll(thread.getArtificialLinksToBeCreated());
		}
		for(Tuple<LinkCandidate, LinkCandidate> t : artificialLinksToBeCreated) {
			createArtificialLink(t.getFirst(), t.getSecond());
		}

		/**
		 * Collect pseudoSchedules from threads
		 */
		final Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoSchedule = new HashMap<>();
		for(PseudoRouting thread : pseudoRoutingThreads) {
			Map<Tuple<TransitLine, TransitRoute>, List<PseudoRouteStop>> threadPseudoSchedule = thread.getPseudoSchedule();
			for(Map.Entry<Tuple<TransitLine, TransitRoute>, List<PseudoRouteStop>> e : threadPseudoSchedule.entrySet()) {
				if(MapUtils.getMap(e.getKey().getFirst(), pseudoSchedule).put(e.getKey().getSecond(), e.getValue()) != null) {
					throw new RuntimeException("Tried to assign a pseudoStopSequence twice to " + e.getKey());
				}
			}
		}

		/** [6]
		 * Replace the parent stop facilities in each transitRoute's routeProfile
		 * with child StopFacilities. Add the new transitRoutes to the schedule.
		 */
		log.info("========================================================================");
		log.info("Replacing parent StopFacilities in schedule with child StopFacilities...");
		PTMapperUtils.createAndReplaceFacilities(schedule, pseudoSchedule, config.getSuffixChildStopFacilities());


		/** [7]
		 * The final routing should be done on the merged network, so a mode dependent
		 * router for each schedule mode is initialized using the same merged network.
		 */
		log.info("=======================================================================================");
		log.info("Initiating final routers to map transit routes with referenced facilities to the network");
		Map<String, Router> finalRouters = new HashMap<>();    // key: ScheduleTransportMode

		// create router for artificial network
		FastAStarRouter artificialOnlyRouter = new FastAStarRouter(NetworkTools.filterNetworkByLinkMode(network, PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE_AS_SET));

		// create router for other mode networks
		for(Map.Entry<String, Set<String>> modeAssignment : config.getModeRoutingAssignment().entrySet()) {
			if(modeAssignment.getValue().size() == 1 && modeAssignment.getValue().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
				finalRouters.put(modeAssignment.getKey(), artificialOnlyRouter);
			} else {
				Set<String> routingTransportModes = new HashSet<>();
				routingTransportModes.add(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE);
				routingTransportModes.addAll(modeAssignment.getValue());
				log.info("Router for "+routingTransportModes);

				finalRouters.put(modeAssignment.getKey(), new FastAStarRouter(NetworkTools.filterNetworkByLinkMode(network, routingTransportModes)));
			}
		}

		/** [8]
		 * Route all transitRoutes with the new referenced links. The shortest path
		 * between child stopFacilities is calculated and added to the schedule.
		 */
		log.info("=============================================");
		log.info("Creating link sequences for transit routes...");
		ScheduleTools.routeSchedule(this.schedule, this.network, finalRouters);

		/** [9]
		 * Now that all lines have been routed, it is possible that a route passes
		 * a link closer to a stop facility than its referenced link.
		 */
		PTMapperUtils.pullChildStopFacilitiesTogether(this.schedule, this.network);

		/** [10]
		 * After all lines are created, clean the schedule and network. Removing
		 * not used transit links includes removing artificial links that
		 * needed to be added to the network for routing purposes.
		 */
		log.info("=============================");
		log.info("Clean schedule and network...");

		// changing the freespeed of the artificial links (value is used in simulations)
		NetworkTools.setFreeSpeedOfLinks(network, PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE, config.getFreespeedArtificial());
		NetworkTools.resetLinkLength(network, PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE);

		// Remove unnecessary parts of schedule
		int routesRemoved = config.getRemoveTransitRoutesWithoutLinkSequences() ? ScheduleCleaner.removeTransitRoutesWithoutLinkSequences(schedule) : 0;
		ScheduleCleaner.removeNotUsedTransitLinks(schedule, network, config.getModesToKeepOnCleanUp());
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);

		// change the network transport modes
		ScheduleTools.assignScheduleModesToLinks(schedule, network);
		if(config.getCombinePtModes()) {
			ScheduleTools.replaceNonCarModesWithPT(network);
		} else if (config.getAddPtMode()){
			ScheduleTools.addPTModeToNetwork(schedule, network);
		}

		/**
		 * Validate the schedule
		 */
		log.info("======================");
		log.info("Validating schedule...");
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(schedule, network);
		if(validationResult.isValid()) {
			log.info("Schedule appears valid!");
		} else {
			log.warn("Schedule is NOT valid!");
		}
		if(validationResult.getErrors().size() > 0) {
			log.info("Validation errors:");
			for(String e : validationResult.getErrors()) {
				log.info(e);
			}
		}
		if(validationResult.getWarnings().size() > 0) {
			log.info("Validation warnings:");
			for(String w : validationResult.getWarnings()) {
				log.info(w);
			}
		}

		/**
		 * Write output files if defined in config
		 */
		if(config.getOutputNetworkFile() != null && config.getOutputScheduleFile() != null) {
			log.info("Writing schedule and network to file...");
			try {
				ScheduleTools.writeTransitSchedule(schedule, config.getOutputScheduleFile());
				NetworkTools.writeNetwork(network, config.getOutputNetworkFile());
			} catch (Exception e) {
				log.error("Output directory not found! Trying to write schedule and network file in working directory");
				double t = System.nanoTime()/1000000;
				try {
					ScheduleTools.writeTransitSchedule(schedule, t+"schedule.xml.gz");
					NetworkTools.writeNetwork(network, t+"network.xml.gz");
				} catch (Exception e1) {
					throw new RuntimeException("Files could not be written in working directory");
				}
			}
			if(config.getOutputStreetNetworkFile() != null) {
				NetworkTools.writeNetwork(NetworkTools.filterNetworkByLinkMode(network, Collections.singleton(TransportMode.car)), config.getOutputStreetNetworkFile());
			}
		} else {
			log.info("");
			log.info("No output paths defined, schedule and network are not written to files.");
		}

		log.info("==================================================");
		log.info("= Mapping transit schedule to network completed! =");
		log.info("==================================================");

		/**
		 * Statistics
		 */
		int artificialLinks = 0;
		for(Link l : network.getLinks().values()) {
			if(l.getAllowedModes().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
				artificialLinks++;
			}
		}
		int withoutArtificialLinks = 0;
		int nRoutes = 0;
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				nRoutes++;

				boolean noArtificial = true;
				List<Id<Link>> linkIds = ScheduleTools.getLinkIds(transitRoute);
				for(Id<Link> linkId : linkIds) {
					if(!network.getLinks().get(linkId).getAllowedModes().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
						noArtificial = false;
					}
				}
				if(noArtificial) {
					withoutArtificialLinks++;
				}
			}
		}
		StopFacilityHistogram histogram = new StopFacilityHistogram(schedule);


		log.info("");
		log.info("    Artificial Links:");
		log.info("       created  " + artificialLinks);
		log.info("    Stop Facilities:");
		log.info("       total input   " + nStopFacilities);
		log.info("       total output  " + schedule.getFacilities().size());
		log.info("       diff.         " + (schedule.getFacilities().size() - nStopFacilities));
		log.info("    Child Stop Facilities:");
		log.info("       median nr created   " + String.format("%.0f", histogram.median()));
		log.info("       average nr created  " + String.format("%.2f", histogram.average()));
		log.info("       max nr created      " + String.format("%.0f", histogram.max()));
		log.info("    Transit Routes:");
		log.info("       removed from schedule            " + routesRemoved);
		log.info("       total routes in schedule         " + nRoutes);
		log.info("       routes without artificial links  " + withoutArtificialLinks);
		log.info("");
		log.info("    Run PlausibilityCheck for further analysis");
		log.info("");
		log.info("==================================================");

	}

	/**
	 * Creates a new link between two linkCandidates (or
	 * more precisely a link connecting the to- and fromNode
	 * of both linkCandidates).<p/>
	 *
	 * @return the new link
	 */
	private Link createArtificialLink(final LinkCandidate fromLinkCandidate, final LinkCandidate toLinkCandidate) {
		Tuple<LinkCandidate, LinkCandidate> key = new Tuple<>(fromLinkCandidate, toLinkCandidate);
		Link newLink;

		if(this.artificialLinks.containsKey(key)) {
			newLink = this.artificialLinks.get(key);
		} else {
			String newLinkIdStr = config.getPrefixArtificial() + artificialId++;
			Id<Node> fromNodeId = Id.create(fromLinkCandidate.getFromNodeIdStr(), Node.class);
			Node fromNode;
			Id<Node> toNodeId = Id.create(toLinkCandidate.getToNodeIdStr(), Node.class);
			Node toNode;

			if(!this.network.getNodes().containsKey(fromNodeId)) {
				fromNode = this.network.getFactory().createNode(fromNodeId, fromLinkCandidate.getFromNodeCoord());
				this.network.addNode(fromNode);
			} else {
				fromNode = this.network.getNodes().get(fromNodeId);
			}
			if(!this.network.getNodes().containsKey(toNodeId)) {
				toNode = this.network.getFactory().createNode(toNodeId, toLinkCandidate.getToNodeCoord());
				this.network.addNode(toNode);
			} else {
				toNode = this.network.getNodes().get(toNodeId);
			}

			newLink = this.network.getFactory().createLink(Id.createLinkId(newLinkIdStr), fromNode,	toNode);

			newLink.setAllowedModes(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE_AS_SET);
			double l = CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord())*config.getBeelineDistanceMaxFactor();
			newLink.setLength(l);
			// needs to be set low so busses don't use those links during modeRouting.
			newLink.setFreespeed(0.5);
			this.network.addLink(newLink);

			artificialLinks.put(key, newLink);
		}
		return newLink;
	}

	/**
	 * Generates and calculates the pseudoTransitRoutes for all the queued
	 * transit routes. If no route on the network can be found (or the
	 * scheduleTransportMode should not be mapped to the network), artificial
	 * links between link candidates are stored to be created later.
	 */
	public class PseudoRouting extends Thread {

		private Set<Tuple<TransitLine, TransitRoute>> queue = new HashSet<>();
		private Set<Tuple<LinkCandidate, LinkCandidate>> artificialLinksToBeCreated = new HashSet<>();
		private Map<Tuple<TransitLine, TransitRoute>, List<PseudoRouteStop>> threadPseudoSchedule = new HashMap<>();

		public void add(TransitLine transitLine, TransitRoute transitRoute) {
			queue.add(new Tuple<>(transitLine, transitRoute));
		}

		@Override
		public void run() {
			for(Tuple<TransitLine, TransitRoute> tuple : queue) {
				TransitRoute transitRoute = tuple.getSecond();

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
					Set<LinkCandidate> linkCandidatesCurrent = linkCandidates.get(scheduleTransportMode).get(routeStops.get(i).getStopFacility());
					Set<LinkCandidate> linkCandidatesNext = linkCandidates.get(scheduleTransportMode).get(routeStops.get(i + 1).getStopFacility());

					final double beelineDistance = CoordUtils.calcEuclideanDistance(currentStopFacility.getCoord(), nextStopFacility.getCoord());
					final double maxAllowedPathCost = beelineDistance * config.getBeelineDistanceMaxFactor();

					//Check if one of the two stops is outside the network
					boolean bothStopsInsideArea = !(!stopIsInArea.get(currentStopFacility) || !stopIsInArea.get(nextStopFacility));

					if(!mapScheduleModeArtificial && bothStopsInsideArea) {
						/**
						 * Calculate the shortest path between all link candidates.
						 */
						for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
							for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
								double pathCost;

								Node nodeA = modeNetwork.getNodes().get(Id.createNodeId(linkCandidateCurrent.getToNodeIdStr()));
								Node nodeB = modeNetwork.getNodes().get(Id.createNodeId(linkCandidateNext.getFromNodeIdStr()));

								LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(nodeA, nodeB);

								// path is null if links are on separate networks
								if(leastCostPath != null) {
									pathCost = leastCostPath.travelCost;

									// if both links are the same, cost should get higher
									if(linkCandidateCurrent.getLinkIdStr().equals(linkCandidateNext.getLinkIdStr())) {
										pathCost *= 4;
									}
								} else {
									pathCost = maxAllowedPathCost*2;
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

									double length = CoordUtils.calcEuclideanDistance(linkCandidateCurrent.getToNodeCoord(), linkCandidateNext.getFromNodeCoord())*config.getBeelineDistanceMaxFactor();
									double artificialPathCost = (config.getPseudoRouteWeightType().equals(PublicTransitMappingConfigGroup.PseudoRouteWeightType.travelTime) ? length / 0.5 : length);

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
								double newPathWeight = (config.getPseudoRouteWeightType().equals(PublicTransitMappingConfigGroup.PseudoRouteWeightType.travelTime) ? length / 0.5 : length);

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
				pseudoGraph.addSourceDummyPaths(0, routeStops.get(0), linkCandidates.get(scheduleTransportMode).get(routeStops.get(0).getStopFacility()));
				int e = routeStops.size()-1;
				pseudoGraph.addDestinationDummyPaths(e, routeStops.get(e), linkCandidates.get(scheduleTransportMode).get(routeStops.get(e).getStopFacility()));

				// run Dijkstra
				pseudoGraph.runDijkstra();
				LinkedList<PseudoRouteStop> pseudoPath = pseudoGraph.getShortestPseudoPath();

				if(pseudoPath == null) {
					log.warn("PseudoGraph has no path from SOURCE to DESTIONATION for transit route " + transitRoute.getId() + " from \"" + routeStops.get(0).getStopFacility().getName() + "\" to \"" + routeStops.get(routeStops.size() - 1).getStopFacility().getName() + "\"");
				} else {
					threadPseudoSchedule.put(tuple, pseudoPath);
				}

				counterPseudoRouting.incCounter();
			}
		}

		public Set<Tuple<LinkCandidate, LinkCandidate>> getArtificialLinksToBeCreated() {
			return artificialLinksToBeCreated;
		}

		public Map<Tuple<TransitLine, TransitRoute>, List<PseudoRouteStop>> getPseudoSchedule() {
			return threadPseudoSchedule;
		}
	}

	private static void setLogLevels() {
		Logger.getLogger(org.matsim.core.router.Dijkstra.class).setLevel(Level.ERROR); // suppress no route found warnings
		Logger.getLogger(org.matsim.core.network.NetworkImpl.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.network.filter.NetworkFilterManager.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessDijkstra.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessDijkstra.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessEuclidean.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessLandmarks.class).setLevel(Level.WARN);
	}
}
