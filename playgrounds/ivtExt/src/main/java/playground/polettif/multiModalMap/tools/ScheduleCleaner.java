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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.*;

import java.util.HashSet;
import java.util.Set;

public class ScheduleCleaner {

	protected static Logger log = Logger.getLogger(ScheduleTools.class);

	private ScheduleCleaner() {}

	/**
	 * Removes all stop facilities not used by a transit route. Modifies the schedule.
	 *
	 * @param schedule the schedule in which the facilities should be removed
	 */
	public static void removeNotUsedStopFacilities(TransitSchedule schedule) {
		log.info("... Removing not used stop facilities");
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
}