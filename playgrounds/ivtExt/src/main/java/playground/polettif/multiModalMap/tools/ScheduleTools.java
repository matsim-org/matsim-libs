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

package playground.polettif.multiModalMap.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.PTMapperUtils;
import playground.polettif.multiModalMap.mapping.router.Router;

import java.util.*;

public class ScheduleTools {

	protected static Logger log = Logger.getLogger(ScheduleTools.class);

	/**
	 * @return the transitSchedule from scheduleFile.
	 */
	public static TransitSchedule loadTransitSchedule(String scheduleFile) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(scheduleFile);

		return scenario.getTransitSchedule();
	}

	/**
	 * @return an empty transit schedule.
	 */
	public static TransitSchedule createSchedule() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		return scenario.getTransitSchedule();
	}

	/**
	 * Writes the transit schedule to filePath.
	 */
	public static void writeTransitSchedule(TransitSchedule schedule, String filePath) {
		new TransitScheduleWriter(schedule).writeFile(filePath);
	}
	
	/**
	 * Removes all stop facilities not used by a transit route. Modifies the schedule.
	 *
	 * @param schedule the schedule in which the facilities should be removed
	 */
	public static void removeNotUsedStopFacilities(TransitSchedule schedule) {
		log.info("... Removing non used stop facilities");
		int removed = 0;

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
			removed++;
		}

		log.info("    "+removed+" stop facilities removed");
	}
	
	/**
	 * Removes routes without link sequences
	 */
	public static int removeTransitRoutesWithoutLinkSequences(TransitSchedule schedule) {
		log.info("... Removing transit routes without link sequences");

		int removed = 0;

		for(TransitLine line : schedule.getTransitLines().values()) {
			Set<TransitRoute> toRemove = new HashSet<>();
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();
				if(networkRoute == null) {
					removeRoute = true;
				} else if(networkRoute.getStartLinkId() == null || networkRoute.getEndLinkId() == null) {
					removeRoute = true;

					for(Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
						if(linkId == null) {
							removeRoute = true;
						}
					}
				}

				if(removeRoute) {
					toRemove.add(transitRoute);
				}
			}

			removed += toRemove.size();

			if(!toRemove.isEmpty()) {
				for(TransitRoute transitRoute : toRemove) {
					line.removeRoute(transitRoute);
				}
			}
		}
		return removed;
	}
	
	/**
	 * Add mode "pt" to any link of the network that is
	 * passed by any transitRoute of the schedule.
	 */
	public static void addPTModeToNetwork(TransitSchedule schedule, Network network) {
		log.info("... Adding mode \"pt\" to all links with public transit");

		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinkIds = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();
				transitLinkIds.add(networkRoute.getStartLinkId());
				transitLinkIds.addAll(networkRoute.getLinkIds());
				transitLinkIds.add(networkRoute.getEndLinkId());
			}
		}

		for(Id<Link> transitLinkId : transitLinkIds) {
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

		if(routers == null) {
			log.error("No routers given, routing cannot be completed!");
			return;
		}

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(routers.containsKey(transitRoute.getTransportMode()) && transitRoute.getStops().size() > 0) {
					Router router = routers.get(transitRoute.getTransportMode());

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

						Id<Link> currentLinkId = Id.createLinkId(routeStops.get(i).getStopFacility().getLinkId().toString());

						Link currentLink = network.getLinks().get(currentLinkId);
						Link nextLink = network.getLinks().get(routeStops.get(i + 1).getStopFacility().getLinkId());

						LeastCostPathCalculator.Path leastCostPath = router.calcLeastCostPath(currentLink.getToNode(), nextLink.getFromNode());

						if(leastCostPath != null) {
							List<Id<Link>> path = PTMapperUtils.getLinkIdsFromPath(leastCostPath);
							if(path != null) {
								linkSequence.addAll(path);
							} else {
								linkSequence = null;
								break;
							}
						} else {
							linkSequence = null;
							break;
						}

						linkSequence.add(nextLink.getId());
					}

					// add link sequence to schedule
					if(linkSequence != null) {
						transitRoute.setRoute(RouteUtils.createNetworkRoute(linkSequence, network));
					} else {
						log.error("No path found for TransitRoute " + transitRoute.getId() + " on TransitLine " + transitLine.getId());
					}
				}
			}
		}
		log.info("Routing all routes with referenced links... done");
	}
	
	/**
	 * Removes links that are not used by public transit. Links which have a mode defined
	 * in modesToKeep are kept regardless of public transit usage.
	 */
	public static void removeNotUsedTransitLinks(TransitSchedule schedule, Network network, Set<String> modesToKeep) {
		log.info("... Removing links that are not used by public transit");
		int removed = 0;

		Set<Id<Link>> usedTransitLinkIds = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				if(route.getRoute() != null)
					usedTransitLinkIds.add(route.getRoute().getStartLinkId());
					usedTransitLinkIds.addAll(route.getRoute().getLinkIds());
					usedTransitLinkIds.add(route.getRoute().getEndLinkId());
			}
		}

		Set<Id<Link>> linksToRemove = new HashSet<>();
		for(Link link : network.getLinks().values()) {
			// only remove link if there are only modes to remove on it
			if(!MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), modesToKeep) && !usedTransitLinkIds.contains(link.getId())) {
				linksToRemove.add(link.getId());
			}
			// only retain modes that are actually used
			else if(MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), modesToKeep) && !usedTransitLinkIds.contains(link.getId())) {
				link.setAllowedModes(MiscUtils.getSharedSetStringEntries(link.getAllowedModes(), modesToKeep));
			}
		}

		for(Id<Link> linkId : linksToRemove) {
			network.removeLink(linkId);
		}

		log.info("    "+removed+" links removed");
	}
	
	/**
	 * Adds mode the schedule transport mode to links. Removes all network
	 * modes elsewhere. Adds mode "artificial" to artificial
	 * links. Used for debugging and visualization since networkModes
	 * should be combined to pt anyway.
	 */
	public static void assignScheduleModesToLinks(TransitSchedule schedule, Network network) {
		log.debug("... Assigning schedule transport mode to network");

		Map<Id<Link>, Set<String>> transitLinkNetworkModes = new HashMap<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				Set<Id<Link>> linkIds = new HashSet<>();
				linkIds.add(route.getRoute().getStartLinkId());
				linkIds.addAll(route.getRoute().getLinkIds());
				linkIds.add(route.getRoute().getEndLinkId());
				for(Id<Link> linkId : linkIds) {
					MapUtils.getSet(linkId, transitLinkNetworkModes).add(route.getTransportMode());
				}
			}
		}

		for(Link link : network.getLinks().values()) {
			if(transitLinkNetworkModes.containsKey(link.getId())) {
				Set<String> modes = new HashSet<>();
				Set<String> linkModes = transitLinkNetworkModes.get(link.getId());
				linkModes.addAll(link.getAllowedModes());

				for(String m : linkModes) {
					modes.add(m);
				}

				link.setAllowedModes(modes);
			}
		}
	}
	
	/**
	 *
	 * @param schedule
	 * @param network
	 */
	public static void replaceNonCarModesWithPT(TransitSchedule schedule, Network network) {
		log.info("... Replacing all non-car link modes with \"pt\"");

		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinkIds = new HashSet<>();

		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().size() > 0 && link.getAllowedModes().contains("car")) {
				Set<String> modes = new HashSet<>();
				modes.add(TransportMode.car);
				modes.add(TransportMode.pt);
				link.setAllowedModes(modes);
			}
		}
	}
}