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

package contrib.publicTransitMapping.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import contrib.publicTransitMapping.tools.CoordTools;
import contrib.publicTransitMapping.tools.MiscUtils;

import java.util.*;

/**
 * Methods to clean transit schedules by removing
 * routes and stop facilities.
 *
 * @author polettif
 */
public class ScheduleCleaner {

	protected static Logger log = Logger.getLogger(ScheduleTools.class);

	private ScheduleCleaner() {
	}

	/**
	 * Removes transit routes without link sequences and not used stop facilities from the schedule.
	 * @param args [0] schedule file, [1] output schedule file (if original file should not be overwritten)
	 */
	public static void main(String[] args) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(args[0]);
		removeTransitRoutesWithoutLinkSequences(schedule);
		removeNotUsedStopFacilities(schedule);
		if(args.length == 1) {
			ScheduleTools.writeTransitSchedule(schedule, args[0]);
		} else if(args.length == 2) {
			ScheduleTools.writeTransitSchedule(schedule, args[1]);
		} else {
			throw new IllegalArgumentException("Wrong number of arguments given");
		}
	}

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

		log.info("    " + removed + " stop facilities removed");
	}

	/**
	 * Removes routes without link sequences
	 */
	public static int removeTransitRoutesWithoutLinkSequences(TransitSchedule schedule) {
		log.info("... Removing transit routes without link sequences");

		int removed = 0;

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : new HashSet<>(transitLine.getRoutes().values())) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();
				if(networkRoute == null) {
					removeRoute = true;
				}
				List<Id<Link>> linkIds = ScheduleTools.getTransitRouteLinkIds(transitRoute);
				if(linkIds.size() == 0) {
					removeRoute = true;
				} else {
					for(Id<Link> linkId : linkIds) {
						if(linkId == null) {
							removeRoute = true;
						}
					}
				}

				if(removeRoute) {
					transitLine.removeRoute(transitRoute);
					removed++;
				}
			}
		}
		log.info("... " + removed + " transit routes removed");

		return removed;
	}

	/**
	 * Removes links that are not used by public transit. Links which have a mode defined
	 * in modesToKeep are kept regardless of public transit usage. Opposite links of used
	 * links are kept if keepOppositeLinks is true.
	 */
	public static void removeNotUsedTransitLinks(TransitSchedule schedule, Network network, Set<String> modesToKeep, boolean keepOppositeLinks) {
		log.info("... Removing links that are not used by public transit");

		Set<Id<Link>> usedTransitLinkIds = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				if(route.getRoute() != null)
					usedTransitLinkIds.addAll(ScheduleTools.getTransitRouteLinkIds(route));
			}
		}

		Map<Id<Link>, ? extends Link> links = network.getLinks();
		if(keepOppositeLinks) {
			for(Id<Link> linkId : new HashSet<>(usedTransitLinkIds)) {
				Link oppositeLink = NetworkTools.getOppositeLink(links.get(linkId));
				if(oppositeLink != null) usedTransitLinkIds.add(oppositeLink.getId());
			}
		}

		int linksRemoved = 0;
		for(Link link : new HashSet<>(network.getLinks().values())) {
			// only remove link if there are only modes to remove on it
			if(!contrib.publicTransitMapping.tools.MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), modesToKeep) && !usedTransitLinkIds.contains(link.getId())) {
				network.removeLink(link.getId());
				linksRemoved++;
			}
			// only retain modes that are actually used
			else if(contrib.publicTransitMapping.tools.MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), modesToKeep) && !usedTransitLinkIds.contains(link.getId())) {
				link.setAllowedModes(MiscUtils.getSharedSetStringEntries(link.getAllowedModes(), modesToKeep));
			}
		}

		// removing nodes
		for(Node n : new HashSet<>(network.getNodes().values())) {
			if(n.getOutLinks().size() == 0 && n.getInLinks().size() == 0) {
				network.removeNode(n.getId());
			}
		}

		log.info("    " + linksRemoved + " links removed");
	}

	/**
	 * Changes the schedule to an unmapped schedule by removes all link sequences
	 * from a transit schedule and removing referenced links from stop facilities.
	 */
	public static void removeMapping(TransitSchedule schedule) {
		log.info("... Removing reference links and link sequences from schedule");

		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			stopFacility.setLinkId(null);
		}

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				route.setRoute(null);
			}
		}
	}

	/**
	 * Removes duplicate departures (with the same departure time)
	 * from a transit route.
	 */
	public static void cleanDepartures(TransitSchedule schedule) {
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				final Set<Double> departureTimes = new HashSet<>();
				final List<Departure> departuresToRemove = new ArrayList<>();
				for(Departure departure : transitRoute.getDepartures().values()) {
					double dt = departure.getDepartureTime();
					if(departureTimes.contains(dt)) {
						departuresToRemove.add(departure);
					} else {
						departureTimes.add(dt);
					}
				}
				for(Departure departure2Remove : departuresToRemove) {
					transitRoute.removeDeparture(departure2Remove);
				}
			}
		}
	}

	/**
	 * Combines transit routes with identical stop sequences. Only routes within
	 * a transit line are combined.
	 */
	public static void combineIdenticalTransitRoutes(TransitSchedule schedule) {
		log.info("Combining TransitRoutes with identical stop sequence...");
		int combined = 0;
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			Map<List<String>, List<TransitRoute>> profiles = new HashMap<>();
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<String> stopFacilitySequence = new LinkedList<>();
				for(TransitRouteStop routeStop : transitRoute.getStops()) {
					stopFacilitySequence.add(routeStop.getStopFacility().getId().toString());
				}
				MapUtils.getList(stopFacilitySequence, profiles).add(transitRoute);
			}

			for(List<TransitRoute> routeList : profiles.values()) {
				if(routeList.size() > 1) {
					TransitRoute finalRoute = routeList.get(0);
					for(int i = 1; i < routeList.size(); i++) {

						combined++;
						transitLine.removeRoute(routeList.get(i));
						for(Departure departure : routeList.get(i).getDepartures().values()) {
							finalRoute.addDeparture(departure);
						}
					}
				}
			}
		}
		log.info("... Combined " + combined + " transit routes");
	}

	/**
	 * Combines TransitRoutes with an identical stop sequence and combines them to one
	 */
	public static void uniteSameRoutesWithJustDifferentDepartures(TransitSchedule schedule) {
		log.info("Combining TransitRoutes with identical stop sequence...");

		long totalNumberOfDepartures = 0;
		long departuresWithChangedSchedules = 0;
		long totalNumberOfStops = 0;
		long stopsWithChangedTimes = 0;
		double changedTotalTimeAtStops = 0.;
		List<Double> timeChanges = new ArrayList<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			// Collect all route profiles
			final Map<String, List<TransitRoute>> routeProfiles = new HashMap<>();
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				totalNumberOfDepartures += transitRoute.getDepartures().size();
				totalNumberOfStops += transitRoute.getDepartures().size() * transitRoute.getStops().size();
				String routeProfile = transitRoute.getStops().get(0).getStopFacility().getId().toString();
				for(int i = 1; i < transitRoute.getStops().size(); i++) {
					//routeProfile = routeProfile + "-" + route.getStops().get(i).toString() + ":" + route.getStops().get(i).getDepartureOffset();
					routeProfile = routeProfile + "-" + transitRoute.getStops().get(i).getStopFacility().getId().toString();
				}
				MapUtils.getList(routeProfile, routeProfiles).add(transitRoute);
			}
			// Check profiles and if the same, add latter to former.
			for(List<TransitRoute> routesToUnite : routeProfiles.values()) {
				TransitRoute finalRoute = routesToUnite.get(0);
				for(int i = 1; i < routesToUnite.size(); i++) {
					TransitRoute routeToAdd = routesToUnite.get(i);
					// unite departures
					for(Departure departure : routeToAdd.getDepartures().values()) {
						finalRoute.addDeparture(departure);
					}
					line.removeRoute(routeToAdd);
					// make analysis
					int numberOfDepartures = routeToAdd.getDepartures().size();
					boolean departureWithChangedDepartureTimes = false;
					for(int j = 0; j < finalRoute.getStops().size(); j++) {
						double changedTotalTimeAtStop =
								Math.abs(finalRoute.getStops().get(j).getArrivalOffset() - routeToAdd.getStops().get(j).getArrivalOffset())
										+ Math.abs(finalRoute.getStops().get(j).getDepartureOffset() - routeToAdd.getStops().get(j).getDepartureOffset());
						if(changedTotalTimeAtStop > 0) {
							stopsWithChangedTimes += numberOfDepartures;
							changedTotalTimeAtStops += changedTotalTimeAtStop * numberOfDepartures;
							for(int k = 0; k < numberOfDepartures; k++) {
								timeChanges.add(changedTotalTimeAtStop);
							}
							departureWithChangedDepartureTimes = true;
						}
					}
					if(departureWithChangedDepartureTimes) {
						departuresWithChangedSchedules += numberOfDepartures;
					}
				}
			}
		}
		log.info("   Total Number of Departures: " + totalNumberOfDepartures);
		log.info("   Number of Departures with changed schedule: " + departuresWithChangedSchedules);
		log.info("   Total Number of Stops: " + totalNumberOfStops);
		log.info("   Number of Stops with changed departure or arrival times: " + stopsWithChangedTimes);
		log.info("   Total time difference caused by changed departure or arrival times: " + changedTotalTimeAtStops);
		log.info("   Average time difference caused by changed times: " + (changedTotalTimeAtStops / stopsWithChangedTimes));
		log.info("   Average time difference over all stops caused by changed times: " + (changedTotalTimeAtStops / totalNumberOfStops));
	}

	/**
	 * Removes vehicles that are not used in the schedule
	 */
	public static void cleanVehicles(TransitSchedule schedule, Vehicles vehicles) {
		log.info("Removing not used vehicles...");
		int removed = 0;
		final Set<Id<Vehicle>> usedVehicles = new HashSet<>();
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(Departure departure : transitRoute.getDepartures().values()) {
					usedVehicles.add(departure.getVehicleId());
				}
			}
		}
		for(Id<Vehicle> vehicleId : new HashSet<>(vehicles.getVehicles().keySet())) {
			if(!usedVehicles.contains(vehicleId)) {
				vehicles.removeVehicle(vehicleId);
				removed++;
			}
		}
		log.info(removed + " vehicles removed");
	}


	/**
	 * Replaces all schedule transport modes (i.e. bus or rail)
	 * with <tt>mode</tt>.
	 */
	public static void replaceScheduleModes(TransitSchedule schedule, String mode) {
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				transitRoute.setTransportMode(mode);
			}
		}
	}

	/**
	 * Removes the given route from the schedule
	 * @return true if the route could be removed
	 */
	public static boolean removeRoute(TransitSchedule schedule, Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {
		TransitLine transitLine = schedule.getTransitLines().get(transitLineId);
		return transitLine != null && transitLine.removeRoute(transitLine.getRoutes().get(transitRouteId));
	}

	/**
	 * Cuts the schedule. All routes that pass stops within radius of center are kept
	 */
	public static void cutSchedule(TransitSchedule schedule, Coord center, double radius) {
		Set<Id<TransitStopFacility>> stopsInArea = new HashSet<>();
		for (TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			if(CoordUtils.calcEuclideanDistance(center, stopFacility.getCoord()) <= radius) {
				stopsInArea.add(stopFacility.getId());
			}
		}
		cutSchedule(schedule, stopsInArea);
	}

	/**
	 * Cuts the schedule. All routes that pass stops northeast of SWcorner
	 * and southwest of NEcorner (i.e. within the box) are kept
	 */
	public static void cutSchedule(TransitSchedule schedule, Coord SWcorner, Coord NEcorner) {
		Set<Id<TransitStopFacility>> stopsInArea = new HashSet<>();
		for (TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			if(CoordTools.isInArea(stopFacility.getCoord(), SWcorner, NEcorner)) {
				stopsInArea.add(stopFacility.getId());
			}
		}
		cutSchedule(schedule, stopsInArea);
	}

	/**
	 * Cuts the schedule, retains routes that pass stops defined in stopsInArea
	 */
	public static void cutSchedule(TransitSchedule schedule, Set<Id<TransitStopFacility>> stopsInArea) {
		log.info("Cutting schedule...");
		log.info("   area contains " + stopsInArea.size() + " stops.");

		// Identify all routes not crossing area and therefore to remove:
		int routesRemoved = 0;
		Set<TransitLine> linesToRemove = new HashSet<>();
		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : new HashSet<>(transitLine.getRoutes().values())) {
				boolean toKeep = false;
				for (TransitRouteStop stop : transitRoute.getStops()) {
					if (stopsInArea.contains(stop.getStopFacility().getId())) {
						toKeep = true;
					}
				}
				if (!toKeep) {
					transitLine.removeRoute(transitRoute);
					routesRemoved++;
				}
			}
			if (transitLine.getRoutes().isEmpty()) {
				linesToRemove.add(transitLine);
			}
		}
		log.info("   routes removed: " + routesRemoved);

		for (TransitLine lineToRemove : linesToRemove) {
			schedule.removeTransitLine(lineToRemove);
		}
		log.info("   lines removed: " + linesToRemove.size());

		removeNotUsedStopFacilities(schedule);
	}

	/**
	 * Removes all transit routes using one of the given modes from the schedule
	 */
	public static void removeTransitRouteByMode(TransitSchedule schedule, Set<String> modesToRemove) {
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : new HashSet<>(transitLine.getRoutes().values())) {
				if(modesToRemove.contains(transitRoute.getTransportMode())) {
					transitLine.removeRoute(transitRoute);
				}
			}
		}
	}

	/**
	 * Removes all invalid transit routes based on a TransitScheduleValidator result.
	 * @deprecated Experimental. Guesses the ids based on the error strings, might not always work
	 */
	@Deprecated
	public static void removeInvalidTransitRoutes(TransitScheduleValidator.ValidationResult result, TransitSchedule schedule) {
		Set<Tuple<Id<TransitLine>, Id<TransitRoute>>> toRemove = new HashSet<>();
		for(String error : result.getErrors()) {
			String[] lineSplit = error.split(", route ");
			String tranistLineId = lineSplit[0].substring(13);

			String transitRouteId;
			if(lineSplit[1].contains(" contains a link that is not part of the network")) {
				transitRouteId = lineSplit[1].split	(" contains a link that is not part of the network")[0];
			} else if(lineSplit[1].contains			(" has inconsistent network route")) {
				transitRouteId = lineSplit[1].split	(" has inconsistent network route")[0];
			} else if(lineSplit[1].contains			(" has no network route")) {
				transitRouteId = lineSplit[1].split	(" has no network route")[0];
			} else if(lineSplit[1].contains			(" contains a link that is not part of the network: ")) {
				transitRouteId = lineSplit[1].split	(" contains a link that is not part of the network: ")[0];
			} else if(lineSplit[1].contains			(": Stop ")) {
				transitRouteId = lineSplit[1].split	(": Stop ")[0];
			} else if(lineSplit[1].contains			(" has no linkId, but is used by transit line ")) {
				transitRouteId = lineSplit[1].split	(" has no linkId, but is used by transit line ")[0];
			} else if(lineSplit[1].contains			(" contains a stop ")) {
				transitRouteId = lineSplit[1].split	(" contains a stop ")[0];
			} else if(lineSplit[1].contains			(": The ")) {
				transitRouteId = lineSplit[1].split	(": The ")[0];
			} else {
				throw new IllegalArgumentException("Error String from ValidationResult not recognised! (" + error + ")");
			}
			toRemove.add(new Tuple<>(Id.create(tranistLineId, TransitLine.class), Id.create(transitRouteId, TransitRoute.class)));
		}

		for(Tuple<Id<TransitLine>, Id<TransitRoute>> t : toRemove) {
			ScheduleCleaner.removeRoute(schedule, t.getFirst(), t.getSecond());
		}
	}
}