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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.DijkstraAlgorithm;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.PseudoGraph;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidatePath;
import playground.polettif.multiModalMap.mapping.router.DijkstraRouter;
import playground.polettif.multiModalMap.mapping.router.ModeLinkFilter;
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
		setNetwork(networkParam);

		log.info("Creating PT lines...");

		Counter counterLine = new Counter("route # ");

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

		/** [.]
		 * preload closest links and create child StopFacilities
		 * if a stop facility is already referenced (manually beforehand for example) no child facilities are created
		 * stopfacilities with no links within search radius need artificial links and nodes before routing starts
		 */
		Map<String, StopFacilityTree> stopFacilityTrees = new HashMap<>();
		for(String mode : config.getModes().keySet()) {
			StopFacilityTree stopFacilityTree = new StopFacilityTree(schedule, modeNetworks.get(mode), mode, config.getNodeSearchRadius(), config.getMaxNClosestLinks(), config.getMaxStopFacilityDistance());
			stopFacilityTrees.put(mode, stopFacilityTree);
		}


		/**
		 * TODO doc
		 */
		for (TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {

				StopFacilityTree stopFacilityTree = stopFacilityTrees.get(transitRoute.getTransportMode());

				String transitRouteTransportMode = transitRoute.getTransportMode();
				Router modeRouter = routers.get(transitRouteTransportMode);

				List<TransitRouteStop> routeStops = transitRoute.getStops();

				counterLine.incCounter();

				PseudoGraph pseudoGraph = new PseudoGraph();
				DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(pseudoGraph);

				/** [.]
				 * calculate shortest paths between each link candidate
				 */
				// add dummy edges and nodes to pseudoGraph before the transitRoute
				pseudoGraph.addDummyBefore(stopFacilityTree.getLinkCandidates(routeStops.get(0).getStopFacility()));

				for (int i = 0; i < routeStops.size()-1; i++) {
					boolean firstPath = false, lastPath = false;
					List<LinkCandidate> linkCandidatesCurrent = stopFacilityTree.getLinkCandidates(routeStops.get(i).getStopFacility());
					List<LinkCandidate> linkCandidatesNext = stopFacilityTree.getLinkCandidates(routeStops.get(i+1).getStopFacility());

					if(i == 0) { firstPath = true; }
					if(i == routeStops.size()-2) { lastPath = true; }

					for(LinkCandidate linkCandidateCurrent : linkCandidatesCurrent) {
						for(LinkCandidate linkCandidateNext : linkCandidatesNext) {
							LeastCostPathCalculator.Path leastCostPath = modeRouter.calcLeastCostPath(linkCandidateCurrent.getLink().getToNode(), linkCandidateNext.getLink().getFromNode());

							double travelTime = leastCostPath.travelTime;

							// if both links are the same and to link are the same, travel time should get higher since those
							if(linkCandidateCurrent.getLink().equals(linkCandidateNext.getLink()))	{
								travelTime = travelTime * config.getSameLinkPunishment();
							}

							pseudoGraph.addPath(new LinkCandidatePath(linkCandidateCurrent, linkCandidateNext, travelTime, firstPath, lastPath));
						}
					}


					// add dummy edges and nodes to pseudoGraph before the transitRoute
					pseudoGraph.addDummyAfter(stopFacilityTree.getLinkCandidates(routeStops.get(routeStops.size()-1).getStopFacility()));

					/* [.]
					 * build pseudo network and find shortest path => List<LinkCandidate>
					 */
					dijkstra.run();
					stopFacilityTree.setReplacementPairs(transitLine, transitRoute, dijkstra.getBestLinkCandidates());
				}
			} // - transitRoute loop
		} // - line loop

		/** [6]
		 *
		 */
		log.info("Replacing parent StopFacilities with child StopFacilities...");
		for(StopFacilityTree stopFacilityTree : stopFacilityTrees.values()) {
			stopFacilityTree.replaceParentWithChildStopFacilities();
		}

		/** [7]
		 * route all routes with the new referenced links
		 */
		PTMapperUtils.routeSchedule(schedule, network, routers);

		// TODO merge child stops with same link ref but different modes

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
		PTMapperUtils.removeNonTransitLinks(schedule, network, config.getModesToCleanUp());
		log.info("Clean Stations and Network... done.");

		log.info("Creating PT lines... done.");
	}
}
