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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.boescpa.lib.tools.spatialCutting.NetworkCutter;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.*;
import playground.polettif.multiModalMap.mapping.router.ModeDependentRouter;
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

	private static final double HIGH_TT_VALUE = 10800;

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

		Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoRoutes = new HashMap<>();

		/** [.]
		 * createPseudoNetwork for all modes. Link travel times are very high, so routing puts the transitroute
		 * on the "real" network where available.
		 * TODO only create pseudoNetwork outside of AOI otherwise we get way too many links
		 */

//		Coord[] extent = NetworkTools.getExtent(network);

		Network independentNetwork = NetworkUtils.createNetwork();
		IndependentNetworkCreator independentNetworkCreator = new IndependentNetworkCreator(schedule, independentNetwork, config);
		independentNetworkCreator.createNetwork();

		new NetworkWriter(independentNetwork).write("C:/Users/Flavio/Desktop/output/test/independentNetwork.xml");
//		independentNetworkCreator.removeLinksWithinAOI(extent[0], extent[1]);
		independentNetworkCreator.removeLinksWithinAOI(new Coord(2674070.0, 1152880.0), new Coord(2705500.0, 1208000.0));
		new NetworkWriter(network).write("C:/Users/Flavio/Desktop/output/test/network.xml");
		new NetworkWriter(independentNetwork).write("C:/Users/Flavio/Desktop/output/test/independentNetwork_cut.xml");

		/** [.]
		 * preload closest links and create child StopFacilities
		 * if a stop facility is already referenced (manually beforehand for example) no child facilities are created
		 * stopfacilities with no links within search radius need artificial links and nodes before routing starts
		 */
		Map<TransitStopFacility, List<LinkCandidate>> linkCandidates = PTMapperUtils.generateLinkCandidates(schedule, network, config);

		/**
		 * Create differen network for all Routes, initiate routers.
		 */
		Map<String, Router> routers = new HashMap<>(); 	// key: ScheduleTransportMode
		for(Map.Entry<String, Set<String>> modeAssignment : config.getModes().entrySet()) {
			routers.put(modeAssignment.getKey(), new ModeDependentRouter(network, modeAssignment.getValue()));
		}

		/**
		 * TODO doc
		 */
		log.info("Calculating pseudoRoutes...");
		for (TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(config.getModes().keySet().contains(transitRoute.getTransportMode())) {
					String transitRouteTransportMode = transitRoute.getTransportMode();
					Router modeRouter = routers.get(transitRouteTransportMode);

					List<TransitRouteStop> routeStops = transitRoute.getStops();

					counterLine.incCounter();

					/** [.]
					 * calculate shortest paths between each link candidate
					 */
					PseudoGraph pseudoGraph = new PseudoGraph();
					DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(pseudoGraph);

					for(int i = 0; i < routeStops.size() - 1; i++) {

						List<LinkCandidate> linkCandidatesCurrent = linkCandidates.get(routeStops.get(i).getStopFacility());
						List<LinkCandidate> linkCandidatesNext = linkCandidates.get(routeStops.get(i + 1).getStopFacility());

						for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
							for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
								LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(linkCandidateCurrent.getLink().getToNode(), linkCandidateNext.getLink().getFromNode());

								double travelTime;

								// path is null if links are on separate networks
								if(leastCostPath != null) {
									travelTime = leastCostPath.travelTime;

									// if both links are the same, travel time should get higher since
									if(linkCandidateCurrent.getLink().equals(linkCandidateNext.getLink())) {
										travelTime *= config.getSameLinkPunishment();
									}
								} else {
									travelTime = HIGH_TT_VALUE;
								}

 								PseudoRouteStop pseudoRouteStopCurrent = new PseudoRouteStop(i, routeStops.get(i), linkCandidateCurrent);
								PseudoRouteStop pseudoRouteStopNext = new PseudoRouteStop(i+1, routeStops.get(i+1), linkCandidateNext);

								pseudoGraph.addPath(new PseudoRoutePath(pseudoRouteStopCurrent, pseudoRouteStopNext, travelTime), (i == 0), (i == routeStops.size() - 2));
							}
						}
					}

					/** [.]
					 * build pseudo network and find shortest path => List<LinkCandidate>
					 */
					dijkstra.run();
					List<PseudoRouteStop> pseudoStopSequence = MapUtils.getList(transitRoute, MapUtils.getMap(transitLine, pseudoRoutes));
					pseudoStopSequence.addAll(dijkstra.getShortesPseudoPath());

				}
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
		 * route all routes with the new referenced links
		 */
		PTMapperUtils.routeSchedule(schedule, network, routers);

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
//		PTMapperUtils.removeNotUsedTransitLinks(schedule, network, config.getModesToKeepOnCleanUp());
		log.info("Clean Stations and Network... done.");

		log.info("Creating PT lines... done.");
	}
}
