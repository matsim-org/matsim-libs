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

package playground.polettif.multiModalMap.validation;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.PTMapperUtils;
import playground.polettif.multiModalMap.mapping.router.FastAStarRouter;
import playground.polettif.multiModalMap.mapping.router.Router;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides tools for rerouting schedules.
 */
public class ScheduleEditor {

	protected static Logger log = Logger.getLogger(ScheduleEditor.class);


	private final Network network;
	private final TransitSchedule schedule;
	private final TransitScheduleFactory scheduleFactory;
	private final Router router;

	private final Map<String, TransitRoute> transitRoutes;

	public ScheduleEditor(Network network, TransitSchedule schedule, Router router) {
		this.network = network;
		this.schedule = schedule;
		this.scheduleFactory = schedule.getFactory();
		this.router = router;

		this.transitRoutes = new HashMap<>();

		// put transit routes in separate container for easier access
		/*
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(Map.Entry<Id<TransitRoute>, TransitRoute> routeEntry : transitLine.getRoutes().entrySet()) {
				if(transitRoutes.put(routeEntry.getKey().toString(), routeEntry.getValue()) != null) {
					throw new IllegalArgumentException("There is more than one route with id " + routeEntry.getKey());
				}
			}
		}
		*/
	}

	public static void main(String[] args) throws IOException {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		new TransitScheduleReader(sc).readFile(args[0]);
		new MatsimNetworkReader(network).readFile(args[1]);
		TransitSchedule schedule = sc.getTransitSchedule();

		ScheduleEditor scheduleEditor = new ScheduleEditor(network, schedule, new FastAStarRouter(network));

		scheduleEditor.parseCommandCsv(args[2]);
		PTMapperUtils.assignScheduleModesToLinks(schedule, network);
		scheduleEditor.writeFiles(args[3], args[4]);
	}

	private void writeFiles(String outputScheduleFile, String outputNetworkFile) {
		log.info("Writing schedule and network to file...");
		new TransitScheduleWriter(schedule).writeFile(outputScheduleFile);
		new NetworkWriter(network).write(outputNetworkFile);
	}

	/**
	 * Parses a command file (csv) and runs the commands specified
	 * @param filePath
	 * @throws IOException
	 */
	public void parseCommandCsv(String filePath) throws IOException {
		CSVReader reader = new CSVReader(new FileReader(filePath), ';');

		String[] line = reader.readNext();
		while(line != null) {
			TransitRoute transitRoute;
			Id<Link> oldLinkId;
			Id<Link> newLinkId;
			Id<TransitStopFacility> oldSFId;
			Id<TransitStopFacility> newSFId;
			Id<TransitStopFacility> stopFacilityId;

			if("reRouteViaLink".equalsIgnoreCase(line[0])) {
				transitRoute = getTransitRoute(line[1], line[2]);
				oldLinkId = Id.createLinkId(line[3]);
				newLinkId = Id.createLinkId(line[4]);
				if(transitRoute == null) {
					log.error("TransitRoute " + line[2] + " on TransitLine " + line[1] + " not found!");
				} else {
					reRouteViaLink(transitRoute, oldLinkId, newLinkId);
				}
			} else if("replaceStopFacilityInRoute".equalsIgnoreCase(line[0])) {
				transitRoute = getTransitRoute(line[1], line[2]);
				oldSFId = Id.create(line[3], TransitStopFacility.class);
				newSFId = Id.create(line[4], TransitStopFacility.class);
				if(transitRoute == null) {
					log.error("TransitRoute " + line[2] + " on TransitLine " + line[1] + " not found!");
				} else {
					replaceStopFacilityInRoute(transitRoute, oldSFId, newSFId);
				}
			} else if("changeRefLink".equals(line[0])) {
				transitRoute = getTransitRoute(line[1], line[2]);
				stopFacilityId = Id.create(line[3], TransitStopFacility.class);
				newLinkId = Id.create(line[4], Link.class);
				if(transitRoute == null) {
					log.error("TransitRoute " + line[2] + " on TransitLine " + line[1] + " not found!");
				} else {
//					changeRefLink(transitRoute, stopFacilityId, newLinkId);
				}
			} else {
				throw new IllegalArgumentException("Invalid command \"" + line[0] + "\"");
			}


			line = reader.readNext();
		}

		reader.close();
	}

	private TransitRoute getTransitRoute(String transitLine, String transitRoute) {
		return schedule.getTransitLines().get(Id.create(transitLine, TransitLine.class)).getRoutes().get(Id.create(transitRoute, TransitRoute.class));
	}

	/**
	 *
	 * @param transitRoute
	 * @param oldLinkId
	 * @param newLinkId
	 */
	public void reRouteViaLink(TransitRoute transitRoute, Id<Link> oldLinkId, Id<Link> newLinkId) {
		List<TransitRouteStop> stopSequence = transitRoute.getStops();
		List<Id<Link>> linkSequence = transitRoute.getRoute().getLinkIds();

		List<Id<Link>> refLinkIds = stopSequence.stream().map(routeStop -> routeStop.getStopFacility().getLinkId()).collect(Collectors.toList());

		if(refLinkIds.contains(oldLinkId)) {
			log.error("Link is referenced to a stop facility, reRouteViaLink cannot be performed. Use changeRefLink instead.");
		} else {
			int i = 0;
			TransitRouteStop fromRouteStop = stopSequence.get(i);
			for(Id<Link> linkId : linkSequence) {
				if(linkId.equals(oldLinkId)) {
					rerouteFromStop(transitRoute, fromRouteStop, newLinkId);
					break;
				}
				if(linkId.equals(refLinkIds.get(i))) {
					fromRouteStop = stopSequence.get(i++);
					i++;
				}
			}
		}
	}

	/**
	 *
	 * @param transitRoute
	 * @param fromRouteStop
	 * @param viaLinkId
	 */
	public void rerouteFromStop(TransitRoute transitRoute, TransitRouteStop fromRouteStop, Id<Link> viaLinkId) {
		List<TransitRouteStop> routeStops = transitRoute.getStops();
		TransitRouteStop toRouteStop = routeStops.get(routeStops.indexOf(fromRouteStop)+1);

		Id<Link> cutFromLinkId = fromRouteStop.getStopFacility().getLinkId();
		Link cutFromLInk = network.getLinks().get(cutFromLinkId);
		Id<Link> cutToLinkId = toRouteStop.getStopFacility().getLinkId();
		Link cutToLink = network.getLinks().get(cutToLinkId);
		Link viaLink = network.getLinks().get(viaLinkId);

		NetworkRoute routeBeforeCut = transitRoute.getRoute().getSubRoute(transitRoute.getRoute().getStartLinkId(), cutFromLinkId);
		NetworkRoute routeAfterCut = transitRoute.getRoute().getSubRoute(cutToLinkId, transitRoute.getRoute().getEndLinkId());

		LeastCostPathCalculator.Path path1 = router.calcLeastCostPath(cutFromLInk, viaLink);
		LeastCostPathCalculator.Path path2 = router.calcLeastCostPath(viaLink, cutToLink);

		List<Id<Link>> newLinkSequence = new ArrayList<>();
		if(path1 != null && path2 != null) {
			newLinkSequence.add(routeBeforeCut.getStartLinkId());
			newLinkSequence.addAll(routeBeforeCut.getLinkIds());
			newLinkSequence.add(routeBeforeCut.getEndLinkId());
			newLinkSequence.addAll(PTMapperUtils.getLinkIdsFromPath(path1));
			newLinkSequence.add(viaLinkId);
			newLinkSequence.addAll(PTMapperUtils.getLinkIdsFromPath(path2));
			newLinkSequence.add(routeAfterCut.getStartLinkId());
			newLinkSequence.addAll(routeAfterCut.getLinkIds());
			newLinkSequence.add(routeAfterCut.getEndLinkId());
			newLinkSequence.add(transitRoute.getRoute().getEndLinkId());
			transitRoute.setRoute(RouteUtils.createNetworkRoute(newLinkSequence, network));
		}
	}

	public void changeRefLink(TransitRoute transitRoute, TransitStopFacility stopFacilityId, Id<Link> newRefLinkId) {
//		TransitStopFacility toReplace = schedule.getFacilities().get(stopFacilityId);
		// searches the child stop facility with the given postareaid, replaces it with another stop facility
		// sf maybe needs to be created
	}

	public void replaceStopFacilityInRoute(TransitRoute transitRoute, Id<TransitStopFacility> toReplaceId, Id<TransitStopFacility> replaceWithId) {
		TransitStopFacility toReplace = schedule.getFacilities().get(toReplaceId);
		TransitStopFacility replaceWith = schedule.getFacilities().get(replaceWithId);

		if(toReplace == null) {
			log.warn("StopFacility " + toReplaceId + " not found in schedule!");
		} else if(replaceWith == null) {
			log.warn("StopFacility " + replaceWithId + " not found in schedule!");
		} else {
			replaceStopFacilityInRoute(transitRoute, toReplace, replaceWith);
		}
	}

	private void replaceStopFacilityInRoute(TransitRoute transitRoute, TransitStopFacility toReplace, TransitStopFacility replaceWith) {
		TransitRouteStop routeStopToReplace = transitRoute.getStop(toReplace);
		if(routeStopToReplace != null) {
			routeStopToReplace.setStopFacility(replaceWith);
			refreshTransitRoute(transitRoute);
		} else {
			log.warn("StopFacility " + toReplace.getId() + " not found in TransitRoute " + transitRoute.getId());
		}
	}

	// todo method to change all paht for transit routes with a given parent stop sequence

	public void replaceStopFacilityInAllRoutes(Id<TransitStopFacility> toReplaceId, Id<TransitStopFacility> replaceWithId) {
		TransitStopFacility toReplace = schedule.getFacilities().get(toReplaceId);
		TransitStopFacility replaceWith = schedule.getFacilities().get(replaceWithId);

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				replaceStopFacilityInRoute(route, toReplace, replaceWith);
			}
		}
	}

	/**
	 * "Refreshes" the transit route by routing between all referenced links
	 * of the stop facilities.
	 */
	public void refreshTransitRoute(TransitRoute transitRoute) {
		List<TransitRouteStop> routeStops = transitRoute.getStops();
		List<Id<Link>> linkSequence = new ArrayList<>();
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

			List<Id<Link>> path = PTMapperUtils.getLinkIdsFromPath(router.calcLeastCostPath(currentLink.getToNode(), nextLink.getFromNode()));

			if(path != null)
				linkSequence.addAll(path);

			linkSequence.add(nextLink.getId());
		}

		// add link sequence to schedule
		transitRoute.setRoute(RouteUtils.createNetworkRoute(linkSequence, network));
	}
}