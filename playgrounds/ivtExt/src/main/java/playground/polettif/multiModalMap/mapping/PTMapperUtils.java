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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.container.PTPath;
import playground.polettif.multiModalMap.mapping.container.PTPathImpl;

import java.util.*;
import java.util.stream.Collectors;


public class PTMapperUtils {

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

	public void replaceStopFacilities() {
/*
		for(Id<TransitLine> lineId:stopFacilitiesToReplace.getLineIds())			{
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
		for(Tuple<TransitLine, TransitRoute> entry : newTransitLines)

		{
			this.schedule.getTransitLines().get(entry.getFirst().getId()).addRoute(entry.getSecond());
		}
		*/
	}

}
