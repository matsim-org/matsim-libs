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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.PseudoRouteStop;
import playground.polettif.multiModalMap.mapping.router.Router;
import playground.polettif.multiModalMap.tools.MiscUtils;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides some static tools for PTMapper.
 *
 * @author polettif
 */
public class PTMapperUtils {

	// TODO cleanup and doc

	protected static Logger log = Logger.getLogger(PTMapperUtils.class);
	/**
	 * Generates link candidates for all stopFacilities. For stop facilities where
	 * no link can be found within nodeSearchRadius an artificial node and loop
	 * link (from and to the new node) is created {@link NetworkTools#createArtificialStopFacilityLink(TransitStopFacility, Network, String)}.
	 * For each link candiate a child stop facility is generated and referenced to
	 * the link. Link candidates for different modes with the same link use the same
	 * child stop facility. Child stop facilities are not created and added to the schedule!
	 * If a stop facility already has a referenced link, this link is used as the only link
	 * candidate.<p/>
	 *
	 * @param schedule with stopFacilities, not modified.
	 * @param network the network where link candidates should be looked for, is modified
	 *                for stop facilities without a link nearby
	 * @param config containing the modeRoutingAssignments and params for link searching
	 * @return a map with all link candidates for a stop facilitiy and
	 * the scheduleTransportMode as top level key.
	 */
	public static Map<String, Map<TransitStopFacility, Set<LinkCandidate>>> generateModeLinkCandidates(TransitSchedule schedule, Network network, PublicTransportMapConfigGroup config) {
		Map<String, Map<TransitStopFacility, Set<LinkCandidate>>> tree = new HashMap<>();

		NetworkImpl networkImpl = ((NetworkImpl) network);

		/**
		 * get closest links for each stop facility (separated by mode)
		 */
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					String scheduleTransportMode = transitRoute.getTransportMode();
					TransitStopFacility stopFacility = transitRouteStop.getStopFacility();

					Set<LinkCandidate> modeLinkCandidates = MapUtils.getSet(stopFacility, MapUtils.getMap(scheduleTransportMode, tree));

					// if no link candidates for the current stop and mode have been generated
					if(modeLinkCandidates.size() == 0) {

						// if stop facilty already has a referenced link
						if(stopFacility.getLinkId() != null) {
							modeLinkCandidates.add(new LinkCandidate(network.getLinks().get(stopFacility.getLinkId()), stopFacility));
						} else {
							// limits number of links, for all links within search radius use networkTools.findClosestLinks()
							Set<Link> closestLinks = NetworkTools.findClosestLinksByMode(networkImpl, stopFacility.getCoord(), scheduleTransportMode, config);

							// if no close links are nearby, a loop link is created and referenced to the facility.
							if(closestLinks.size() == 0) {
								Link loopLink = NetworkTools.createArtificialStopFacilityLink(stopFacility, network, config.getPrefixArtificial());
								closestLinks.add(loopLink);
							}

							/**
							 * generate a LinkCandidate for each close link
							 */
							for(Link link : closestLinks) {
								modeLinkCandidates.add(new LinkCandidate(link, stopFacility));
							}
						}
					}
				}
			}
		}
		return tree;
	}

	/**
	 * Generates link candidates for all stopFacilities. For stop facilities where
	 * which no link can be found within nodeSearchRadius an artificial node and loop
	 * link (from and to the new node) are created {@link NetworkTools#createArtificialStopFacilityLink(TransitStopFacility, Network, String)}.
	 * For each link candiate a child stop facility is generated and referenced to
	 * the link. Link candidates for different modes with the same link use the same
	 * child stop facility. Child stop facilities are added to the schedule.
	 *
	 * @param schedule with stopFacilities, is modified.
	 * @param network  where link candidates should be mapped, is modified.
	 * @return the LinkCandidates for each stopFacility, split by modes.
	 */
	public static Map<TransitStopFacility, Map<String, Set<LinkCandidate>>> generateModeSeparatedLinkCandidates(TransitSchedule schedule, Network network, PublicTransportMapConfigGroup config) {

		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		Map<TransitStopFacility, Map<String, Set<LinkCandidate>>> tree = new HashMap<>();
		Map<String, TransitStopFacility> childFacilities = new HashMap<>();

		NetworkImpl networkImpl = ((NetworkImpl) network);

		/**
		 * get closest links for each stop facility (separated by mode)
		 */
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					String scheduleTransportMode = transitRoute.getTransportMode();
					TransitStopFacility stopFacility = transitRouteStop.getStopFacility();

					Map<String, Set<LinkCandidate>> parentFacilityLinkCandidates = MapUtils.getMap(stopFacility, tree);
					Set<LinkCandidate> modeLinkCandidates = MapUtils.getSet(scheduleTransportMode, parentFacilityLinkCandidates);

					// if no link candidates for the current stop and mode have been generated
					if(modeLinkCandidates.size() == 0) {
						// limits number of links, for all links within search radius use networkTools.findClosestLinks()
						Set<Link> closestLinks = NetworkTools.findClosestLinksByMode(networkImpl, stopFacility.getCoord(), scheduleTransportMode, config);

						// if no close links are nearby, a loop link is created and referenced to the facility.
						if(closestLinks.size() == 0) {
							Link loopLink = NetworkTools.createArtificialStopFacilityLink(stopFacility, network, config.getPrefixArtificial());
							loopLink.setLength(10);
							stopFacility.setLinkId(loopLink.getId());
							closestLinks.add(loopLink);
						}

						/**
						 * generate child stop facility for each linkcandidate and reference them
						 */
						for(Link link : closestLinks) {
							LinkCandidate newLinkCandidate = new LinkCandidate(link, stopFacility);

							String id = stopFacility.getId() + config.getSuffixChildStopFacilities() + link.getId();

							// if child stop for this link has already been generated
							if(childFacilities.containsKey(id)) {
								newLinkCandidate.setChildStop(childFacilities.get(id));
							} else {
								TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
										Id.create(id, TransitStopFacility.class),
										stopFacility.getCoord(),
										stopFacility.getIsBlockingLane()
								);
								newFacility.setLinkId(link.getId());
								newFacility.setName(stopFacility.getName());
								newFacility.setStopPostAreaId(stopFacility.getStopPostAreaId());
								childFacilities.put(id, newFacility);

								newLinkCandidate.setChildStop(newFacility);
							}

							MapUtils.getSet(scheduleTransportMode, MapUtils.getMap(stopFacility, tree)).add(newLinkCandidate);
						}

					}
				}
			}
		}

		/**
		 * assign new facilities to schedule
		 */
		childFacilities.values().forEach(schedule::addStopFacility);

		return tree;
	}

	/**
	 * Using the pseudo schedule, the facilities in the stop sequences of the actual schedule
	 * are replaced with child facilities. Child facilities are created in this step and
	 * added to the schedule.
	 * @param schedule where the facilities should be replaced
	 * @param pseudoSchedule defines the actual sequence of pseudoRouteStops
	 * @param childStopFacilitySuffix what suffix the child facility should get in the id
	 */
	public static void createAndReplaceFacilities(TransitSchedule schedule, Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoSchedule, String childStopFacilitySuffix) {
		log.info("Replacing parent StopFacilities with child StopFacilities...");
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		List<Tuple<TransitLine, TransitRoute>> newRoutes = new ArrayList<>();

		for(Map.Entry<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> lineEntry : pseudoSchedule.entrySet()) {
			for(Map.Entry<TransitRoute, List<PseudoRouteStop>> routeEntry : lineEntry.getValue().entrySet()) {

				List<PseudoRouteStop> pseudoStopSequence = routeEntry.getValue();
				List<TransitRouteStop> newStopSequence = new ArrayList<>();

				for(PseudoRouteStop pseudoStop : pseudoStopSequence) {
					Id<TransitStopFacility> childStopFacilityId = Id.create(pseudoStop.getParentStopFacilityId() + childStopFacilitySuffix + pseudoStop.getLinkIdStr(), TransitStopFacility.class);

					// if child stop facility for this link has not yet been generated
					if(!schedule.getFacilities().containsKey(childStopFacilityId)) {
						TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
								Id.create(childStopFacilityId, TransitStopFacility.class),
								pseudoStop.getCoord(),
								pseudoStop.getIsBlockingLane()
						);
						newFacility.setLinkId(Id.createLinkId(pseudoStop.getLinkIdStr()));
						newFacility.setName(pseudoStop.getFacilityName());
						newFacility.setStopPostAreaId(pseudoStop.getStopPostAreaId());
						schedule.addStopFacility(newFacility);
					}

					// create new TransitRouteStop and add it to the newStopSequence
					TransitRouteStop newTransitRouteStop = scheduleFactory.createTransitRouteStop(
							schedule.getFacilities().get(childStopFacilityId),
							pseudoStop.getArrivalOffset(),
							pseudoStop.getDepartureOffset());
					newTransitRouteStop.setAwaitDepartureTime(pseudoStop.isAwaitDepartureTime());
					newStopSequence.add(newTransitRouteStop);
				}

				// create a new transitRoute
				TransitRoute newRoute = scheduleFactory.createTransitRoute(routeEntry.getKey().getId(), null, newStopSequence, routeEntry.getKey().getTransportMode());

				// add departures
				routeEntry.getKey().getDepartures().values().forEach(newRoute::addDeparture);

				// remove the old route
				schedule.getTransitLines().get(lineEntry.getKey().getId()).removeRoute(routeEntry.getKey());

				// add new route to container
				newRoutes.add(new Tuple<>(lineEntry.getKey(), newRoute));
			}
		}

		// add transit lines and routes again
		for(Tuple<TransitLine, TransitRoute> entry : newRoutes) {
			schedule.getTransitLines().get(entry.getFirst().getId()).addRoute(entry.getSecond());
		}
	}


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
				if(lcCurrent.getLinkIdStr().equals(lcNext.getLinkIdStr())) {
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
}