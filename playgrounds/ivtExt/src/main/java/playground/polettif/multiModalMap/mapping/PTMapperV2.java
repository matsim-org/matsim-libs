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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.gtfs.GTFSReader;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRFastAStarLandmarksSimpleRouting;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRouter;
import playground.polettif.multiModalMap.mapping.containter.SolvedSubRoutes;
import playground.polettif.multiModalMap.mapping.containter.InterStopRoute;

import java.util.*;

/**
 * Default implementation of PTLinesCreator.
 *
 * V2: combined linking stopFacilities to links and routing
 * looks at pairs of stops and link candidates and uses best route
 *
 * @author polettif
 */
public class PTMapperV2 extends PTMapper {

	// TODO ensure coordinate system is not WGS84 since this is not suitable for coordinate calculations

	// params TODO move to a config?
	private final static double SEARCH_RADIUS = 300; //[m] 150
	private final static int MAX_N_CLOSEST_LINKS = 4; // number of link candidates considered for all stops

	// not used
	private final static double MAX_FACILITY_NODE_DISTANCE = 100;
	private final static double MAX_FACILITY_LINK_DISTANCE = 50;

	private final static double WEIGHT_TRAVELTIME_COMPARED_TO_STOPFACILITY_DISTANCE = 1.0;
	private final static boolean CONNECTION_TO_ALL_LINKS_WITHIN_SEARCH_AREA = false;

	private final static String PREFIX_SPLIT_LINKS = "split_";
	private final static String PREFIX_ARTIFICIAL_LINKS = "pt_";

	// TODO use transit modes
	private final Set<String> transitModes = Collections.singleton(TransportMode.pt);

	int artificialId = 0;
	private PTLRouter router = null;
	private Map<TransitStopFacility, List<Link>> allClosestLinks = new HashMap<>();

	public PTMapperV2(TransitSchedule schedule) {
		super(schedule);
	}

	public PTMapperV2(TransitSchedule schedule, Network network) {
		super(schedule, network);
	}

	@Override
	public void routePTLines(Network networkParam) {
		setNetwork(networkParam);

		// TODO add DUMMY_LINK to avoid null pointer stuff
		// TODO remove after routing
		this.network.addNode(networkFactory.createNode(Id.createNodeId(GTFSReader.DUMMY_NODE_1), new Coord(0.0, 0.0)));
		this.network.addNode(networkFactory.createNode(Id.createNodeId(GTFSReader.DUMMY_NODE_2), new Coord(0.0, 0.0)));
		this.network.addLink(networkFactory.createLink(Id.createLinkId(GTFSReader.DUMMY_LINK), network.getNodes().get(Id.createNodeId(GTFSReader.DUMMY_NODE_1)), network.getNodes().get(Id.createNodeId(GTFSReader.DUMMY_NODE_2))));

		log.info("Creating PT lines...");

		Counter counterLine = new Counter("route # ");




		/*
		* preload closest links
		* stopfacilities with no links within search radius need artificial links and nodes before routing starts
		*/
		NetworkImpl networkImpl = ((NetworkImpl) network); // used by search for nearest node
		for(TransitStopFacility stopFacility : this.schedule.getFacilities().values()) {
			// limits number of links, for all links within search radius use Tools.findClosestLinks()
			List<Link> closestLinks = Tools.findOnlyNClosestLinks(networkImpl, stopFacility.getCoord(), SEARCH_RADIUS, MAX_N_CLOSEST_LINKS);

			if (closestLinks == null) {
				Node newNode = networkFactory.createNode(Id.create(PREFIX_ARTIFICIAL_LINKS + artificialId++, Node.class), stopFacility.getCoord());
				Node nearestNode = networkImpl.getNearestNode(stopFacility.getCoord());
				Link newLink = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), newNode, nearestNode);
				Link newLink2 = networkFactory.createLink(Id.createLinkId(PREFIX_ARTIFICIAL_LINKS + artificialId++), nearestNode, newNode);

				network.addNode(newNode);
				network.addLink(newLink);
				network.addLink(newLink2);

				List<Link> newList = new ArrayList<>();
				newList.add(newLink);
				allClosestLinks.put(stopFacility, newList);
			} else {
				allClosestLinks.put(stopFacility, closestLinks);
			}
		}

		/*
		* initiate router
		*/
		this.router = new PTLRFastAStarLandmarksSimpleRouting(this.network); // TODO param for routing algorithm

		/*
		* loop throgh
		* - lines
		*   - routes
		* 	   - stops
		* 	     look at pairs of stops
		* 	     - get set of close links (linkCandidates) for each stop
		*          - route between all linkCandidates
		*            assign score value to routes (using traveltime and facility-link distance
		*            use route (between two stops) with the best score
		*      combine best subroutes to route (linkSequence)
		*/
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			log.info("Line "+ line.getId());
			for (TransitRoute route : line.getRoutes().values()) {
				log.info("Route: " + route.getId());
				counterLine.incCounter();

				SolvedSubRoutes solvedSubRoutes = new SolvedSubRoutes();


				int i = 0; // iterate through all stops of the route and calculate best scores
				while (i < (route.getStops().size())) {
					// look to next stop
					TransitRouteStop currentStop = route.getStops().get(i);
					TransitRouteStop nextStop = (i < route.getStops().size() - 1) ? route.getStops().get(i + 1) : null;
					TransitRouteStop previousStop = (i > 0 ? route.getStops().get(i - 1) : null);

					// check if part of current and next stop was already routed
					if(nextStop != null && !solvedSubRoutes.contains(currentStop, nextStop)) {

						SortedMap<Double, InterStopRoute> routeScores = new TreeMap<>();
						List<InterStopRoute> interStopRoutes = new ArrayList<>();

						List<Link> closestLinksCurrent = allClosestLinks.get(currentStop.getStopFacility());
						List<Link> closestLinksNext = allClosestLinks.get(nextStop.getStopFacility());

						// TODO prevent u-turns

						// look at all routes for all linkCandidate combinations
						for (Link linkCandidateCurrent : closestLinksCurrent) {
							for (Link linkCandidateNext : closestLinksNext) {
								LeastCostPathCalculator.Path pathCandidate = this.router.calcLeastCostPath(linkCandidateCurrent.getToNode(), linkCandidateNext.getFromNode(), null, null);
								interStopRoutes.add(new InterStopRoute(currentStop, nextStop, linkCandidateCurrent, linkCandidateNext, pathCandidate));
							}
						}

						// calculate score for all possible interStopRoutes
						for (InterStopRoute interStopRoute : interStopRoutes) {
							routeScores.put(interStopRoute.getScore(1,1,1), interStopRoute);
						}

						// assign best scoring interStopRoute to the set of solved stop pairs
						InterStopRoute bestInterStopRoute = routeScores.get(routeScores.firstKey());
						solvedSubRoutes.put(bestInterStopRoute);

						/*
						* check whether the linkCandidate for the current stop is different from previous routing pair
						* reroute subroute previousStop-currentStop to bestLinkCandidate for current stop
						*
						* TODO other approaches:
						* two candidates for current link: previous routing and current routing
						* if not: route from previous to next, use the link in this path.
						* if both candidates are not in optimal path? use next worse path
						 */
						if (i > 0) {
							InterStopRoute previousRoute = solvedSubRoutes.getInterStopRoute(previousStop, currentStop);
							if (!previousRoute.getToLink().equals(bestInterStopRoute.getFromLink())) {
								LeastCostPathCalculator.Path pathReroute = this.router.calcLeastCostPath(
										previousRoute.getFromLink().getFromNode(),
										bestInterStopRoute.getFromLink().getFromNode(), null, null);

								solvedSubRoutes.put(new InterStopRoute(previousStop, currentStop, previousRoute.getFromLink(), bestInterStopRoute.getFromLink(), pathReroute));
							}
						}
					}
					i++;
				}

				// combine all subroutes to one route (as a sequence of links)
				List<Id<Link>> linkSequence = new ArrayList<>();
				linkSequence.addAll(solvedSubRoutes.getLinkIdList(route.getStops()));

				if (linkSequence.size() > 0) {
					route.setRoute(RouteUtils.createNetworkRoute(linkSequence, this.network));
				} else {
					log.warn("No route found for transit route " + route.toString() + ". No route assigned.");
				}

				// reference links to stopFacilities
				for(Map.Entry<TransitStopFacility, Id<Link>> entry : solvedSubRoutes.getStopFacilityRefLinkIds().entrySet()) {
					this.schedule.getFacilities().get(entry.getKey().getId()).setLinkId(entry.getValue());
				}
			}
		}

		cleanStationsAndNetwork();
		log.info("Creating PT lines... done.");
	}


	/**
	 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
	 * and all nodes which are non-linked to any link after the above cleaning...
	 * Clean also the allowed modes for only the modes, no line-number any more...
	 */
	protected void cleanStationsAndNetwork() {
		log.info("Clean Stations and Network...");
		// TODO get rid of dummy links which are implemented via gtfsreader (not independent)
		network.removeLink(Id.createLinkId(GTFSReader.DUMMY_LINK));
		network.removeNode(Id.createNodeId(GTFSReader.DUMMY_NODE_1));
		network.removeNode(Id.createNodeId(GTFSReader.DUMMY_NODE_2));
		cleanSchedule();
		prepareNetwork();
		removeNonUsedStopFacilities();
// TODO		setConnectedStopFacilitiesToIsBlocking();
		log.info("Clean Stations and Network... done.");
	}

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
	 * Add to any link that is passed by any route a "pt" in the modes, if it hasn't already one...
	 */
	private void prepareNetwork() {
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

	private void removeNonUsedStopFacilities() {
		// Collect all used stop facilities:
		Set<Id<TransitStopFacility>> usedStopFacilities = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					usedStopFacilities.add(stop.getStopFacility().getId());
				}
			}
		}
		// Check all stop facilities if not used:
		Set<TransitStopFacility> unusedStopFacilites = new HashSet<>();
		for (Id<TransitStopFacility> facilityId : this.schedule.getFacilities().keySet()) {
			if (!usedStopFacilities.contains(facilityId)) {
				unusedStopFacilites.add(this.schedule.getFacilities().get(facilityId));
			}
		}
		// Remove all stop facilities not used:
		for (TransitStopFacility facility : unusedStopFacilites) {
			this.schedule.removeStopFacility(facility);
		}
	}


}
