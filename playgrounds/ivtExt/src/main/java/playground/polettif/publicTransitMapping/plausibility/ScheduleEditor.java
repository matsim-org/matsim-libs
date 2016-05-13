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

package playground.polettif.publicTransitMapping.plausibility;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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
import playground.polettif.publicTransitMapping.mapping.PTMapperUtils;
import playground.polettif.publicTransitMapping.mapping.router.Router;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides tools for rerouting and adapting schedules via csv.
 *
 * @author polettif
 */
public class ScheduleEditor {

	protected static Logger log = Logger.getLogger(ScheduleEditor.class);


	private final Network network;
	private final TransitSchedule schedule;
	private final TransitScheduleFactory scheduleFactory;
	private final Map<String, Router> routers;

	private final Map<String, TransitRoute> transitRoutes;

	public ScheduleEditor(TransitSchedule schedule, Network network, Map<String, Router> routers) {
		this.network = network;
		this.schedule = schedule;
		this.scheduleFactory = schedule.getFactory();
		this.routers = routers;

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

	// commands
	private final String CHILD_FACILITY_SUFFIX = "[.]link:";
	private final String REPLACE_STOP_FACILITY = "replaceStopFacility";
	private final String ALL_TRANSIT_ROUTES_ON_LINK = "allTransitRoutesOnLink";
	private final String CHANGE_REF_LINK = "changeRefLink";


	/*
	Code:
	["replaceStopFacility"] [TransitRoute] [toReplaceId] [replaceWithId]
	["replaceStopFacility"] ["allTransitRoutesOnLink"] [linkId] [toReplaceId] [replaceWithId]
	["replaceStopFacility"] ["allTransitRoutesOnLink"] [firstLinkId] [secondLinkId] [toReplaceId] [replaceWithId]
	 */

	public static void main(String[] args) throws IOException {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		new TransitScheduleReader(sc).readFile(args[0]);
		new MatsimNetworkReader(network).readFile(args[1]);
		TransitSchedule schedule = sc.getTransitSchedule();

//		ScheduleEditor scheduleEditor = new ScheduleEditor(network, schedule, new FastAStarRouter(network));

//		scheduleEditor.parseCommandCsv(args[2]);
//		ScheduleTools.assignScheduleModesToLinks(schedule, network);
//		scheduleEditor.writeFiles(args[3], args[4]);
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
			cmd(line);
			line = reader.readNext();
		}
		reader.close();
	}

	private void cmd(String[] cmd) {
		TransitRoute transitRoute;
		Set<TransitRoute> transitRoutes;
		Id<Link> oldLinkId;
		Id<Link> newLinkId;
		Id<TransitStopFacility> oldSFId;
		Id<TransitStopFacility> newSFId;
		Id<TransitStopFacility> stopFacilityId;

		if("rrViaLink".equalsIgnoreCase(cmd[0])) {
			transitRoute = getTransitRoute(cmd[1], cmd[2]);
			oldLinkId = Id.createLinkId(cmd[3]);
			newLinkId = Id.createLinkId(cmd[4]);
			if(transitRoute == null) {
				throw new IllegalArgumentException("TransitRoute " + cmd[2] + " on TransitLine " + cmd[1] + " not found!");
			} else {
				rrViaLink(transitRoute, oldLinkId, newLinkId);
			}

			/*
			["changeRefLink"] [TransitRoute] [toReplaceId] [replaceWithId]
		X	["replaceStopFacility"] ["allTransitRoutesOnLink"] [linkId] [toReplaceId] [replaceWithId]
		X	["replaceStopFacility"] ["allTransitRoutesOnLink"] [linkId] [toReplaceId (ParentStop)] [replaceWithId]
		X	["replaceStopFacility"] ["allTransitRoutesOnLink"] [firstLinkId] [secondLinkId] [toReplaceId] [replaceWithId]
			 */
		} else if(CHANGE_REF_LINK.equals(cmd[0])) {
			switch(cmd[1]) {
				case ALL_TRANSIT_ROUTES_ON_LINK :
					transitRoutes = getTransitRoutesOnLink(Id.createLinkId(cmd[2]));
					for(TransitRoute tr : transitRoutes) {
						changeRefLink(tr, cmd[3], cmd[4]);
					}
				break;
				default :
					changeRefLink(getTransitRoute(cmd[1], cmd[2]), cmd[3], cmd[4]);

				/*
				default :
					transitRoute = getTransitRoute(cmd[1], cmd[2]);
					oldSFId = Id.create(cmd[3], TransitStopFacility.class);
					newSFId = Id.create(cmd[4], TransitStopFacility.class);
					if(transitRoute == null) {
						throw new IllegalArgumentException("TransitRoute " + cmd[2] + " on TransitLine " + cmd[1] + " not found!");
					} else {
						replaceStopFacilityInRoute(transitRoute, oldSFId, newSFId);
					}
					break;*/
			}

		} else if("changeRefLink".equals(cmd[0])) {
			transitRoute = getTransitRoute(cmd[1], cmd[2]);
			stopFacilityId = Id.create(cmd[3], TransitStopFacility.class);
			newLinkId = Id.create(cmd[4], Link.class);
			if(transitRoute == null) {
				throw new IllegalArgumentException("TransitRoute " + cmd[2] + " on TransitLine " + cmd[1] + " not found!");
			} else {
//					changeRefLink(transitRoute, stopFacilityId, newLinkId);
			}
		} else if("//".equals(cmd[0].substring(0, 2))) {
			// comment
		} else {
			throw new IllegalArgumentException("Invalid command \"" + cmd[0] + "\"");
		}
	}

	private TransitRoute getTransitRoute(String transitLineStr, String transitRouteStr) {
		TransitLine transitLine = schedule.getTransitLines().get(Id.create(transitLineStr, TransitLine.class));

		if(transitLine == null) {
			throw new IllegalArgumentException("TransitLine " + transitLineStr + " not found!");
		}

		return transitLine.getRoutes().get(Id.create(transitRouteStr, TransitRoute.class));
	}

	/**
	 * Reroutes the section between two stops that passes the oldlink via the new link
	 * @param transitRoute
	 * @param oldLinkId
	 * @param newLinkId
	 */
	public void rrViaLink(TransitRoute transitRoute, Id<Link> oldLinkId, Id<Link> newLinkId) {
		List<TransitRouteStop> stopSequence = transitRoute.getStops();
		List<Id<Link>> linkSequence = transitRoute.getRoute().getLinkIds();

		List<Id<Link>> refLinkIds = stopSequence.stream().map(routeStop -> routeStop.getStopFacility().getLinkId()).collect(Collectors.toList());

		if(refLinkIds.contains(oldLinkId)) {
			throw new IllegalArgumentException("Link is referenced to a stop facility, rrViaLink cannot be performed. Use changeRefLink instead.");
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
	 * Reroutes the section after fromRouteStop via the given viaLinkId
	 * @param transitRoute
	 * @param fromRouteStop
	 * @param viaLinkId
	 */
	public void rerouteFromStop(TransitRoute transitRoute, TransitRouteStop fromRouteStop, Id<Link> viaLinkId) {
		Router router = routers.get(transitRoute.getTransportMode());

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

	private TransitStopFacility getChildStopInRoute(TransitRoute transitRoute, Id<TransitStopFacility> parentStopId) {
		String parentStopString = parentStopId.toString();
		for(TransitRouteStop routeStop : transitRoute.getStops()) {
			String[] childStopSplit = routeStop.getStopFacility().getId().toString().split(CHILD_FACILITY_SUFFIX);
			if(parentStopString.equals(childStopSplit[0])) {
				return routeStop.getStopFacility();
			}
		}
		log.warn("No child facility for " + parentStopId + " in Transit Route " + transitRoute + "found.");
		return null;
	}

	/**
	 * changes the child stop in the given route
	 */
	private void changeRefLink(TransitRoute transitRoute, String parentStopIdStr, String newRefLinkIdStr) {
		Id<TransitStopFacility> parentStopId = Id.create(parentStopIdStr, TransitStopFacility.class);
		Id<Link> newRefLinkId = Id.createLinkId(newRefLinkIdStr);
		Id<TransitStopFacility> newChildStopId = Id.create(parentStopIdStr + CHILD_FACILITY_SUFFIX + newRefLinkIdStr, TransitStopFacility.class);

		TransitStopFacility childStopToReplace = getChildStopInRoute(transitRoute, parentStopId);
		if(childStopToReplace == null) {
			return;
		}

		TransitStopFacility childStopReplaceWith = schedule.getFacilities().get(newChildStopId);
		if(childStopReplaceWith == null) {
			log.warn("StopFacility " + newChildStopId + " not found in schedule. Child facility is created");
			childStopReplaceWith = createStopFacility(newChildStopId, childStopToReplace.getCoord(), childStopToReplace.getName(), newRefLinkId);
			this.schedule.addStopFacility(childStopReplaceWith);
		}

		replaceStopFacilityInRoute(transitRoute, childStopToReplace, childStopReplaceWith);
	}

	private TransitStopFacility createStopFacility(Id<TransitStopFacility> facilityId, Coord coord, String name, Id<Link> linkId) {
		TransitStopFacility newTransitStopFacility = scheduleFactory.createTransitStopFacility(facilityId, coord, false);
		newTransitStopFacility.setName(name);
		newTransitStopFacility.setLinkId(linkId);
		return newTransitStopFacility;
	}


	/**
	 * Replaces a stop facility with another one in the given route. Both ids must exist.
	 * @param transitRoute
	 * @param toReplaceId
	 * @param replaceWithId
	 */
	public void replaceStopFacilityInRoute(TransitRoute transitRoute, Id<TransitStopFacility> toReplaceId, Id<TransitStopFacility> replaceWithId) {
		TransitStopFacility toReplace = schedule.getFacilities().get(toReplaceId);
		TransitStopFacility replaceWith = schedule.getFacilities().get(replaceWithId);

		if(toReplace == null) {
			log.warn("StopFacility " + toReplaceId + " not found in schedule!");
		} else if(replaceWith == null) {
			log.warn("StopFacility " + replaceWithId + " not found in schedule!");
		}
		replaceStopFacilityInRoute(transitRoute, toReplace, replaceWith);
	}

	public void replaceStopFacilityInRoute(TransitRoute transitRoute, TransitStopFacility toReplace, TransitStopFacility replaceWith) {
		TransitRouteStop routeStopToReplace = transitRoute.getStop(toReplace);
		if(routeStopToReplace != null) {
			routeStopToReplace.setStopFacility(replaceWith);
			refreshTransitRoute(transitRoute);
		} else {
			log.warn("StopFacility " + toReplace.getId() + " not found in TransitRoute " + transitRoute.getId());
		}
	}

	/**
	 * Replaces a stop facility with another one the whole schedule
	 * @param toReplaceId
	 * @param replaceWithId
	 */
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
	 * Gets all transit routes that ar on the given link
	 */
	public Set<TransitRoute> getTransitRoutesOnLink(Id<Link> linkId) {
		Set<TransitRoute> transitRoutesOnLink = new HashSet<>();
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(ScheduleTools.getLinkIds(transitRoute).contains(linkId)) {
					transitRoutesOnLink.add(transitRoute);
				}
			}
		}
		return transitRoutesOnLink;
	}

	/**
	 * "Refreshes" the transit route by routing between all referenced links
	 * of the stop facilities.
	 */
	public void refreshTransitRoute(TransitRoute transitRoute) {
		Router router = routers.get(transitRoute.getTransportMode());
		List<TransitRouteStop> routeStops = transitRoute.getStops();
		List<Id<Link>> linkSequence = new ArrayList<>();
		linkSequence.add(routeStops.get(0).getStopFacility().getLinkId());

		// route
		for(int i = 0; i < routeStops.size() - 1; i++) {
			if(routeStops.get(i).getStopFacility().getLinkId() == null) {
				throw new IllegalArgumentException("stop facility " + routeStops.get(i).getStopFacility().getName() + " (" + routeStops.get(i).getStopFacility().getId() + " not referenced!");
			}
			if(routeStops.get(i + 1).getStopFacility().getLinkId() == null) {
				throw new IllegalArgumentException("stop facility " + routeStops.get(i - 1).getStopFacility().getName() + " (" + routeStops.get(i + 1).getStopFacility().getId() + " not referenced!");
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