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

package playground.polettif.multiModalMap.mapping.LinkScoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.PTMapper;
import playground.polettif.multiModalMap.mapping.PTMapperUtils;
import playground.polettif.multiModalMap.mapping.container.LinkWeightCalculator;
import playground.polettif.multiModalMap.mapping.container.PTPath;
import playground.polettif.multiModalMap.mapping.router.FastAStarLandmarksRouting;
import playground.polettif.multiModalMap.mapping.router.Router;
import playground.polettif.multiModalMap.tools.NetworkTools;
import playground.polettif.multiModalMap.workbench.RunOSM2Network;

import java.util.*;

/**
 * References an unmapped transit schedule to a  b network. Combines routing and referencing of stopFacilities. Creates additional
 * stop facilities if a stopFacility has more than one plausible link. @see main()
 * <p>
 * TODO doc input is modified
 *
 * @author polettif
 */
public class PTMapperLinkScoring extends PTMapper {

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
	private final static int MAX_STOPFACILITY_LINK_DISTANCE = 40;

	/**
	 * For the link score, link weights (given by routing) and the distance link-stopFacility is considered. The
	 * influence of the distance can be adjusted, 1.0 means the distance is as important as the link weight (both
	 * values are scaled 0...1). Value depends again on the accuracy of stop facility coordinates. Set lower (not
	 * less than zero however) if StopFacility coordinates are inaccurate.
	 */
	private final static double LINK_FACILITY_DISTANCE_WEIGHT = 0.0;

	/**
	 * Number of next stops that should be included in calculations for link weights.
	 * Paths are calculated for stop[i] -> stop[i+1] up to stop[i] -> stop[i+LOOKAHEAD_STOPS]
	 */
	private final static int LOOKAHEAD_STOPS = 3;

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
	public PTMapperLinkScoring(TransitSchedule schedule) {
		super(schedule);
	}

	/**
	 * Routes the unmapped MATSim Transit Schedule file to the network given by file. Writes the resulting
	 * schedule and network to xml files.<p/>
	 * <p>
	 * The mapper combines routing and referencing of stopFacilities. Additional stop facilities are created if a
	 * stopFacility has more than one plausible link. What happens for each route:
	 * <ol>
	 * <li>getting linkCandidates for each routeStop</li>
	 * <li>create a ChildStopFacility for each linkCandidate and reference that link</li>
	 * <li>routes between linkCandidates for several/all pairs of stops</li>
	 * <li>calculate linkWeights for links used by the routes (basically the more a link is used by different paths, the better its weight)</li>
	 * <li>get the most plausible link for each parent stop. The plausibilityScore is calculated with the link weight and the distance between link and stopfacility</li>
	 * <li>exchange the parent stop in the StopSequence with the ChildStopFacility which refers to the best scoring link</li>
	 * <li>routing between the now fixed stoplinks of the route</li>
	 * <li>not used facilities are removed</li>
	 * </ol>
	 * <p/>
	 *
	 * @param args <br/>[0] unmapped MATSim Transit Schedule file<br/>
	 *             [1] MATSim network file<br/>
	 *             [2] output schedule file path<br/>
	 *             [3] output network file path
	 */
	public static void main(String[] args) {
		if(args.length != 4) {
			System.out.println("Incorrect number of arguments\n[0] unmapped schedule file\n[1] network file\n[2] output schedule path\n[3]output network path");
		} else {
			mapFromFiles(args[0], args[1], args[2], args[3]);
		}
	}

	/**
	 * Routes the unmapped MATSim Transit Schedule file to the network given by file. Writes the resulting
	 * schedule and network to xml files.<p/>
	 * <p>
	 * The mapper combines routing and referencing of stopFacilities. Additional stop facilities are created if a
	 * stopFacility has more than one plausible link. What happens for each route:
	 * <ol>
	 * <li>getting linkCandidates for each routeStop</li>
	 * <li>create a ChildStopFacility for each linkCandidate and reference that link</li>
	 * <li>routes between linkCandidates for several/all pairs of stops</li>
	 * <li>calculate linkWeights for links used by the routes (basically the more a link is used by different paths, the better its weight)</li>
	 * <li>get the most plausible link for each parent stop. The plausibilityScore is calculated with the link weight and the distance between link and stopfacility</li>
	 * <li>exchange the parent stop in the StopSequence with the ChildStopFacility which refers to the best scoring link</li>
	 * <li>routing between the now fixed stoplinks of the route</li>
	 * <li>not used facilities are removed</li>
	 * </ol>
	 * <p/>
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
		new PTMapperLinkScoring(mainSchedule).mapScheduleToNetwork(mainNetwork);

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
		for(TransitStopFacility stopFacility : this.schedule.getFacilities().values()) {

			// limits number of links, for all links within search radius use Tools.findClosestLinks()
			List<Link> closestLinks = NetworkTools.findNClosestLinks(networkImpl, stopFacility.getCoord(), NODE_SEARCH_RADIUS, MAX_N_CLOSEST_LINKS, MAX_STOPFACILITY_LINK_DISTANCE);

			if(closestLinks.size() == 0) {
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
			for(Link l : closestLinks) {
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
		Router router = new FastAStarLandmarksRouting(this.network);

		/** [3]-[5]
		 * iterate through
		 * - lines
		 *   - routes
		 * 	   - stops
		 * 	   > calculate link weights
		 * 	   > get best scoring link and assign child stop facility
		 */
		for(TransitLine line : this.schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {

				List<TransitRouteStop> routeStops = route.getStops();
				LinkWeightCalculator linkWeightCalculator = new LinkWeightCalculator(routeStops);

				if(route.getTransportMode().equals("bus")) {
					counterLine.incCounter();

					/** [3]
					 * iterate through all stops of the route and get possible paths for linkCandidate pairs. Store the
					 * possible paths to calculate linkWeights later.
					 */
					for(int i = 0; i < routeStops.size(); i++) {
						TransitRouteStop currentStop = routeStops.get(i);

						// calculate linkWeight for between the current stop and some stops ahead (defined in LOOKAHEAD_STOPS)
						for(int j = i + 1; j <= i + LOOKAHEAD_STOPS && j < routeStops.size(); j++) {
							TransitRouteStop nextStop = routeStops.get(j);

							List<Link> closestLinksCurrent = allLinkCandidates.get(currentStop.getStopFacility());
							List<Link> closestLinksNext = allLinkCandidates.get(nextStop.getStopFacility());

							// look at all routes for all linkCandidate combinations
							for(Link linkCandidateCurrent : closestLinksCurrent) {
								for(Link linkCandidateNext : closestLinksNext) {
									if(!linkCandidateCurrent.equals(linkCandidateNext)) {
										LeastCostPathCalculator.Path pathCandidate = null ;
//										LeastCostPathCalculator.Path pathCandidate = router.calcLeastCostPath(linkCandidateCurrent.getToNode(), linkCandidateNext.getFromNode(), null, null);
										Logger.getLogger(RunOSM2Network.class).fatal("did not compile with the above line, thus commenting it out. kai") ;
										System.exit(-1);

										PTPath ptPath = PTMapperUtils.createPTPath(currentStop.getStopFacility(), linkCandidateCurrent, nextStop.getStopFacility(), linkCandidateNext, pathCandidate);

										linkWeightCalculator.add(ptPath);
									}
								}
							}
						}
					}

					/** [4]
					 * get the linkWeights for all links relevant for the current sequence of route stops
					 *
					 * Each possible route between two possible linkCandidates that passes a link adds 
					 * weight to the link (based on travel time). Higher weight means more paths have passed a link.
					 */
					Map<Id<Link>, Double> routeLinkWeights = linkWeightCalculator.getLinkWeights();

					/** [5]
					 * Get the best scoring link and exchange the parent stop in the routeProfile (stopSequence) with
					 * the child stop (which has the best scoring link already referenced). The score is calucated with
					 * the linkweight and the distance from the link and the stop facility {@link
					 * (Cannot modify the schedule within this loop, replacements are stored).
					 */
					for(TransitRouteStop currentStop : routeStops) {
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
		for(Id<TransitLine> lineId : stopFacilitiesToReplace.getLineIds()) {
			for(Id<TransitRoute> routeId : stopFacilitiesToReplace.getRouteIds(lineId)) {

				TransitLine line = this.schedule.getTransitLines().get(lineId);
				TransitRoute route = this.schedule.getTransitLines().get(line.getId()).getRoutes().get(routeId);
				Id<TransitRoute> oldRouteId = route.getId();
				String oldTransportMode = route.getTransportMode();
				List<TransitRouteStop> oldStopSequence = this.schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()).getStops();
				List<TransitRouteStop> newStopSequence = new ArrayList<>();

				for(TransitRouteStop stop : oldStopSequence) {
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
		for(Tuple<TransitLine, TransitRoute> entry : newTransitLines) {
			this.schedule.getTransitLines().get(entry.getFirst().getId()).addRoute(entry.getSecond());
		}

		/** [7]
		 * route all routes with the new referenced links
		 */
		PTMapperUtils.routeSchedule(schedule, network, router);

		/** [8]
		 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
		 * and all nodes which are non-linked to any link after the above cleaning...
		 * Clean also the allowed modes for only the modes, no line-number any more...
		 */
		log.info("Clean Stations and Network...");
		PTMapperUtils.cleanSchedule(schedule);
//		PTMapperUtils.addPTModeToNetwork(schedule, network);
		PTMapperUtils.removeNonUsedStopFacilities(schedule);
//		PTMapperUtils.setConnectedStopFacilitiesToIsBlocking();
		log.info("Clean Stations and Network... done.");

		log.info("Creating PT lines... done.");
	}

	/**
	 * Container class to store the lines in with routes which have parent stop facilities needing to be replaced
	 * with child stop facilities.
	 */
	private class ReplacementStorageLines {

		private Map<Id<TransitLine>, ReplacementStorageRoutes> lines = new HashMap<>();

		public void putReplacementPair(Id<TransitLine> lineId, Id<TransitRoute> routeId, TransitStopFacility parentStopFacility, TransitStopFacility childStopFacility) {
			ReplacementStorageRoutes storageRoutes;
			if(lines.containsKey(lineId)) {
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
			if(routes.containsKey(routeId)) {
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
