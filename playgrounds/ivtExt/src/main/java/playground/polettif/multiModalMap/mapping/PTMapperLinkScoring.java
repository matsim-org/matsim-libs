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
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRFastAStarLandmarksSimpleRouting;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRouter;
import playground.polettif.multiModalMap.gtfs.GTFSReader;
import playground.polettif.multiModalMap.mapping.containter.SubRoutes;
import playground.polettif.multiModalMap.mapping.containter.InterStopPath;
import playground.polettif.multiModalMap.mapping.containter.InterStopPathSet;

import java.util.*;

/**
 * Combines routing and referencing of stopFacilities. What happens for each route:
 * <ol>
 * <li>getting linkCandidates for each routeStop</li>
 * <li>routes between linkCandidates for several/all pairs of stops</li>
 * <li>calculate linkWeights for links used by the routes (basically the more a link is used by different paths, the better its weight)</li>
 * <li>reference the most plausible link to a stopFacilities. The plausibilityScore is calculated with the link weight and the distance between link and stopfacility</li>
 * <li>routing between the now fixed stoplinks of the route</li>
 * </ol>
 * 
 * Already calculated path, route pairs and weights are stored.
 * 
 * @author polettif
 */
public class PTMapperLinkScoring extends PTMapper {

	// TODO ensure coordinate system is not WGS84 since this is not suitable for coordinate calculations

	// params TODO move to a config?
	private final static double NODE_SEARCH_RADIUS = 300; //[m] 150
	private final static int MAX_N_CLOSEST_LINKS = 6; // number of link candidates considered for all stops
	private final static int LOOKAHEAD_STOPS = 10; // number of next stops that should be included in calculations
	private final static int MAX_INITIAL_ARTIFICIAL_LINK_LENGTH = 1000; // maximal length of artificial links

	private final static String PREFIX_ARTIFICIAL_LINKS = "pt_";

	// TODO use transit modes
	private final Set<String> transitModes = Collections.singleton(TransportMode.pt);

	private int artificialId = 0;
	private Map<TransitStopFacility, List<Link>> allClosestLinks = new HashMap<>();

	/**
	 * Constructor
	 */
	public PTMapperLinkScoring(TransitSchedule schedule) {
		super(schedule);
	}

	public PTMapperLinkScoring(TransitSchedule schedule, Network network) {
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

		/** [1]
		* preload closest links
		* stopfacilities with no links within search radius need artificial links and nodes before routing starts
		*/
		List<TransitStopFacility> facilitiesTooFar = new ArrayList<>();
		NetworkImpl networkImpl = ((NetworkImpl) network); // used by search for nearest node
		for(TransitStopFacility stopFacility : this.schedule.getFacilities().values()) {
			
			// limits number of links, for all links within search radius use Tools.findClosestLinks()
			List<Link> closestLinks = Tools.findOnlyNClosestLinks(networkImpl, stopFacility.getCoord(), NODE_SEARCH_RADIUS, MAX_N_CLOSEST_LINKS);

			if (closestLinks == null) {
				// if there are links outside the map area, the very first links are routed weird (i.e. directly to the covered area)
				if(CoordUtils.calcEuclideanDistance(stopFacility.getCoord(), networkImpl.getNearestNode(stopFacility.getCoord()).getCoord()) > MAX_INITIAL_ARTIFICIAL_LINK_LENGTH) {
					facilitiesTooFar.add(stopFacility);
				}
				else {
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
				}
			} else {
				allClosestLinks.put(stopFacility, closestLinks);
			}
		}

		/**
		* create artificial links for facilities initially too far from a network node. Can now use artificial links.
		* Still not perfect, but acceptable since stopfacilities outside the map area are normally not that relevant
		 */
		networkImpl = ((NetworkImpl) network);
		for(TransitStopFacility stopFacility : facilitiesTooFar) {
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
		}

		/**
		* initiate router
		*/
		PTLRouter router = new PTLRFastAStarLandmarksSimpleRouting(this.network);

		/**
		*
		* - lines
		*   - routes
		* 	   - stops
		* 	   - calculate link weights
		* 	   - assign best scoring links to stopFacilities
		* 	   - route between assigned links
		* 	   - add link sequence to schedule
		* 	     
		*/
		SubRoutes subRoutes = new SubRoutes();

		for (TransitLine line : this.schedule.getTransitLines().values()) {
			log.info("Line "+ line.getId());
			for(TransitRoute route : line.getRoutes().values()) {

				List<TransitRouteStop> routeStops = route.getStops();

				if(route.getTransportMode().equals("bus")) {
					log.info("     Route " + route.getId());
					counterLine.incCounter();

					/** [2]
					 * iterate through all stops of the route and get paths
					 */
					for (int i = 0; i < routeStops.size(); i++) {
						TransitRouteStop currentStop = routeStops.get(i);
						
						// calculate linkWeight for between the current stop and some stops ahead (defined in LOOKAHEAD_STOPS)
						for (int j = i+1; j <= i + LOOKAHEAD_STOPS && j < routeStops.size(); j++) {
							TransitRouteStop nextStop = routeStops.get(j);

							InterStopPathSet currentInterStopPaths = new InterStopPathSet(currentStop, nextStop);

							// check if part of current and next stop was already routed
							if (!subRoutes.contains(currentStop, nextStop)) {
								List<Link> closestLinksCurrent = allClosestLinks.get(currentStop.getStopFacility());
								List<Link> closestLinksNext = allClosestLinks.get(nextStop.getStopFacility());

								// look at all routes for all linkCandidate combinations
								for (Link linkCandidateCurrent : closestLinksCurrent) {
									for (Link linkCandidateNext : closestLinksNext) {
										if (!linkCandidateCurrent.equals(linkCandidateNext)) {
											LeastCostPathCalculator.Path pathCandidate = router.calcLeastCostPath(linkCandidateCurrent.getToNode(), linkCandidateNext.getFromNode(), null, null);
											InterStopPath isp = new InterStopPath(currentStop, nextStop, linkCandidateCurrent, linkCandidateNext, pathCandidate);
											currentInterStopPaths.add(isp);
										}
									}
								}

								// store interStopRoutes
								subRoutes.add(currentInterStopPaths);
							}
						}
					}

					/** [3]
					 * get the linkWeights for all links relevant for the current sequence of route stops
					 * 
					 * Each possible route between two possible linkCandidates that passes a link adds 
					 * weight to the link. Higher weight means more paths have passed a link.
					  */
					Map<Id<Link>, Double> routeLinkWeights = subRoutes.getTransitRouteLinkWeights(routeStops);


					/** [4]
					* Assign the best scoring link* to each stop, then route between the two links.
					* 
					* score is calucated with the linkweight and the distance from the link and the stop facility
					 */
					this.schedule.getFacilities().get(routeStops.get(0).getStopFacility().getId())
							.setLinkId(getMostPlausibleLink(routeStops.get(0).getStopFacility(), routeLinkWeights).getId());
					
					List<Id<Link>> linkSequence = new ArrayList<>();
					for(int i = 0; i<routeStops.size()-1; i++) {
						TransitRouteStop currentStop = routeStops.get(i);
						TransitRouteStop nextStop = routeStops.get(i+1);

						// TODO if link reference is already set, do not calculcate it anymore 
						
						//get the most plausible links for this and the next stop 
						Link currentLink = getMostPlausibleLink(currentStop.getStopFacility(), routeLinkWeights);
						Link nextLink = getMostPlausibleLink(nextStop.getStopFacility(), routeLinkWeights);
						
						this.schedule.getFacilities().get(nextStop.getStopFacility().getId())
								.setLinkId(nextLink.getId());

						// add very first link
						if(i==0) { linkSequence.add(currentLink.getId()); }

						this.schedule.getFacilities().get(currentStop.getStopFacility().getId())
								.setLinkId(currentLink.getId());


						/** [5]
						 * route between the now referenced links
						 */
						// if stop pair was already routed, use stored path
						List<Id<Link>> path = null;
						if(subRoutes.get(currentStop, nextStop).contains(currentLink, nextLink)) {
							path = subRoutes.get(currentStop, nextStop).getPath(currentLink, nextLink).getIntermediateLinkIds();
						}
						else {
							path = InterStopPath.getLinkIdsFromPath(router.calcLeastCostPath(currentLink.getToNode(), nextLink.getFromNode(), null, null));
						}

						if(path != null)
							linkSequence.addAll(path);

						linkSequence.add(nextLink.getId());
					}

					// add link sequence to schedule
					route.setRoute(RouteUtils.createNetworkRoute(linkSequence, this.network));
				}
			} // - route loop

		} // - line loop

		cleanStationsAndNetwork();
		log.info("Creating PT lines... done.");
	}


	/**
	 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
	 * and all nodes which are non-linked to any link after the above cleaning...
	 * Clean also the allowed modes for only the modes, no line-number any more...
	 */
	private void cleanStationsAndNetwork() {
		log.info("Clean Stations and Network...");
		// TODO get rid of dummy links which are implemented via gtfsreader (not independent)
		network.removeLink(Id.createLinkId(GTFSReader.DUMMY_LINK));
		network.removeNode(Id.createNodeId(GTFSReader.DUMMY_NODE_1));
		network.removeNode(Id.createNodeId(GTFSReader.DUMMY_NODE_2));

// TODO clean network
		cleanSchedule();
//		prepareNetwork();
		removeNonUsedStopFacilities();
//		setConnectedStopFacilitiesToIsBlocking();

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

	/**
	 * calculates the plausibility score for all closest links of a stopFacility
	 *
	 * score = d + w
	 *
	 * d = distance stopFacility-Link (scaled 0..1)
	 * w = link weight (scaled 0..1, only weights of linkCandidates are used to normalize)
	 *
	 * @param stopFacility used to calculate the distance to the links
	 * @return map with plausibilityScores
	 */
	private SortedMap<Double, Link> getLinkPlausibilityScores(TransitStopFacility stopFacility, Map<Id<Link>, Double> routeLinkWeights) {
		Map<Id<Link>, Double> distances = new HashMap<>();
		SortedMap<Double, Link> rankedLinks = new TreeMap<>();
		List<Link> links = allClosestLinks.get(stopFacility);
		Map<Id<Link>, Double> routeLinkWeightsSubset = new HashMap<>();

		double minDist = Double.MAX_VALUE;

		// calculate all distances stopFacility-Link (get linkweight subset on the way)
		for(Link ll : links) {
			double distance = CoordUtils.distancePointLinesegment(ll.getFromNode().getCoord(), ll.getToNode().getCoord(), stopFacility.getCoord());
			if(distance < minDist)
				minDist = distance;

			distances.put(ll.getId(), distance);

			routeLinkWeightsSubset.put(ll.getId(), routeLinkWeights.get(ll.getId()));
		}

		// scale linkweights
		routeLinkWeightsSubset = normalize(routeLinkWeightsSubset);

		// scale distances
		distances = normalizeInvert(distances); // .put(ll.getId(), 2-distances.get(ll.getId())/minDist);

		for(Link ll : links) {
			double plausibilityScore = distances.get(ll.getId()) + (routeLinkWeightsSubset.get(ll.getId()));
			rankedLinks.put(plausibilityScore, ll);
		}

		return rankedLinks;
	}

	/**
	 * @return the most plausible linkId calculated by {@link #getLinkPlausibilityScores}
	 */
	public Link getMostPlausibleLink(TransitStopFacility stopFacility, Map<Id<Link>, Double> routeLinkWeights) {
		SortedMap<Double, Link> rankedLinks = getLinkPlausibilityScores(stopFacility, routeLinkWeights);

		return rankedLinks.get(rankedLinks.lastKey());
	}

	/**
	 * normalizes the values of a map via value/maxValue
	 * @return the normalized map
	 */
	public static Map<Id<Link>, Double> normalize(Map<Id<Link>, Double> map) {
		// get maximal weight
		double maxValue = 0;
		for(Double v : map.values()) {
			if(v > maxValue)
				maxValue = v;
		}

		// scale weights
		for(Map.Entry<Id<Link>, Double> e : map.entrySet()) {
			map.put(e.getKey(), map.get(e.getKey())/maxValue);
		}
	return map;
	}

	/**
	 * normalizes the values of a map via 1-value/maxValue
	 * @return the normalized map
	 */
	public static Map<Id<Link>, Double> normalizeInvert(Map<Id<Link>, Double> map) {
		double maxValue = 0;
		for(Double v : map.values()) {
			if(v > maxValue)
				maxValue = v;
		}

		for(Map.Entry<Id<Link>, Double> e : map.entrySet()) {
			map.put(e.getKey(), 1-map.get(e.getKey())/maxValue);
		}

		return map;
	}


}
