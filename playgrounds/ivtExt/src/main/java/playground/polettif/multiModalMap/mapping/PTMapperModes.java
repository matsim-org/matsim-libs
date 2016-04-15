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

import gnu.trove.map.hash.TByteIntHashMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.*;
import playground.polettif.multiModalMap.mapping.router.ModeDependentRouter;
=======
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.DijkstraAlgorithm;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.PseudoGraph;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidatePath;
import playground.polettif.multiModalMap.mapping.router.DijkstraRouter;
import playground.polettif.multiModalMap.mapping.router.ModeLinkFilter;
>>>>>>> setup default config, new mode specific ptmapper
import playground.polettif.multiModalMap.mapping.router.Router;
import playground.polettif.multiModalMap.tools.NetworkTools;

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

<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
	private static final double HIGH_TT_VALUE = 10800;

=======
>>>>>>> setup default config, new mode specific ptmapper
	/**
	 * Constructor.
	 * The given schedule is modified via {@link #mapScheduleToNetwork(Network)}
	 */
	public PTMapperModes(TransitSchedule schedule) {
		super(schedule);
	}

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

		log.info("Creating PT lines...");

		Counter counterLine = new Counter("route # ");

<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
		Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoRoutes = new HashMap<>();

		/** [.]
		 * createPseudoNetwork for all modes. Link travel times are very high, so routing puts the transitroute
		 * on the "real" network where available.
		 * TODO only create pseudoNetwork outside of AOI otherwise we get way too many links
		 */
//		new IndependentNetworkCreator(schedule, network, config.getPrefixArtificialLinks()).createNetwork();
//		new NetworkWriter(network).write("E:/output/test/independentNetworks.xml");
=======
		/**
		 * Create differen network for all Routes, initiate routers.
		 * TODO move to separate class, preprocess data
 		 */
		Map<String, Network> modeNetworks = new HashMap<>(); // key: ScheduleTransportMode
		Map<String, Router> routers = new HashMap<>(); 	// key: ScheduleTransportMode
		for(Map.Entry<String, String> mode : config.getModes().entrySet()) {
			log.info("Creating network for transportMode " + mode.getKey());
			Network newNetwork = NetworkTools.getFilteredNetwork(network, new ModeLinkFilter(mode.getValue()), null);
			modeNetworks.put(mode.getKey(), newNetwork);
			new NetworkWriter(newNetwork).write("C:/Users/polettif/Desktop/output/modeNetworks/zh_"+mode.getKey()+".xml");
			routers.put(mode.getKey(), new DijkstraRouter(newNetwork));
		}
>>>>>>> setup default config, new mode specific ptmapper

		/** [.]
		 * preload closest links and create child StopFacilities
		 * if a stop facility is already referenced (manually beforehand for example) no child facilities are created
		 * stopfacilities with no links within search radius need artificial links and nodes before routing starts
		 */
<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
		Map<TransitStopFacility, List<LinkCandidate>> linkCandidates = PTMapperUtils.generateLinkCandidates(schedule, network, config);

		/**
		 * Create differen network for all Routes, initiate routers.
		 */
		Map<String, Router> routers = new HashMap<>(); 	// key: ScheduleTransportMode
		for(Map.Entry<String, Set<String>> modeAssignment : config.getModes().entrySet()) {
			routers.put(modeAssignment.getKey(), new ModeDependentRouter(network, modeAssignment.getValue()));
		}
=======
		Map<String, StopFacilityTree> stopFacilityTrees = new HashMap<>();
		for(String mode : config.getModes().keySet()) {
			StopFacilityTree stopFacilityTree = new StopFacilityTree(schedule, modeNetworks.get(mode), mode, config.getNodeSearchRadius(), config.getMaxNClosestLinks(), config.getMaxStopFacilityDistance());
			stopFacilityTrees.put(mode, stopFacilityTree);
		}

>>>>>>> setup default config, new mode specific ptmapper

		/**
		 * TODO doc
		 */
		for (TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(config.getModes().keySet().contains(transitRoute.getTransportMode())) {
					String transitRouteTransportMode = transitRoute.getTransportMode();
					Router modeRouter = routers.get(transitRouteTransportMode);

<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
					List<TransitRouteStop> routeStops = transitRoute.getStops();
=======
				StopFacilityTree stopFacilityTree = stopFacilityTrees.get(transitRoute.getTransportMode());

				String transitRouteTransportMode = transitRoute.getTransportMode();
				Router modeRouter = routers.get(transitRouteTransportMode);

				List<TransitRouteStop> routeStops = transitRoute.getStops();
>>>>>>> setup default config, new mode specific ptmapper

					counterLine.incCounter();

					if(transitRoute.getId().toString().equals("line404_00400_001")) {
						log.debug("break");
					}

					/** [.]
					 * calculate shortest paths between each link candidate
					 */
					PseudoGraph pseudoGraph = new PseudoGraph();
					DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(pseudoGraph);

<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
					for(int i = 0; i < routeStops.size() - 1; i++) {
						List<LinkCandidate> linkCandidatesCurrent = linkCandidates.get(routeStops.get(i).getStopFacility());
						List<LinkCandidate> linkCandidatesNext = linkCandidates.get(routeStops.get(i + 1).getStopFacility());

						for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
							for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
								LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(linkCandidateCurrent.getLink().getToNode(), linkCandidateNext.getLink().getFromNode());
=======
				/** [.]
				 * calculate shortest paths between each link candidate
				 */
				// add dummy edges and nodes to pseudoGraph before the transitRoute
				pseudoGraph.addDummyBefore(stopFacilityTree.getLinkCandidates(routeStops.get(0).getStopFacility()));
>>>>>>> setup default config, new mode specific ptmapper

								double travelTime;

								// path is null if links are on separate networks
								if(leastCostPath != null) {
									travelTime = leastCostPath.travelTime;

<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
									// if both links are the same, travel time should get higher since
									if(linkCandidateCurrent.getLink().equals(linkCandidateNext.getLink())) {
										travelTime *= config.getSameLinkPunishment();
									}
								} else {
									travelTime = HIGH_TT_VALUE;
								}
=======
					for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
						for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
							LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(linkCandidateCurrent.getLink().getToNode(), linkCandidateNext.getLink().getFromNode());
>>>>>>> setup default config, new mode specific ptmapper

//								Map<LinkCandidate, PseudoRouteStop> pseudoRouteStopCurrent = MapUtils.getMap(linkCandidateCurrent, MapUtils.getMap(routeStops.get(i), pseudoRouteStops));
								PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(routeStops.get(i), linkCandidateCurrent);
								PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(routeStops.get(i+1), linkCandidateNext);

								pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, travelTime), (i == 0), (i == routeStops.size() - 2));
							}
						}
					}

					//debug
					Set<PseudoRoutePath> paths = pseudoGraph.getEdges();
					PseudoRoutePath path = null;

					for(PseudoRoutePath p : paths) {
						if(p.getId().getFirst().getId().equals("source")) {
							path = p;
							break;
						}
					}

					while(!path.getToPseudoStop().isDestination()) {
						log.warn("starting from " + path.getFromPseudoStop());
						for(PseudoRoutePath p : paths) {
							log.info(p.getFromPseudoStop());
							if(p.getToPseudoStop().equals(path.getFromPseudoStop())) {
								path = p;
								log.info("found one");
								break;
							}
						}
					}
					// /debug

					/** [.]
					 * build pseudo network and find shortest path => List<LinkCandidate>
					 */
					dijkstra.run();
					List<PseudoRouteStop> linkCandidateSequence = MapUtils.getList(transitRoute, MapUtils.getMap(transitLine, pseudoRoutes));
					linkCandidateSequence.addAll(dijkstra.getShortesPseudoPath());

				}
			} // - transitRoute loop
		} // - line loop

		/** [6]
		 * Replacing parent StopFacilities with child StopFacilities
		 */
		log.info("Replacing parent StopFacilities with child StopFacilities...");
<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
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
=======
		for(StopFacilityTree stopFacilityTree : stopFacilityTrees.values()) {
			stopFacilityTree.replaceParentWithChildStopFacilities();
>>>>>>> setup default config, new mode specific ptmapper
		}

		/** [7]
		 * route all routes with the new referenced links
		 */
		PTMapperUtils.routeSchedule(schedule, network, routers);
<<<<<<< b1b7094614681fac2e0579491657bb3f28927df4
=======

		// TODO merge child stops with same link ref but different modes
>>>>>>> setup default config, new mode specific ptmapper

		/** [8]
		 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
		 * and all nodes which are non-linked to any link after the above cleaning...
		 * Clean also the allowed modes for only the modes, no line-number any more...
		 */
		log.info("Clean Stations and Network...");
//		PTMapperUtils.cleanSchedule(schedule);
//		PTMapperUtils.removeNonUsedStopFacilities(schedule);
//		PTMapperUtils.addPTModeToNetwork(schedule, network);
//		PTMapperUtils.setConnectedStopFacilitiesToIsBlocking(schedule, network);
		PTMapperUtils.removeNonTransitLinks(schedule, network, config.getModesToCleanUp());
		log.info("Clean Stations and Network... done.");

		log.info("Creating PT lines... done.");
	}
}
