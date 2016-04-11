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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.container.LinkWeightCalculator;
import playground.polettif.multiModalMap.mapping.container.PTPath;
import playground.polettif.multiModalMap.mapping.router.PTPathCalculator;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.*;

/**
 * References an unmapped transit schedule to a  b network. Combines routing and referencing of stopFacilities. Creates additional
 * stop facilities if a stopFacility has more than one plausible link. @see main()
 *
 * Creates pseudo paths via all link candidates, chooses the path with the lowest travel time.
 * <p>
 * TODO doc input is modified
 *
 * @author polettif
 */
public class PTMapperConstrainedRolloverShortestPath extends PTMapper {

	// TODO ensure coordinate system is not WGS84 since this is not suitable for coordinate calculations (or warn accordingly)
	// TODO move params to a config?

	/**
	 * Defines the radius [meter] from a stop facility within nodes are searched.
	 * Mainly a maximum value for performance.
	 */
	private final static double NODE_SEARCH_RADIUS = 300;

	/**
	 * Number of link candidates considered for all stops, depends on accuracy of
	 * stops and desired performance. Somewhere between 4 and 10 seems reasonable,
	 * depending on the accuracy of the stop facility coordinates.
	 */
	private final static int MAX_N_CLOSEST_LINKS = 12;

	/**
	 * The maximal distance [meter] a link candidate is allowed to have from the stop facility.
	 */
	private final static int MAX_STOPFACILITY_LINK_DISTANCE = 50;

	/**
	 * For the link score, link weights (given by routing) and the distance link-stopFacility is considered. The
	 * influence of the distance can be adjusted, 1.0 means the distance is as important as the link weight (both
	 * values are scaled 0...1). Value depends again on the accuracy of stop facility coordinates. Set lower (not
	 * less than zero however) if StopFacility coordinates are inaccurate.
	 */
	private final static double LINK_FACILITY_DISTANCE_WEIGHT = 0.0;

	/**
	 * ID prefix used for artificial link created if no nodes are found within {@link #NODE_SEARCH_RADIUS}
	 */
	private final static String PREFIX_ARTIFICIAL_LINKS = "pt_";

	/**
	 * Suffix used for child stop facilities. A number for each child of a parent stop facility is appended (i.e. stop0123_fac:2)
	 */
	private final static String SUFFIX_CHILD_STOPFACILITIES = "_fac:";

	/**
	 * fields
	 */
	private int artificialId = 0;
	private Map<TransitStopFacility, List<Link>> allLinkCandidates = new HashMap<>();
	private ReplacementStorageLines stopFacilitiesToReplace = new ReplacementStorageLines();
	private HashMap<Tuple<TransitStopFacility, Id<Link>>, TransitStopFacility> refStopFacility = new HashMap<>();
	private HashMap<TransitStopFacility, Integer> childFacilityCounter = new HashMap<>();
	private List<Tuple<TransitLine, TransitRoute>> newTransitLines = new ArrayList<>();


	/**
	 * Constructor
	 */
	public PTMapperConstrainedRolloverShortestPath(TransitSchedule schedule) {
		super(schedule);
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
		new PTMapperConstrainedRolloverShortestPath(mainSchedule).mapScheduleToNetwork(mainNetwork);

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

		/** [1]
		 * preload closest links and create child StopFacilities
		 * if a stop facility is already referenced (manually beforehand for example) no child facilities are created
		 * stopfacilities with no links within search radius need artificial links and nodes before routing starts
		 */
		List<TransitStopFacility> newFacilities = new ArrayList<>();
		NetworkImpl networkImpl = ((NetworkImpl) network); // used by search for nearest node
		for (TransitStopFacility stopFacility : this.schedule.getFacilities().values()) {

			// limits number of links, for all links within search radius use Tools.findClosestLinks()
			List<Link> closestLinks = NetworkTools.findOnlyNClosestLinks(networkImpl, stopFacility.getCoord(), NODE_SEARCH_RADIUS, MAX_N_CLOSEST_LINKS);

			if (closestLinks == null) {
				Node newNode = networkFactory.createNode(Id.create(PREFIX_ARTIFICIAL_LINKS + artificialId++, Node.class), stopFacility.getCoord());
				Node nearestNode = networkImpl.getNearestNode(stopFacility.getCoord());
				Link newLink = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), newNode, nearestNode);
				Link newLink2 = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), nearestNode, newNode);

				network.addNode(newNode);
				network.addLink(newLink);
				network.addLink(newLink2);

				closestLinks = new ArrayList<>();
				closestLinks.add(newLink);
			}
			// store closest links in database
			allLinkCandidates.put(stopFacility, closestLinks);

			/** [2]
			 * generate child stop facilities and reference them
			 */
			for (Link l : closestLinks) {
				Integer counter = (childFacilityCounter.containsKey(stopFacility) ? childFacilityCounter.put(stopFacility, childFacilityCounter.get(stopFacility) + 1) + 1 : childFacilityCounter.put(stopFacility, 1));
				counter = (counter == null ? 1 : counter);
				TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
						Id.create(stopFacility.getId() + SUFFIX_CHILD_STOPFACILITIES + counter, TransitStopFacility.class),
						stopFacility.getCoord(),
						stopFacility.getIsBlockingLane()
				);
				newFacility.setLinkId(l.getId());
				newFacility.setName(stopFacility.getName());
				newFacility.setStopPostAreaId(stopFacility.getStopPostAreaId());
				newFacilities.add(newFacility);

				refStopFacility.put(new Tuple<>(stopFacility, l.getId()), newFacility);
			}

		}

		// assign new facilities to schedule
		newFacilities.forEach(this.schedule::addStopFacility);

		// initiate router
		PTPathCalculator router = new PTPathCalculator(this.network);

		/** [3]-[5]
		 * iterate through
		 * - lines
		 *   - routes
		 * 	   - stops
		 * 	   > calculate link weights
		 * 	   > get best scoring link and assign child stop facility
		 */
		Map<Tuple<Link, Link>, PTPath> shortestPathMap = new HashMap<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {

				List<TransitRouteStop> routeStops = route.getStops();

				LinkWeightCalculator linkWeightCalculator = new LinkWeightCalculator(routeStops);

				if (route.getTransportMode().equals("bus")) {
					counterLine.incCounter();

					Map<Id<Link>, Double> routeLinkWeights = new HashMap<>();

					/** [3]
					 * iterate from the second to the penultimate stop and calculate all possible paths between the link
					 * candidates.
					 */
					for (int i = 0; i < routeStops.size()-1; i++) {
						TransitRouteStop currentStop = routeStops.get(i);

						List<Link> linkCandidatesCurrent = allLinkCandidates.get(currentStop.getStopFacility());
						List<Link> linkCandidatesNext = allLinkCandidates.get(routeStops.get(i+1).getStopFacility());

						for(Link linkCurrent : linkCandidatesCurrent) {
							for(Link linkNext : linkCandidatesNext) {
								Tuple<Link, Link> key = new Tuple<>(linkCurrent, linkNext);
								if(!shortestPathMap.containsKey(key)) {
								//	LeastCostPathCalculator.Path leastCostPath = router.calcLeastCostPath(linkCurrent.getToNode(), linkNext.getFromNode());
								//	PTPath ptPath = PTMapperUtils.createPTPath(routeStops.get(i).getStopFacility(), linkCurrent, routeStops.get(j).getStopFacility(), linkNext, leastCostPath);

								//	shortestPathMap.put(key, ptPath);
								}
							}
						}
					}

					/** [4]
					 * get the linkWeights for all links relevant for the current sequence of route stops
					 */
			//		List<Id<Link>> DEBUGrouteLinkWeights = PTMapperUtils.getShortestPTPath(shortestPathMap);

					// debug
					if(route.getId().toString().equals("line72_01531_000")) {  //line404_00400_001
						log.info("visualizing route");
						NetworkTools.visualizeWeightsAsLanes(network, routeLinkWeights);
					}

					/** [.]
					 * route from the first stop to the last with link weights as disutility
					 */
//					router.setWeights(routeLinkWeights);

					/** [5]
					 * Get the best scoring link and exchange the parent stop in the routeProfile (stopSequence) with
					 * the child stop (which has the best scoring link already referenced). The score is calucated with
					 * the linkweight and the distance from the link and the stop facility {@link
					 * (Cannot modify the schedule within this loop, replacements are stored).
					 */
					for (TransitRouteStop currentStop : routeStops) {
						Link mostPlausibleLink = PTMapperUtils.getMostPlausibleLink(currentStop.getStopFacility(), allLinkCandidates.get(currentStop.getStopFacility()), routeLinkWeights, LINK_FACILITY_DISTANCE_WEIGHT);
						stopFacilitiesToReplace.putReplacementPair(line.getId(), route.getId(), currentStop.getStopFacility(), refStopFacility.get(new Tuple<>(currentStop.getStopFacility(), mostPlausibleLink.getId())));
					}

				}
			} // - route loop

		} // - line loop

		/** [6]
		 * Actually replace the parent stopFacilities in the schedule
		 */
		log.info("Replacing StopFacilities with ChildStopFacilities...");
		for (Id<TransitLine> lineId : stopFacilitiesToReplace.getLineIds()) {
			for (Id<TransitRoute> routeId : stopFacilitiesToReplace.getRouteIds(lineId)) {

				TransitLine line = this.schedule.getTransitLines().get(lineId);
				TransitRoute route = this.schedule.getTransitLines().get(line.getId()).getRoutes().get(routeId);
				Id<TransitRoute> oldRouteId = route.getId();
				String oldTransportMode = route.getTransportMode();
				List<TransitRouteStop> oldStopSequence = this.schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()).getStops();
				List<TransitRouteStop> newStopSequence = new ArrayList<>();

				for (TransitRouteStop stop : oldStopSequence) {
					TransitRouteStop newTransitRouteStop = scheduleFactory.createTransitRouteStop(stopFacilitiesToReplace.getStoredRoute(lineId).getReplacementPairs(routeId).get(stop.getStopFacility()), stop.getArrivalOffset(), stop.getDepartureOffset());
					newTransitRouteStop.setAwaitDepartureTime(stop.isAwaitDepartureTime());
					newStopSequence.add(newTransitRouteStop);
				}

				TransitRoute newRoute = scheduleFactory.createTransitRoute(oldRouteId, null, newStopSequence, oldTransportMode);

				// add departures
				route.getDepartures().values().forEach(newRoute::addDeparture);

				this.schedule.getTransitLines().get(line.getId()).removeRoute(route);

				newTransitLines.add(new Tuple<>(line, newRoute));
			}
		}

		// add transit lines and routes again
		for (Tuple<TransitLine, TransitRoute> entry : newTransitLines) {
			this.schedule.getTransitLines().get(entry.getFirst().getId()).addRoute(entry.getSecond());
		}

		/** [7]
		 * route all routes with the new referenced links
		 */
		// todo maybe put it back into the previous loop?
		log.info("Routing all routes with referenced links...");
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				// TODO change modes
				if (route.getTransportMode().equals("bus")) {

					List<TransitRouteStop> routeStops = route.getStops();
					List<Id<Link>> linkSequence = new ArrayList<>();

					// add very first link
					linkSequence.add(routeStops.get(0).getStopFacility().getLinkId());

					// route backwards (from stop to previous stop)
					for (int i = 1; i < routeStops.size(); i++) {
						TransitRouteStop previousStop = routeStops.get(i - 1);
						Link previousLink = network.getLinks().get(previousStop.getStopFacility().getLinkId());
						TransitRouteStop currentStop = routeStops.get(i);
						Link currentLink = network.getLinks().get(currentStop.getStopFacility().getLinkId());

						List<Id<Link>> path = PTMapperUtils.getLinkIdsFromPath(router.calcLeastCostPath(previousLink.getToNode(), currentLink.getFromNode(), null, null));

						if (path != null)
							linkSequence.addAll(path);

						linkSequence.add(currentLink.getId());
					}
					// add link sequence to schedule
					route.setRoute(RouteUtils.createNetworkRoute(linkSequence, this.network));
				}
			}
		}

		/** [8]
		 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
		 * and all nodes which are non-linked to any link after the above cleaning...
		 * Clean also the allowed modes for only the modes, no line-number any more...
		 */
		log.info("Clean Stations and Network...");
//		cleanSchedule();
//		addPTModeToNetwork();
		PTMapperUtils.removeNonUsedStopFacilities(schedule);
//		setConnectedStopFacilitiesToIsBlocking();
		log.info("Clean Stations and Network... done.");

		log.info("Creating PT lines... done.");
	}

	/**
	 * cleanStationsAndNetwork
	 * boescpa
	 */
	private void setConnectedStopFacilitiesToIsBlocking() {
		Set<TransitStopFacility> facilitiesToExchange = new HashSet<>();
		for (TransitStopFacility oldFacility : this.schedule.getFacilities().values()) {
			if (this.network.getLinks().get(oldFacility.getLinkId()).getAllowedModes().contains(TransportMode.car)) {
				TransitStopFacility newFacility = this.scheduleFactory.createTransitStopFacility(
						oldFacility.getId(), oldFacility.getCoord(), true);
				newFacility.setName(oldFacility.getName());
				newFacility.setLinkId(oldFacility.getLinkId());
				newFacility.setStopPostAreaId(oldFacility.getStopPostAreaId());
				facilitiesToExchange.add(newFacility);
			}
		}
		for (TransitStopFacility facility : facilitiesToExchange) {
			TransitStopFacility facilityToRemove = this.schedule.getFacilities().get(facility.getId());
			this.schedule.removeStopFacility(facilityToRemove);
			this.schedule.addStopFacility(facility);
		}
	}

	/**
	 * cleanStationsAndNetwork
	 * boescpa
	 */
	private void cleanSchedule() {
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			Set<TransitRoute> toRemove = new HashSet<>();
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();
				if (networkRoute.getStartLinkId() == null || networkRoute.getEndLinkId() == null) {
					removeRoute = true;
				}
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					if (linkId == null) {
						removeRoute = true;
					}
				}
				if (removeRoute) {
					log.error("NetworkRoute for " + transitRoute.getId().toString() + " incomplete. Remove route.");
					toRemove.add(transitRoute);
				}
			}
			if (!toRemove.isEmpty()) {
				for (TransitRoute transitRoute : toRemove) {
					line.removeRoute(transitRoute);
				}
			}
		}
	}

	/**
	 * cleanStationsAndNetwork
	 * Add to any link that is passed by any route a "pt" in the modes, if it hasn't already one...
	 * boescpa
	 */
	private void addPTModeToNetwork() {
		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinks = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();
				transitLinks.add(networkRoute.getStartLinkId());
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					transitLinks.add(linkId);
				}
				transitLinks.add(networkRoute.getEndLinkId());
			}
		}
		for (Id<Link> transitLinkId : transitLinks) {
			Link transitLink = networkLinks.get(transitLinkId);
			if (!transitLink.getAllowedModes().contains(TransportMode.pt)) {
				Set<String> modes = new HashSet<>();
				modes.addAll(transitLink.getAllowedModes());
				modes.add(TransportMode.pt);
				transitLink.setAllowedModes(modes);
			}
		}
	}

	/**
	 * Container class to store the lines in with routes which have parent stop facilities needing to be replaced
	 * with child stop facilities.
	 */
	private class ReplacementStorageLines {

		private Map<Id<TransitLine>, ReplacementStorageRoutes> lines = new HashMap<>();

		public void putReplacementPair(Id<TransitLine> lineId, Id<TransitRoute> routeId, TransitStopFacility parentStopFacility, TransitStopFacility childStopFacility) {
			ReplacementStorageRoutes storageRoutes;
			if (lines.containsKey(lineId)) {
				storageRoutes = lines.get(lineId);
			} else {
				storageRoutes = new ReplacementStorageRoutes();
			}
			storageRoutes.addPair(routeId, parentStopFacility, childStopFacility);
			lines.put(lineId, storageRoutes);
		}

		public Set<Id<TransitLine>> getLineIds() {
			return lines.keySet();
		}

		public Set<Id<TransitRoute>> getRouteIds(Id<TransitLine> lineId) {
			return lines.get(lineId).getRouteIds();
		}

		public ReplacementStorageRoutes getStoredRoute(Id<TransitLine> lineId) {
			return lines.get(lineId);
		}
	}

	/**
	 * Container class to store routes with parent stop facilities that need to be replaced with child stop facilities.
	 */
	private class ReplacementStorageRoutes {

		private Map<Id<TransitRoute>, Map<TransitStopFacility, TransitStopFacility>> routes = new HashMap<>();

		public void addPair(Id<TransitRoute> routeId, TransitStopFacility parentStopFacility, TransitStopFacility childStopFacility) {
			Map<TransitStopFacility, TransitStopFacility> tmp;
			if (routes.containsKey(routeId)) {
				tmp = routes.get(routeId);
			} else {
				tmp = new HashMap<>();
			}

			tmp.put(parentStopFacility, childStopFacility);

			routes.put(routeId, tmp);
		}

		public Set<Id<TransitRoute>> getRouteIds() {
			return routes.keySet();
		}

		public Map<TransitStopFacility, TransitStopFacility> getReplacementPairs(Id<TransitRoute> routeId) {
			return routes.get(routeId);
		}
	}
}
