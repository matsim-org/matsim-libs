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

package playground.polettif.publicTransitMapping.mapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingStrings;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;
import playground.polettif.publicTransitMapping.tools.CoordTools;
import playground.polettif.publicTransitMapping.tools.MiscUtils;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides some static tools for PTMapper.
 *
 * @author polettif
 */
public class PTMapperUtils {

	protected static Logger log = Logger.getLogger(PTMapperUtils.class);
	private static final String suffixChildStopFacilities = PublicTransitMappingStrings.SUFFIX_CHILD_STOP_FACILITIES;
	private static final String suffixChildStopFacilitiesRegex = PublicTransitMappingStrings.SUFFIX_CHILD_STOP_FACILITIES_REGEX;

	/**
	 * @return the linkIds of the links in path
	 */
	public static List<Id<Link>> getLinkIdsFromPath(LeastCostPathCalculator.Path path) {
//		List<Id<Link>> list = new ArrayList<>();
//		for(Link link : path.links) {
//			list.add(link.getId());
//		}
		return path.links.stream().map(Link::getId).collect(Collectors.toList());
	}

	/**
	 * If link candidates have the same link for boths stop facilities, the link candidate is
	 * assigned to the stop facility that is closer (i.e. removed from the other set).
	 */
	public static void separateLinkCandidates(Set<LinkCandidate> linkCandidatesCurrent, Set<LinkCandidate> linkCandidatesNext) {
		Set<LinkCandidate> removeFromCurrent = new HashSet<>();
		Set<LinkCandidate> removeFromNext = new HashSet<>();

		for(LinkCandidate lcCurrent : linkCandidatesCurrent) {
			for(LinkCandidate lcNext : linkCandidatesNext) {
				if(lcCurrent.getLinkId().equals(lcNext.getLinkId())) {
					if(lcCurrent.getStopFacilityDistance() > lcNext.getStopFacilityDistance()) {
						removeFromCurrent.add(lcCurrent);
					} else {
						removeFromNext.add(lcNext);
					}
				}
			}
		}
		removeFromCurrent.forEach(linkCandidatesCurrent::remove);
		removeFromNext.forEach(linkCandidatesNext::remove);
	}


	/**
	 * Checks for each child stop facility if the link before or after is closer to the facility
	 * than its referenced link. If so, the child stop facility is replaced with the one closer
	 * to the facility coordinates. Transit routes with loop route profiles (i.e. a stop is accessed
	 * twice in a stop sequence) are ignored.
	 * @return the number of child stop facilities pulled
	 */
	public static int pullChildStopFacilitiesTogether(TransitSchedule schedule, Network network) {
		int nPulled = 0;
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				boolean hasStopLoop = ScheduleTools.routeHasStopSequenceLoop(transitRoute);
				if(transitRoute.getRoute() != null) {
					TransitRouteStop currentStop;
					List<TransitRouteStop> routeStops = transitRoute.getStops();

					Iterator<TransitRouteStop> stopsIterator = routeStops.iterator();

					List<Id<Link>> linkIdList = ScheduleTools.getTransitRouteLinkIds(transitRoute);
					List<Link> linkList = NetworkTools.getLinksFromIds(network, linkIdList);

					currentStop = stopsIterator.next();
					// look for a closer link before the route's start
					if(!hasStopLoop) {
						Set<Link> inlinksWithSameMode = NetworkTools.filterLinkSetExactlyByModes(linkList.get(0).getFromNode().getInLinks().values(), linkList.get(0).getAllowedModes());
						Id<Link> closerLinkBefore = useCloserRefLinkForChildStopFacility(schedule, network, transitRoute, currentStop.getStopFacility(), inlinksWithSameMode);
						if(closerLinkBefore != null) {
							linkIdList.add(0, closerLinkBefore);
							nPulled++;
						}
					}
						currentStop = stopsIterator.next();

					// optimize referenced links between start and end
					for(int i = 1; i < linkList.size()-1; i++) {

						if(linkList.get(i).toString().equals("114813")) {
							log.debug("");
						}

						if(linkList.get(i).getId().equals(currentStop.getStopFacility().getLinkId())) {
							Set<Link> testSet = new HashSet<>();
							testSet.add(linkList.get(i));
							testSet.add(linkList.get(i-1));
							testSet.add(linkList.get(i+1));
							Id<Link> check = useCloserRefLinkForChildStopFacility(schedule, network, transitRoute, currentStop.getStopFacility(), testSet);

							if(check != null) nPulled++;

							if(stopsIterator.hasNext()) {
								currentStop = stopsIterator.next();
							}
						}
					}

					// look for a closer link after the route's end
					if(!hasStopLoop) {
						currentStop = routeStops.get(routeStops.size() - 1);
						Set<Link> outlinksWithSameMode = NetworkTools.filterLinkSetExactlyByModes(linkList.get(linkList.size() - 1).getToNode().getOutLinks().values(), linkList.get(linkList.size() - 1).getAllowedModes());
						Id<Link> closerLinkAfter = useCloserRefLinkForChildStopFacility(schedule, network, transitRoute, currentStop.getStopFacility(), outlinksWithSameMode);
						if(closerLinkAfter != null) {
							linkIdList.add(closerLinkAfter);
							nPulled++;
						}
					}

					// set the new link list
					transitRoute.setRoute(RouteUtils.createNetworkRoute(linkIdList, network));
				}
			}
		}
		return nPulled;
	}


	/**
	 * If a link of <tt>comparingLinks</tt> is closer to the stop facility than
	 * its currently referenced link, the closest link is used.
	 * @return The id of the new closest link or <tt>null</tt> if the existing ref link
	 * was used.
	 */
	private static Id<Link> useCloserRefLinkForChildStopFacility(TransitSchedule schedule, Network network, TransitRoute transitRoute, TransitStopFacility stopFacility, Collection<? extends Link> comparingLinks) {
		// check if previous link is closer to stop facility
		double minDist = CoordTools.distanceStopFacilityToLink(stopFacility, network.getLinks().get(stopFacility.getLinkId()));
		Link minLink = null;

		for(Link comparingLink : comparingLinks) {
			double distCompare = CoordTools.distanceStopFacilityToLink(stopFacility, comparingLink);
			if(distCompare < minDist) {
				minDist = distCompare;
				minLink = comparingLink;
			}
		}

		if(minLink != null) {
			TransitStopFacility newChildStopFacility;
			String[] split = stopFacility.getId().toString().split(suffixChildStopFacilitiesRegex);
			Id<TransitStopFacility> newChildStopFacilityId = Id.create(split[0] + suffixChildStopFacilities + minLink.getId(), TransitStopFacility.class);
			if(schedule.getFacilities().containsKey(newChildStopFacilityId)) {
				newChildStopFacility = schedule.getFacilities().get(newChildStopFacilityId);
			} else {
				newChildStopFacility = schedule.getFactory().createTransitStopFacility(newChildStopFacilityId, stopFacility.getCoord(), false);
				newChildStopFacility.setName(stopFacility.getName());
				newChildStopFacility.setStopPostAreaId(stopFacility.getStopPostAreaId());
				newChildStopFacility.setLinkId(minLink.getId());
				schedule.addStopFacility(newChildStopFacility);
			}
			transitRoute.getStop(stopFacility).setStopFacility(newChildStopFacility);
			return minLink.getId();
		} else {
			return null;
		}
	}

	/**
	 * Changes the free speed of links based on the necessary travel times
	 * given by the schedule. Rather experimental and only recommended for
	 * artificial and possibly rail links.
	 */
	public static void setFreeSpeedBasedOnSchedule(Network network, TransitSchedule schedule, Set<String> networkModes) {
		Map<Id<Link>, Double> necessaryMinSpeeds = new HashMap<>();

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Id<Link>> linkIds = ScheduleTools.getTransitRouteLinkIds(transitRoute);

				Iterator<TransitRouteStop> stopsIterator = transitRoute.getStops().iterator();
				List<Link> links = NetworkTools.getLinksFromIds(network, linkIds);

				List<Id<Link>> linkIdsUpToCurrentStop = new ArrayList<>();
				TransitRouteStop previousStop = stopsIterator.next();
				TransitRouteStop nextStop = stopsIterator.next();
				double lengthUpToCurrentStop = 0;
				double departTime = previousStop.getDepartureOffset();

				for(int i = 0; i < links.size() - 2; i++) {
					Link linkFrom = links.get(i);
					Link linkTo = links.get(i + 1);

					linkIdsUpToCurrentStop.add(linkFrom.getId());

					// get schedule travel time and necessary freespeed
					lengthUpToCurrentStop += linkFrom.getLength();
					if(nextStop.getStopFacility().getLinkId().equals(linkTo.getId())) {
						double ttSchedule = nextStop.getArrivalOffset() - departTime;
						double theoreticalMinSpeed = (lengthUpToCurrentStop / ttSchedule) * 1.02;

						for(Id<Link> linkId : linkIdsUpToCurrentStop) {
							double setMinSpeed = MapUtils.getDouble(linkId, necessaryMinSpeeds, 0);
							if(theoreticalMinSpeed > setMinSpeed) {
								necessaryMinSpeeds.put(linkId, theoreticalMinSpeed);
							}
						}

						// reset
						lengthUpToCurrentStop = 0;
						linkIdsUpToCurrentStop = new ArrayList<>();
						previousStop = nextStop;
						departTime = previousStop.getDepartureOffset();
						if(!nextStop.equals(transitRoute.getStops().get(transitRoute.getStops().size() - 1))) {
							nextStop = stopsIterator.next();
						}
					}
				}
			}
		}

		for(Link link : network.getLinks().values()) {
			if(MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), networkModes)) {
				if(necessaryMinSpeeds.containsKey(link.getId())) {
					double necessaryMinSpeed = necessaryMinSpeeds.get(link.getId());
					if(necessaryMinSpeed > link.getFreespeed()) {
						link.setFreespeed(Math.ceil(necessaryMinSpeed));
					}
				}
			}
		}
	}
}