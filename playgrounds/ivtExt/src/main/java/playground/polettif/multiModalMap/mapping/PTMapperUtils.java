/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package playground.polettif.multiModalMap.mapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.FastAStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.container.PTPath;
import playground.polettif.multiModalMap.mapping.container.PTPathImpl;
import playground.polettif.multiModalMap.mapping.router.Router;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.*;
import java.util.stream.Collectors;


public class PTMapperUtils {

	/**
	 * ID prefix used for artificial link created if no nodes are found within NODE_SEARCH_RADIUS
	 */
	private final static String PREFIX_ARTIFICIAL_LINKS = "pt_";

	/**
	 * Suffix used for child stop facilities. A number for each child of a parent stop facility is appended (i.e. stop0123_fac:2)
	 */
	private final static String SUFFIX_CHILD_STOPFACILITIES = "_fac:";

	protected static Logger log = Logger.getLogger(PTMapperUtils.class);

	public static PTPath createPTPath(TransitStopFacility fromStopFacility, Link fromLink, TransitStopFacility toStopFacility, Link toLink, LeastCostPathCalculator.Path path) {
		return new PTPathImpl(fromStopFacility, fromLink, toStopFacility, toLink, path);
	}

	/**
	 * Calculates the plausibility score for all closest links of a stopFacility
	 * <br/>
	 * score = LINK_FACILITY_DISTANCE_WEIGHT * d + w
	 * <br/>
	 * d = distance stopFacility-Link (scaled 0..1)<br/>
	 * w = link weight (scaled 0..1)
	 *
	 * @param stopFacility used to calculate the distance to the links
	 * @param linkWeights  the link weights for the current route
	 * @return Map with plausibilityScores
	 */
	private static SortedMap<Double, Link> getLinkPlausibilityScores(TransitStopFacility stopFacility, List<Link> linkCandidates, Map<Id<Link>, Double> linkWeights, double linkFacilityDistanceWeight) {
		Map<Id<Link>, Double> distances = new HashMap<>();
		SortedMap<Double, Link> rankedLinks = new TreeMap<>();
		Map<Id<Link>, Double> routeLinkWeightsSubset = new HashMap<>();

		double minDist = Double.MAX_VALUE;

		// calculate all distances stopFacility-Link (get linkweight subset on the way)
		for(Link ll : linkCandidates) {
			double distance = CoordUtils.distancePointLinesegment(ll.getFromNode().getCoord(), ll.getToNode().getCoord(), stopFacility.getCoord());

			distances.put(ll.getId(), distance);

			double weight;
			if(linkWeights.containsKey(ll.getId())) {
				weight = linkWeights.get(ll.getId());
			} else {
				weight = 0;
			}

			routeLinkWeightsSubset.put(ll.getId(), weight);
		}

		// scale linkweights
		routeLinkWeightsSubset = normalize(routeLinkWeightsSubset);

		// scale distances
		distances = normalizeInvert(distances);

		for(Link ll : linkCandidates) {
			double plausibilityScore = linkFacilityDistanceWeight * distances.get(ll.getId()) + (routeLinkWeightsSubset.get(ll.getId()));


			rankedLinks.put(plausibilityScore, ll);
		}

		return rankedLinks;
	}

	/**
	 * @return the most plausible linkId calculated by {@link #getLinkPlausibilityScores}
	 */
	public static Link getMostPlausibleLink(TransitStopFacility stopFacility, List<Link> linkCandidates, Map<Id<Link>, Double> linkWeights, double linkFacilityDistanceWeight) {
		SortedMap<Double, Link> rankedLinks = PTMapperUtils.getLinkPlausibilityScores(stopFacility, linkCandidates, linkWeights, linkFacilityDistanceWeight);

		if(rankedLinks.size() == 0) {
			log.error("no plausible links found");
		}

		return rankedLinks.get(rankedLinks.lastKey());
	}

	/**
	 * normalizes the values of a map via value/maxValue
	 *
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
			map.put(e.getKey(), map.get(e.getKey()) / maxValue);
		}
		return map;
	}

	/**
	 * Normalizes the values of a map via 1-value/maxValue
	 *
	 * @return the normalized map
	 */
	public static Map<Id<Link>, Double> normalizeInvert(Map<Id<Link>, Double> map) {
		double maxValue = 0;
		for(Double v : map.values()) {
			if(v > maxValue)
				maxValue = v;
		}

		for(Map.Entry<Id<Link>, Double> e : map.entrySet()) {
			map.put(e.getKey(), 1 - map.get(e.getKey()) / maxValue);
		}

		return map;
	}


	/**
	 * removes all stop facilities not used by a transit route. Modifies the schedule
	 *
	 * @param schedule the schedule in which the facilities should be removed
	 */
	public static void removeNonUsedStopFacilities(TransitSchedule schedule) {
		// Collect all used stop facilities:
		Set<Id<TransitStopFacility>> usedStopFacilities = new HashSet<>();
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				for(TransitRouteStop stop : route.getStops()) {
					usedStopFacilities.add(stop.getStopFacility().getId());
				}
			}
		}
		// Check all stop facilities if not used:
		Set<TransitStopFacility> unusedStopFacilites = new HashSet<>();
		for(Id<TransitStopFacility> facilityId : schedule.getFacilities().keySet()) {
			if(!usedStopFacilities.contains(facilityId)) {
				unusedStopFacilites.add(schedule.getFacilities().get(facilityId));
			}
		}
		// Remove all stop facilities not used:
		for(TransitStopFacility facility : unusedStopFacilites) {
			schedule.removeStopFacility(facility);
		}
	}

	public static List<Id<Link>> getLinkIdsFromPath(LeastCostPathCalculator.Path path) {
		return path.links.stream().map(Link::getId).collect(Collectors.toList());
	}


	public static boolean linkSequenceHasLoops(List<Link> links) {
		Set tmpSet = new HashSet<>(links);
		return tmpSet.size() < links.size();
	}

	public static boolean linkSequenceHasUTurns(List<Link> links) {
		for(int i = 1; i < links.size(); i++) {
			if(links.get(i).getToNode().equals(links.get(i - 1).getFromNode())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * cleans the schedule by removing routes without link sequences
	 * @param schedule
	 */
	public static void cleanSchedule(TransitSchedule schedule) {
		for (TransitLine line : schedule.getTransitLines().values()) {
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
	 * Sets the isBlocking value of every stop facility that is referenced to a car link to true.
	 */
	@Deprecated
	public static void setConnectedStopFacilitiesToIsBlocking(TransitSchedule schedule, Network network) {
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		Set<TransitStopFacility> facilitiesToExchange = new HashSet<>();
		for (TransitStopFacility oldFacility : schedule.getFacilities().values()) {
			if (network.getLinks().get(oldFacility.getLinkId()).getAllowedModes().contains(TransportMode.car)) {
				TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
						oldFacility.getId(), oldFacility.getCoord(), true);
				newFacility.setName(oldFacility.getName());
				newFacility.setLinkId(oldFacility.getLinkId());
				newFacility.setStopPostAreaId(oldFacility.getStopPostAreaId());
				facilitiesToExchange.add(newFacility);
			}
		}
		for (TransitStopFacility facility : facilitiesToExchange) {
			TransitStopFacility facilityToRemove = schedule.getFacilities().get(facility.getId());
			schedule.removeStopFacility(facilityToRemove);
			schedule.addStopFacility(facility);
		}
	}


	/**
	 * Add mode "pt" to any link of the network that is passed by any route.
	 */
	public static void addPTModeToNetwork(TransitSchedule schedule, Network network) {
		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinks = new HashSet<>();
		for (TransitLine line : schedule.getTransitLines().values()) {
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
	 * Generates link sequences for all transit routes in the schedule, modifies the schedule.
	 * StopFacilities must have a reference link.
	 *
	 * @param schedule where transitRoutes should be routed
	 */
	public static void routeSchedule(TransitSchedule schedule, Network network, Router router) {
		Counter counterRoute = new Counter("route # ");

		log.info("Routing all routes with referenced links...");
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				counterRoute.incCounter();

				// TODO change modes
				if(route.getTransportMode().equals("bus")) {

					List<TransitRouteStop> routeStops = route.getStops();
					List<Id<Link>> linkSequence = new ArrayList<>();

					// add very first link
					linkSequence.add(routeStops.get(0).getStopFacility().getLinkId());

					// route
					for(int i = 0; i < routeStops.size()-1; i++) {
						if(routeStops.get(i).getStopFacility().getLinkId() == null) {
							log.error("stop facility " + routeStops.get(i).getStopFacility().getName() + " (" + routeStops.get(i).getStopFacility().getId() + " not referenced!");
						}
						if(routeStops.get(i+1).getStopFacility().getLinkId() == null) {
							log.error("stop facility " + routeStops.get(i-1).getStopFacility().getName() + " (" + routeStops.get(i+1).getStopFacility().getId() + " not referenced!");
						}

						Link currentLink = network.getLinks().get(routeStops.get(i).getStopFacility().getLinkId());
						Link nextLink = network.getLinks().get(routeStops.get(i+1).getStopFacility().getLinkId());

						List<Id<Link>> path = PTMapperUtils.getLinkIdsFromPath(router.calcLeastCostPath(currentLink.getToNode(), nextLink.getFromNode(), null, null));

						if(path != null)
							linkSequence.addAll(path);

						linkSequence.add(nextLink.getId());
					}

					// add link sequence to schedule
					route.setRoute(RouteUtils.createNetworkRoute(linkSequence, network));
				}
			}
		}
		log.info("Routing all routes with referenced links... done");
	}

}
