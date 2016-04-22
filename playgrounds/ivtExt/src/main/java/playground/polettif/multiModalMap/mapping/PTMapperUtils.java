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
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.PseudoRouteStop;
import playground.polettif.multiModalMap.mapping.router.Router;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides some static tools for PTMapper.
 *
 * @author polettif
 */
public class PTMapperUtils {

	protected static Logger log = Logger.getLogger(PTMapperUtils.class);

	/**
	 * Generates link candidates for all stopFacilities. For stop facilities where which no link can
	 * be found within nodeSearchRadius an ARTIFICIAL_LINK_MODE node and two ARTIFICIAL_LINK_MODE links (in & out) are
	 * created and added to the network. For each link candiate a child stop facility is generated
	 * and referenced to the link. Child stop facilities are added to the schedule.
	 *
	 * @param schedule         with stopFacilities, is modified.
	 * @param network          where link candidates should be mapped, is modified.
	 * @param nodeSearchRadius only links within this radius from the stop facility are considered for closest links
	 *                         calculations.
	 * @param maxNclosestLinks the maximum number of closest links (and thus link candidates) should be used.
	 *                         Note: if two links have the same distance to the stop facility both are used
	 * @return the LinkCandidates for each stopFacility. Note: <code>List&lt;LinkCandidate&gt;</code> is <code>null</code> if no
	 * links are close to the stopFacility.
	 */
	public static Map<TransitStopFacility, List<LinkCandidate>> generateLinkCandidates(TransitSchedule schedule, Network network, double nodeSearchRadius, int maxNclosestLinks, double maxLinkFacilityDistance) {

		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		Map<TransitStopFacility, List<LinkCandidate>> linkCandidates = new HashMap<>();
		List<TransitStopFacility> childFacilities = new ArrayList<>();

		NetworkImpl networkImpl = ((NetworkImpl) network);

		/**
		 * get closest links for each stop facility (separated by mode)
		 */
		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			// limits number of links, for all links within search radius use networkTools.findClosestLinks()
			List<Link> closestLinks = NetworkTools.findNClosestLinks(networkImpl, stopFacility.getCoord(), nodeSearchRadius, maxNclosestLinks, maxLinkFacilityDistance);

			// if no close links are nearby, a loop link is created and referenced to the facility.
			if(closestLinks.size() == 0) {
				Link loopLink = NetworkTools.createArtificialStopFacilityLink(stopFacility, network);
				loopLink.setLength(10);
				stopFacility.setLinkId(loopLink.getId());
				closestLinks.add(loopLink);
			}

			/**
			 * generate child stop facility for each linkcandidate and reference them
			 */
			for(Link link : closestLinks) {
				LinkCandidate newLC = new LinkCandidate(link, stopFacility);
				MapUtils.getList(stopFacility, linkCandidates).add(newLC);

				String id = stopFacility.getId() + ".link:" + link.getId();

				TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
						Id.create(id, TransitStopFacility.class),
						stopFacility.getCoord(),
						stopFacility.getIsBlockingLane()
				);
				newFacility.setLinkId(link.getId());
				newFacility.setName(stopFacility.getName());
				newFacility.setStopPostAreaId(stopFacility.getStopPostAreaId());
				childFacilities.add(newFacility);

				newLC.setChildStop(newFacility);
			}
		}
		/**
		 * assign new facilities to schedule
		 */
		childFacilities.forEach(schedule::addStopFacility);

		return linkCandidates;
	}

	/**
	 * Generates link candidates for all stopFacilities. For stop facilities where
	 * which no link can be found within nodeSearchRadius an artificial node and loop
	 * link (from and to the new node) are created {@link NetworkTools#createArtificialStopFacilityLink(TransitStopFacility, Network)}.
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
						Set<Link> closestLinks = NetworkTools.findNClosestLinksByMode(networkImpl, stopFacility.getCoord(), scheduleTransportMode, config);

						// if no close links are nearby, a loop link is created and referenced to the facility.
						if(closestLinks.size() == 0) {
							Link loopLink = NetworkTools.createArtificialStopFacilityLink(stopFacility, network);
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
	 * Modifies the schedule. Replaces the parent StopFacilities
	 * with the child StopFacilities. The replacement pairs are
	 * given by pseudoTransitRoutes and the PseudoRouteStop sequence
	 * especially
	 */
	public static void replaceFacilities(TransitSchedule schedule, Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoRoutes) {

		TransitScheduleFactory scheduleFactory = schedule.getFactory();
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
	 * Removes all stop facilities not used by a transit route. Modifies the schedule.
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
	 * Checks if a link sequence has loops (i.e. the same link is passed twice).
	 *
	 * @param links
	 */
	public static boolean linkSequenceHasLoops(List<Link> links) {
		Set tmpSet = new HashSet<>(links);
		return tmpSet.size() < links.size();
	}


	/**
	 * Checks if a link sequence has u-turns (i.e. the opposite direction link is
	 * passed immediately after a link).
	 */
	public static boolean linkSequenceHasUTurns(List<Link> links) {
		for(int i = 1; i < links.size(); i++) {
			if(links.get(i).getToNode().equals(links.get(i - 1).getFromNode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes routes without link sequences
	 */
	public static void removeTransitRoutesWithoutLinkSequences(TransitSchedule schedule) {
		for(TransitLine line : schedule.getTransitLines().values()) {
			Set<TransitRoute> toRemove = new HashSet<>();
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();
				if(networkRoute.getStartLinkId() == null || networkRoute.getEndLinkId() == null) {
					removeRoute = true;
				}
				for(Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					if(linkId == null) {
						removeRoute = true;
					}
				}
				if(removeRoute) {
					log.error("NetworkRoute for " + transitRoute.getId().toString() + " incomplete. Remove route.");
					toRemove.add(transitRoute);
				}
			}
			if(!toRemove.isEmpty()) {
				for(TransitRoute transitRoute : toRemove) {
					line.removeRoute(transitRoute);
				}
			}
		}
	}

	/**
	 * Add mode "pt" to any link of the network that is
	 * passed by any transitRoute of the schedule.
	 */
	public static void addPTModeToNetwork(TransitSchedule schedule, Network network) {
		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinks = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();
				transitLinks.add(networkRoute.getStartLinkId());
				for(Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					transitLinks.add(linkId);
				}
				transitLinks.add(networkRoute.getEndLinkId());
			}
		}

		for(Id<Link> transitLinkId : transitLinks) {
			Link transitLink = networkLinks.get(transitLinkId);
			if(!transitLink.getAllowedModes().contains(TransportMode.pt)) {
				Set<String> modes = new HashSet<>();
				modes.addAll(transitLink.getAllowedModes());
				modes.add(TransportMode.pt);
				transitLink.setAllowedModes(modes);
			}
		}
	}

	/**
	 * Generates link sequences for all transit routes in the schedule, modifies the schedule.
	 * All stopFacilities used by a route must have a link referenced.
	 *
	 * @param schedule where transitRoutes should be routed
	 * @param network  the network where the routes should be routed
	 * @param routers  A map defining the Router for each scheduleTransportMode (the mode
	 *                 defined in the transitRoute).
	 */
	public static void routeSchedule(TransitSchedule schedule, Network network, Map<String, Router> routers) {
		Counter counterRoute = new Counter("route # ");

		log.info("Routing all routes with referenced links...");
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {

				Router router;
				if(routers == null) {
					log.error("No routers given, routing cannot be completed!");
					break;
				} else if(routers.size() == 1 && routers.containsKey("one")) {
					router = routers.get("one");
				} else if(routers.containsKey(transitRoute.getTransportMode())) {
					router = routers.get(transitRoute.getTransportMode());
				} else {
					log.info("Using router on ARTIFICIAL_LINK_MODE links for schedule transport mode " + transitRoute.getTransportMode() + ".");
					router = routers.get(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE);
				}

				counterRoute.incCounter();

				List<TransitRouteStop> routeStops = transitRoute.getStops();
				List<Id<Link>> linkSequence = new ArrayList<>();

				// add very first link
				linkSequence.add(routeStops.get(0).getStopFacility().getLinkId());

				// route
				for(int i = 0; i < routeStops.size() - 1; i++) {
					if(routeStops.get(i).getStopFacility().getLinkId() == null) {
						log.error("stop facility " + routeStops.get(i).getStopFacility().getName() + " (" + routeStops.get(i).getStopFacility().getId() + " not referenced!");
					}
					if(routeStops.get(i + 1).getStopFacility().getLinkId() == null) {
						log.error("stop facility " + routeStops.get(i - 1).getStopFacility().getName() + " (" + routeStops.get(i + 1).getStopFacility().getId() + " not referenced!");
					}

					Link currentLink = network.getLinks().get(routeStops.get(i).getStopFacility().getLinkId());
					Link nextLink = network.getLinks().get(routeStops.get(i + 1).getStopFacility().getLinkId());

					List<Id<Link>> path = PTMapperUtils.getLinkIdsFromPath(router.calcLeastCostPath(currentLink.getToNode(), nextLink.getFromNode()));

					if(path != null)
						linkSequence.addAll(path);

					linkSequence.add(nextLink.getId());
				}

				// add link sequence to schedule
				transitRoute.setRoute(RouteUtils.createNetworkRoute(linkSequence, network));

			}
		}
		log.info("Routing all routes with referenced links... done");
	}

	/**
	 * Removes links that are not used by public transit. Links which have a mode defined
	 * in modesToKeep are kept regardless of public transit usage.
	 */
	public static void removeNotUsedTransitLinks(TransitSchedule schedule, Network network, Set<String> modesToKeep) {
		Set<Id<Link>> usedTransitLinkIds = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				if(route.getRoute() != null)
					usedTransitLinkIds.addAll(route.getRoute().getLinkIds());
			}
		}

		Set<Id<Link>> linksToRemove = new HashSet<>();
		for(Link link : network.getLinks().values()) {
			// only remove link if there are only modes to remove on it
			if(!NetworkTools.setsShareMinOneEntry(link.getAllowedModes(), modesToKeep) && !usedTransitLinkIds.contains(link.getId())) {
				linksToRemove.add(link.getId());
			}
		}

		for(Id<Link> linkId : linksToRemove) {
			network.removeLink(linkId);
		}
	}

	/**
	 * Adds mode "bus" to links used by busses. Removes it elsewhere (osm). Adds mode to ARTIFICIAL_LINK_MODE links.
	 * Used for debugging and visualization since networkModes should be combined to pt anyway.
	 */
	public static void assignScheduleModesToLinks(TransitSchedule schedule, Network network) {
		Map<Id<Link>, Set<String>> transitLinkNetworkModes = new HashMap<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				List<Id<Link>> linkIds = route.getRoute().getLinkIds();
				for(Id<Link> linkId : linkIds) {
					MapUtils.getSet(linkId, transitLinkNetworkModes).add(route.getTransportMode());
				}
			}
		}

		for(Link link : network.getLinks().values()) {
			// only remove link if there are only modes to remove on it
			if(!transitLinkNetworkModes.containsKey(link.getId())) {
				link.setAllowedModes(Collections.singleton("car"));
			} else {
				Set<String> modes = new HashSet<>();
				Set<String> linkModes = transitLinkNetworkModes.get(link.getId());
				linkModes.addAll(link.getAllowedModes());

				for(String m : linkModes) {
					modes.add(m.toLowerCase());
				}

				link.setAllowedModes(modes);

			}
		}
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
}