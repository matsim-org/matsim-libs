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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.*;
import playground.polettif.multiModalMap.mapping.router.ModeDependentRouter;
import playground.polettif.multiModalMap.mapping.router.Router;

import java.util.*;

/**
 * References an unmapped transit schedule to a  b network. Combines routing and referencing of stopFacilities. Creates additional
 * stop facilities if a stopFacility has more than one plausible link. @see main()
 *
 * Creates pseudo paths via all link candidates, chooses the path with the lowest travel time.
 * <p>
 *
 * @author polettif
 */
public class PTMapperModes extends PTMapper {

	private Map<Tuple<LinkCandidate, LinkCandidate>, Link> artificialLinks = new HashMap<>();
	Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoRoutes = new HashMap<>();

	/**
	 * Constructor.
	 * The given schedule is modified via {@link #mapScheduleToNetwork(Network)}
	 */
	public PTMapperModes(TransitSchedule schedule, PublicTransportMapConfigGroup config) {
		super(schedule, config);
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
		if (args.length != 4) {
			System.out.println("Incorrect number of arguments\n[0] unmapped schedule file\n[1] network file\n[2] output schedule path\n[3]output network path");
		} else {
			mapFromFiles(args[0], args[1], args[2], args[3]);
		}
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
		new PTMapperModes(mainSchedule, PublicTransportMapConfigGroup.createDefaultConfig()).mapScheduleToNetwork(mainNetwork);

		log.info("Writing schedule and network to file...");
		new TransitScheduleWriter(mainSchedule).writeFile(outputScheduleFile);
		new NetworkWriter(mainNetwork).write(outputNetworkFile);

		log.info("Mapping public transit to network successful!");
	}


	@Override
	public void mapScheduleToNetwork(Network networkParam) {
		this.network = networkParam;
		this.networkFactory = networkParam.getFactory();

		Map<String, Set<String>> modesAssignment = config.getModesAssignmentCopy();

		log.info("Mapping Transit Schedule to PT lines...");

		Counter counterLine = new Counter("route # ");

		// TODO (pre)define which modes should be artificially routed

		/** [.]
		 * preload closest links and create child StopFacilities
		 * if a stop facility is already referenced (manually beforehand for example) no child facilities are created
		 * stopfacilities with no links within search radius need ARTIFICIAL_LINK_MODE links and nodes before routing starts
		 */
		Map<TransitStopFacility, Map<String, Set<LinkCandidate>>> linkCandidates = PTMapperUtils.generateModeSeparatedLinkCandidates(schedule, network, config);

		/**
		 * Create differen network for all Routes, initiate routers.
		 */
		Map<String, Router> routers = getModeRouters(modesAssignment);

		/**
		 * TODO doc
		 */
		log.info("Calculating pseudoRoutes...");
		for (TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				String scheduleTransportMode = transitRoute.getTransportMode();
				boolean mapRouteToNetwork = (config.getModesAssignment().containsKey(scheduleTransportMode));

				Router modeRouter = routers.get(scheduleTransportMode);
				List<TransitRouteStop> routeStops = transitRoute.getStops();

				counterLine.incCounter();
				log.info("transitRoute " + transitRoute.getId());

				/** [.]
				 * calculate shortest paths between each link candidate
				 */
				PseudoGraph pseudoGraph = new PseudoGraph();
				DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(pseudoGraph);

				for(int i = 0; i < routeStops.size() - 1; i++) {
					TransitStopFacility currentStopFacility = routeStops.get(i).getStopFacility();
					TransitStopFacility nextStopFacility = routeStops.get(i+1).getStopFacility();

					double beelineDistance = CoordUtils.calcEuclideanDistance(currentStopFacility.getCoord(), nextStopFacility.getCoord());
					double minTravelCost = Double.MAX_VALUE;
					boolean atLeastOnerouteFound = false;

					Set<LinkCandidate> linkCandidatesCurrent = linkCandidates.get(routeStops.get(i).getStopFacility()).get(scheduleTransportMode);
					Set<LinkCandidate> linkCandidatesNext = linkCandidates.get(routeStops.get(i+1).getStopFacility()).get(scheduleTransportMode);

					if(mapRouteToNetwork) {
						for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
							for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
								LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(linkCandidateCurrent.getLink().getToNode(), linkCandidateNext.getLink().getFromNode());

								// path is null if links are on separate networks
								if(leastCostPath != null) {
									atLeastOnerouteFound = true;
									double travelCost = leastCostPath.travelTime;

									// if both links are the same, travel time should get higher since
									if(linkCandidateCurrent.getLink().equals(linkCandidateNext.getLink())) {
										travelCost *= config.getSameLinkPunishment();
									}

									if(travelCost < minTravelCost) {
										minTravelCost = travelCost;
									}

									PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
									PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(i+1, routeStops.get(i+1), linkCandidateNext);

									pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, travelCost), (i == 0), (i == routeStops.size() - 2));
								}
							}
						}
					} else {
						// put "newly found" schedule transport mode in
						modesAssignment.put(scheduleTransportMode, Collections.singleton(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE));
					}

					/**
					 * if no route exists between the current stop and the next, create an
					 * ARTIFICIAL_LINK_MODE link between all linkcandidates (usually this means between
					 * one ARTIFICIAL_LINK_MODE stopFacilityLink and the other linkCandidates.
					 *
					 * Is also applied if scheduleMode should use ARTIFICIAL_LINK_MODE links
					 */
					double maxAllowedTravelCost = beelineDistance*config.getBeelineDistanceMaxFactor();
					if(minTravelCost > maxAllowedTravelCost || !atLeastOnerouteFound) {
						for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
							for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
								createArtificialLink(linkCandidateCurrent, linkCandidateNext, config.getBeelineDistanceMaxFactor());

								PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
								PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(i+1, routeStops.get(i+1), linkCandidateNext);

								pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, maxAllowedTravelCost), (i == 0), (i == routeStops.size() - 2));
							}
						}
					}
				}

				/** [.]
				 * build pseudo network and find shortest path => List<LinkCandidate>
				 */
				dijkstra.run();
				List<PseudoRouteStop> pseudoStopSequence = MapUtils.getList(transitRoute, MapUtils.getMap(transitLine, pseudoRoutes));
				pseudoStopSequence.addAll(dijkstra.getShortesPseudoPath());
			} // - transitRoute loop
		} // - line loop

		/** [6]
		 * Replacing parent StopFacilities with child StopFacilities
		 */
		log.info("Replacing parent StopFacilities with child StopFacilities...");
		List<Tuple<TransitLine, TransitRoute>> newRoutes = new ArrayList<>();
		for(Map.Entry<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> lineEntry : pseudoRoutes.entrySet()) {
			for(Map.Entry<TransitRoute, List<PseudoRouteStop>> routeEntry : lineEntry.getValue().entrySet()) {

				List<PseudoRouteStop> pseudoStopSequence = routeEntry.getValue();
				List<TransitRouteStop> newStopSequence = new ArrayList<>();

				for(PseudoRouteStop pseudoStop : pseudoStopSequence) {
					TransitRouteStop newTransitRouteStop = scheduleFactory.createTransitRouteStop(
							pseudoStop.getChildStopFacility(), pseudoStop.getArrivalOffset(), pseudoStop.getDepartureOffset());
					newTransitRouteStop.setAwaitDepartureTime(pseudoStop.isAwaitDepartureTime());

					newStopSequence.add(newTransitRouteStop);
				}

				TransitRoute newRoute = scheduleFactory.createTransitRoute(routeEntry.getKey().getId(), null, newStopSequence, routeEntry.getKey().getTransportMode());

				// add departures
				routeEntry.getKey().getDepartures().values().forEach(newRoute::addDeparture);

				// remove the old route
				this.schedule.getTransitLines().get(lineEntry.getKey().getId()).removeRoute(routeEntry.getKey());

				// add new route to container
				newRoutes.add(new Tuple<>(lineEntry.getKey(), newRoute));
			}
		}

		// add transit lines and routes again
		for(Tuple<TransitLine, TransitRoute> entry : newRoutes) {
			this.schedule.getTransitLines().get(entry.getFirst().getId()).addRoute(entry.getSecond());
		}

		/** [7]
		 * route all routes with the new referenced links. Routers have to be initialized again
		 * since the network has changed (ARTIFICIAL_LINK_MODE links).
		 */
		Map<String, Router> finalRouters = getModeRouters(modesAssignment);
		PTMapperUtils.routeSchedule(schedule, network, finalRouters);

		/** [8]
		 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
		 * and all nodes which are non-linked to any link after the above cleaning...
		 * Clean also the allowed modes for only the modes, no line-number any more...
		 */
		log.info("Clean Stations and Network...");
//		PTMapperUtils.cleanSchedule(schedule);
		PTMapperUtils.removeNonUsedStopFacilities(schedule);
//		PTMapperUtils.addPTModeToNetwork(schedule, network);
//		PTMapperUtils.setConnectedStopFacilitiesToIsBlocking(schedule, network);
		PTMapperUtils.removeNotUsedTransitLinks(schedule, network, config.getModesToKeepOnCleanUp());
		PTMapperUtils.assignScheduleModesToLinks(schedule, network);

		// todo combine all networkModes to pt

		log.info("Clean Stations and Network... done.");

		log.info("Creating PT lines... done.");
	}

	private Map<String, Router> getModeRouters(Map<String, Set<String>> modesAssignment) {
		Map<String, Router> routers = new HashMap<>(); 	// key: ScheduleTransportMode
		for(Map.Entry<String, Set<String>> modeAssignment : modesAssignment.entrySet()) {
			log.info("Creating Router for " + modeAssignment.getKey() +" ...");
			routers.put(modeAssignment.getKey(), new ModeDependentRouter(network, modeAssignment.getValue()));
		}
		routers.put(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE, new ModeDependentRouter(network, Collections.singleton(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE)));
		return routers;
	}

	/**
	 * Creates a new link between two linkCandidates
	 */
	private Link createArtificialLink(final LinkCandidate fromLinkCandidate, final LinkCandidate toLinkCandidate, double lengthFactor) {

		Tuple<LinkCandidate, LinkCandidate> key = new Tuple<>(fromLinkCandidate, toLinkCandidate);

		if(!artificialLinks.containsKey(key)) {
			Link newLink = networkFactory.createLink(Id.createLinkId(config.getPrefixArtificial() + config.getArtificialId()),
					fromLinkCandidate.getLink().getToNode(),
					toLinkCandidate.getLink().getFromNode());

			newLink.setAllowedModes(Collections.singleton(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE));
			newLink.setLength(lengthFactor * CoordUtils.calcEuclideanDistance(fromLinkCandidate.getLink().getToNode().getCoord(), toLinkCandidate.getLink().getFromNode().getCoord()));
			network.addLink(newLink);

			artificialLinks.put(key, newLink);

			return newLink;
		}
		else {
			return artificialLinks.get(key);
		}
	}
}
