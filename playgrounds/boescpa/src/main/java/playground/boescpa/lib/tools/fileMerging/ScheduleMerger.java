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

package playground.boescpa.lib.tools.fileMerging;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges two transit schedules to a new one and returns this new one.
 * All references to links and vehicles are kept.
 *
 * @author boescpa
 */
public class ScheduleMerger {
	private static Logger log = Logger.getLogger(ScheduleMerger.class);
	private static TransitScheduleFactory factory;
	private static TransitSchedule mergedSchedule;
	private static String postfix;

	public static TransitSchedule mergeSchedules(TransitSchedule transitScheduleA, TransitSchedule transitScheduleB) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		mergedSchedule = scenario.getTransitSchedule();
		factory = mergedSchedule.getFactory();

		log.info("     Merging schedules...");

		// facilities:
		Counter counter = new Counter("transit stop facilities # ");
		postfix = "a";
		for (TransitStopFacility facility : transitScheduleA.getFacilities().values()) {
			mergedSchedule.addStopFacility(copyTransitStopFacility(facility));
			counter.incCounter();
		}
		postfix = "b";
		for (TransitStopFacility facility : transitScheduleB.getFacilities().values()) {
			mergedSchedule.addStopFacility(copyTransitStopFacility(facility));
			counter.incCounter();
		}
		counter.printCounter();

		// lines:
		counter = new Counter("transit lines # ");
		postfix = "a";
		for (TransitLine line : transitScheduleA.getTransitLines().values()) {
			mergedSchedule.addTransitLine(copyTransitLine(line));
			counter.incCounter();
		}
		postfix = "b";
		for (TransitLine line : transitScheduleB.getTransitLines().values()) {
			mergedSchedule.addTransitLine(copyTransitLine(line));
			counter.incCounter();
		}
		counter.printCounter();

		log.info("     Merging schedules... done.");

		return mergedSchedule;
	}

	protected static TransitStopFacility copyTransitStopFacility(TransitStopFacility facility) {
		TransitStopFacility newFacility = factory.createTransitStopFacility(
				Id.create(facility.getId().toString() + postfix, TransitStopFacility.class),
				facility.getCoord(), facility.getIsBlockingLane());
		newFacility.setName(facility.getName());
		newFacility.setLinkId(facility.getLinkId());
		newFacility.setStopPostAreaId(facility.getStopPostAreaId());
		return newFacility;
	}

	protected static TransitLine copyTransitLine(TransitLine line) {
		TransitLine newLine = factory.createTransitLine(Id.create(line.getId().toString() + postfix, TransitLine.class));
		newLine.setName(newLine.getName());
		for (TransitRoute route : line.getRoutes().values()) {
			newLine.addRoute(copyTransitRoute(route));
		}
		return newLine;
	}

	private static TransitRoute copyTransitRoute(TransitRoute route) {
		NetworkRoute networkRoute = copyNetworkRoute(route.getRoute());
		List<TransitRouteStop> stops = copyTransitRouteStops(route.getStops());
		TransitRoute newRoute = factory.createTransitRoute(Id.create(route.getId().toString() + postfix, TransitRoute.class),
				networkRoute, stops, route.getTransportMode());
		newRoute.setDescription(route.getDescription());
		for (Departure departure : route.getDepartures().values()) {
			newRoute.addDeparture(copyDeparture(departure));
		}
		return newRoute;
	}

	private static Departure copyDeparture(Departure departure) {
		final Departure newDeparture = factory.createDeparture(Id.create(departure.getId().toString(), Departure.class),
				departure.getDepartureTime());
		newDeparture.setVehicleId(Id.create(departure.getVehicleId().toString(), Vehicle.class));
		return newDeparture;
	}

	private static List<TransitRouteStop> copyTransitRouteStops(List<TransitRouteStop> stops) {
		final List<TransitRouteStop> newStops = new ArrayList<>();
		for (TransitRouteStop stop : stops) {
			final TransitStopFacility transitStopFacility = mergedSchedule.getFacilities().get(
					Id.create(stop.getStopFacility().getId().toString() + postfix, TransitStopFacility.class));
			TransitRouteStop newStop =
					factory.createTransitRouteStop(transitStopFacility, stop.getArrivalOffset(), stop.getDepartureOffset());
			newStop.setAwaitDepartureTime(stop.isAwaitDepartureTime());
			newStops.add(newStop);
		}
		return newStops;
	}

	private static NetworkRoute copyNetworkRoute(NetworkRoute route) {
		final List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(Id.create(route.getStartLinkId().toString(), Link.class));
		for (Id<Link> linkId : route.getLinkIds()) {
			linkIds.add(Id.create(linkId.toString(), Link.class));
		}
		linkIds.add(Id.create(route.getEndLinkId().toString(), Link.class));
		NetworkRoute newRoute = RouteUtils.createNetworkRoute(linkIds, null);
		newRoute.setTravelCost(route.getTravelCost());
		newRoute.setVehicleId(
				(route.getVehicleId() != null) ? Id.create(route.getVehicleId().toString(), Vehicle.class) : null);
		newRoute.setDistance(route.getDistance());
		newRoute.setTravelTime(route.getTravelTime());
		return newRoute;
	}
}
