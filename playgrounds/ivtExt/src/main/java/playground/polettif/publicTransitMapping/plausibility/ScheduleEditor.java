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
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.publicTransitMapping.mapping.PTMapperUtils;
import playground.polettif.publicTransitMapping.mapping.router.ModeDependentRouter;
import playground.polettif.publicTransitMapping.mapping.router.Router;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static playground.polettif.publicTransitMapping.tools.ScheduleTools.getLinkIds;

/**
 * Provides tools for rerouting and adapting schedules via a csv "command file".
 *
 * @author polettif
 */
public class ScheduleEditor {

	protected static Logger log = Logger.getLogger(ScheduleEditor.class);

	// fields
	private final Network network;
	private final TransitSchedule schedule;
	private final TransitScheduleFactory scheduleFactory;
	private final Map<String, Router> routers;

	private final Map<String, TransitRoute> transitRoutes;
	private final ParentStops parentStops;

	private static final String SUFFIX_PATTERN = "[.]link:";
	private static final String SUFFIX = ".link:";


	// commands
	public static final String RR_VIA_LINK = "rerouteViaLink";
	public static final String RR_FROM_STOP = "rerouteFromStop";
	public static final String REPLACE_STOP_FACILITY = "replaceStopFacility";
	public static final String ALL_TRANSIT_ROUTES_ON_LINK = "allTransitRoutesOnLink";
	public static final String CHANGE_REF_LINK = "changeRefLink";
	public static final String COMMENT_START = "//";

	public ScheduleEditor(TransitSchedule schedule, Network network, Map<String, Router> routers) {
		this.schedule = schedule;
		this.network = network;
		this.scheduleFactory = schedule.getFactory();
		this.routers = routers;
		this.parentStops = new ParentStops();

		this.transitRoutes = new HashMap<>();
	}

	public ScheduleEditor(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.network = network;
		this.scheduleFactory = schedule.getFactory();
		this.transitRoutes = new HashMap<>();
		this.parentStops = new ParentStops();

		this.routers = new HashMap<>();
		log.info("Guessing routers based on schedule transport modes and used network transport modes.");
		Map<String, Set<String>> modeAssignments = new HashMap<>();
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				Set<String> usedNetworkModes = MapUtils.getSet(transitRoute.getTransportMode(), modeAssignments);
				List<Link> links = NetworkTools.getLinksFromIds(network, getLinkIds(transitRoute));
				for(Link link : links) {
					usedNetworkModes.addAll(link.getAllowedModes());
				}
			}
		}

		Map<Set<String>, Router> modeDependentRouters = new HashMap<>();
		for(Set<String> networkModes : modeAssignments.values()) {
			if(!modeDependentRouters.containsKey(networkModes)) {
				modeDependentRouters.put(networkModes, new ModeDependentRouter(network, networkModes));
			}
		}

		for(Map.Entry<String, Set<String>> e : modeAssignments.entrySet()) {
			routers.put(e.getKey(), modeDependentRouters.get(e.getValue()));
		}
	}


	public static void main(String[] args) throws IOException {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		new TransitScheduleReader(sc).readFile(args[0]);
		new MatsimNetworkReader(network).readFile(args[1]);
		TransitSchedule schedule = sc.getTransitSchedule();

		ScheduleEditor scheduleEditor = new ScheduleEditor(schedule, network);

		scheduleEditor.parseCommandCsv(args[2]);
		ScheduleTools.assignScheduleModesToLinks(schedule, network);
		scheduleEditor.writeFiles(args[3], args[4]);
	}

	private void writeFiles(String outputScheduleFile, String outputNetworkFile) {
		log.info("Writing schedule and network to file...");
		new TransitScheduleWriter(schedule).writeFile(outputScheduleFile);
		new NetworkWriter(network).write(outputNetworkFile);
	}

	/**
	 * Parses a command file (csv) and runs the commands specified
	 *
	 * @param filePath
	 * @throws IOException
	 */
	public void parseCommandCsv(String filePath) throws IOException {
		CSVReader reader = new CSVReader(new FileReader(filePath), ';');

		String[] line = reader.readNext();
		while(line != null) {
			runCmdLine(line);
			line = reader.readNext();
		}
		reader.close();
	}

	/**
	 * executes a command line
	 */
	private void runCmdLine(String[] cmd) {
		TransitRoute transitRoute;
		Set<TransitRoute> tmpTransitRoutes;

		/**
		 * Reroute TransitRoute via new Link
		 * ["rerouteViaLink"] [TransitLineId] [TransitRouteId] [oldLinkId] [newLinkId]
		 */
		if(RR_VIA_LINK.equals(cmd[0])) {
			rerouteViaLink(getTransitRoute(cmd[1], cmd[2]), cmd[3], cmd[3]);
		}

		/**
		 * Reroute TransitRoute from a given stop facility
		 * ["rerouteFromStop"] [TransitLineId] [TransitRouteId] [fromStopId] [newLinkId]
		 */
		else if(RR_FROM_STOP.equals(cmd[0])) {
			rerouteFromStop(getTransitRoute(cmd[1], cmd[2]), cmd[3], cmd[3]);
		}

		/**
		 * Changes the referenced link of a stopfacility. Effectively creates a new child stop facility.
		 * ["changeRefLink"] [StopFacilityId] [newlinkId]
		 * ["changeRefLink"] [TransitLineId] [TransitRouteId] [ParentId] [newlinkId]
		 * ["changeRefLink"] ["allTransitRoutesOnLink"] [linkId] [ParentId] [newlinkId]
		 */
		else if(CHANGE_REF_LINK.equals(cmd[0])) {
			if(cmd.length == 3) {
				changeRefLink(cmd[1], cmd[2]);
			} else if(cmd.length == 5) {
				switch (cmd[1]) {
					case ALL_TRANSIT_ROUTES_ON_LINK:
						tmpTransitRoutes = getTransitRoutesOnLink(Id.createLinkId(cmd[2]));
						for(TransitRoute tr : tmpTransitRoutes) {
							changeRefLink(tr, cmd[3], cmd[4]);
						}
						break;
					default:
						changeRefLink(getTransitRoute(cmd[1], cmd[2]), cmd[3], cmd[4]);
				}
			}
		}
		/**
		 * comment
		 */
		else if(COMMENT_START.equals(cmd[0].substring(0, 2))) {
			// comment
		} else {
			throw new IllegalArgumentException("Invalid command \"" + cmd[0] + "\"");
		}
	}

	/**
	 * @return the TransitRoute of the schedule based on transit line and transit route as strings
	 */
	private TransitRoute getTransitRoute(String transitLineStr, String transitRouteStr) {
		TransitLine transitLine = schedule.getTransitLines().get(Id.create(transitLineStr, TransitLine.class));

		if(transitLine == null) {
			throw new IllegalArgumentException("TransitLine " + transitLineStr + " not found!");
		}

		Id<TransitRoute> transitRouteId = Id.create(transitRouteStr, TransitRoute.class);
		if(!transitLine.getRoutes().containsKey(transitRouteId)) {
			throw new IllegalArgumentException("TransitRoute " + transitRouteStr + " not found in Transitline " + transitLineStr + "!");
		}

		return transitLine.getRoutes().get(transitRouteId);
	}

	/**
	 * Reroutes the section between two stops that passes the oldlink via the new link
	 *
	 * @param transitRoute
	 * @param oldLinkId
	 * @param newLinkId
	 */
	public void rerouteViaLink(TransitRoute transitRoute, Id<Link> oldLinkId, Id<Link> newLinkId) {
		List<TransitRouteStop> stopSequence = transitRoute.getStops();
		List<Id<Link>> linkSequence = transitRoute.getRoute().getLinkIds();

		List<Id<Link>> refLinkIds = stopSequence.stream().map(routeStop -> routeStop.getStopFacility().getLinkId()).collect(Collectors.toList());

		if(refLinkIds.contains(oldLinkId)) {
			throw new IllegalArgumentException("Link is referenced to a stop facility, rerouteViaLink cannot be performed. Use changeRefLink instead.");
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
	public void rerouteViaLink(TransitRoute transitRoute, String oldLinkId, String newLinkId) {
		rerouteViaLink(transitRoute, Id.createLinkId(oldLinkId), Id.createLinkId(newLinkId));
	}

	/**
	 * Reroutes the section after fromRouteStop via the given viaLinkId
	 *
	 * @param transitRoute
	 * @param fromRouteStop
	 * @param viaLinkId
	 */
	public void rerouteFromStop(TransitRoute transitRoute, TransitRouteStop fromRouteStop, Id<Link> viaLinkId) {
		Router router = routers.get(transitRoute.getTransportMode());

		List<TransitRouteStop> routeStops = transitRoute.getStops();
		TransitRouteStop toRouteStop = routeStops.get(routeStops.indexOf(fromRouteStop) + 1);

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
	private void rerouteFromStop(TransitRoute transitRoute, String fromStopFacilityId, String viaLinkId) {
		rerouteFromStop(transitRoute, getRouteStop(transitRoute, fromStopFacilityId), Id.createLinkId(viaLinkId));
	}


	/**
	 * @return the stop facility of a transit route that has the given parentId
	 */
	private TransitStopFacility getChildStopInRoute(TransitRoute transitRoute, String parentId) {
		for(TransitRouteStop routeStop : transitRoute.getStops()) {
			if(parentId.equals(getParentId(routeStop.getStopFacility()))) {
				return routeStop.getStopFacility();
			}
		}
		throw new IllegalArgumentException("No child facility for " + parentId + " found in Transit Route " + transitRoute + ".");
	}

	/**
	 * @return the stop facility of a transit route that has the given id
	 */
	private TransitStopFacility getStopFacilityInRoute(TransitRoute transitRoute, String stopFacilityId) {
		return getRouteStop(transitRoute, stopFacilityId).getStopFacility();
	}

	/**
	 * @return the TransitRouteStop with that contain the given stop facility
	 */
	private TransitRouteStop getRouteStop(TransitRoute transitRoute, String stopFacilityId) {
		for(TransitRouteStop routeStop : transitRoute.getStops()) {
			if(stopFacilityId.equals(routeStop.getStopFacility().getId().toString())) {
				return routeStop;
			}
		}
		throw new IllegalArgumentException("No child facility for " + stopFacilityId + " found in Transit Route " + transitRoute + ".");
	}


	private Id<TransitStopFacility> createChildStopFacilityId(String stopIdStr, String refLinkId) {
		return Id.create(getParentId(stopIdStr) + SUFFIX + refLinkId, TransitStopFacility.class);
	}

	private Id<TransitStopFacility> createStopFacilityId(String stopIdStr) {
		return Id.create(stopIdStr, TransitStopFacility.class);
	}

	/**
	 * Changes the reference of a stop facility (for all routes)
	 */
	private void changeRefLink(String stopIdStr, String newRefLinkIdStr) {
		TransitStopFacility oldStopFacility = schedule.getFacilities().get(Id.create(stopIdStr, TransitStopFacility.class));

		TransitStopFacility newChildStopFacility = parentStops.getChildStopFacility(getParentId(stopIdStr), newRefLinkIdStr);

		replaceStopFacilityInAllRoutes(oldStopFacility, newChildStopFacility);
	}

	/**
	 * changes the child stop in the given route
	 */
	private void changeRefLink(TransitRoute transitRoute, String childStopFacilityIdStr, String newRefLinkIdStr) {
		TransitStopFacility childStopToReplace = schedule.getFacilities().get(Id.create(childStopFacilityIdStr, TransitStopFacility.class));
		TransitStopFacility childStopReplaceWith = parentStops.getChildStopFacility(getParentId(childStopFacilityIdStr), newRefLinkIdStr);

		replaceStopFacilityInRoute(transitRoute, childStopToReplace, childStopReplaceWith);
	}

	/**
	 * creates a new stop facility and adds it to the schedule
	 */
	private TransitStopFacility createStopFacility(Id<TransitStopFacility> facilityId, Coord coord, String name, Id<Link> linkId) {
		TransitStopFacility newTransitStopFacility = scheduleFactory.createTransitStopFacility(facilityId, coord, false);
		newTransitStopFacility.setName(name);
		newTransitStopFacility.setLinkId(linkId);
		return newTransitStopFacility;
	}


	/**
	 * Replaces a stop facility with another one in the given route. Both ids must exist.
	 */
	public void replaceStopFacilityInRoute(TransitRoute transitRoute, Id<TransitStopFacility> toReplaceId, Id<TransitStopFacility> replaceWithId) {
		TransitStopFacility toReplace = schedule.getFacilities().get(toReplaceId);
		TransitStopFacility replaceWith = schedule.getFacilities().get(replaceWithId);

		if(toReplace == null) {
			throw new IllegalArgumentException("StopFacility " + toReplaceId + " not found in schedule!");
		} else if(replaceWith == null) {
			throw new IllegalArgumentException("StopFacility " + replaceWithId + " not found in schedule!");
		}
		replaceStopFacilityInRoute(transitRoute, toReplace, replaceWith);
	}

	/**
	 * Replaces a stop facility with another one in the given route. Both facilities must exist.
	 */
	public void replaceStopFacilityInRoute(TransitRoute transitRoute, TransitStopFacility toReplace, TransitStopFacility replaceWith) {
		TransitRouteStop routeStopToReplace = transitRoute.getStop(toReplace);
		if(routeStopToReplace != null) {
			routeStopToReplace.setStopFacility(replaceWith);
			refreshTransitRoute(transitRoute);
		} else {
			throw new IllegalArgumentException("StopFacility " + toReplace.getId() + " not found in TransitRoute " + transitRoute.getId());
		}
	}

	/**
	 * Replaces a stop facility with another one the whole schedule. Both must exist.
	 */
	public void replaceStopFacilityInAllRoutes(TransitStopFacility toReplace, TransitStopFacility replaceWith) {
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

	/**
	 * Refreshes the whole schedule by routing all transit routes.
	 */
	public void refreshSchedule() {
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			transitLine.getRoutes().values().forEach(this::refreshTransitRoute);
		}
	}

	private String getParentId(String stopFacilityId) {
		String[] childStopSplit = stopFacilityId.split(SUFFIX_PATTERN);
		return childStopSplit[0];
	}

	private String getParentId(TransitStopFacility stopFacility) {
		return getParentId(stopFacility.getId().toString());
	}


	/**
	 * Container class for all parent stop facilities
	 */
	private class ParentStops {

		Map<String, ParentStopFacility> fac = new HashMap<>();

		public ParentStops() {
			for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
				String parentId = getParentId(stopFacility);
				if(!fac.containsKey(getParentId(stopFacility))) {
					fac.put(parentId, new ParentStopFacility(stopFacility));
				} else {
					fac.get(parentId).getChildStopFacility(stopFacility);
				}
			}
		}

		private ParentStopFacility get(String parentId) {
			return fac.get(parentId);
		}

		private TransitStopFacility getChildStopFacility(String parentId, String newRefLinkIdStr) {
			return fac.get(parentId).getChildStopFacility(newRefLinkIdStr);
		}
	}

	/**
	 * Container class for parent a stop facility (are most likely
	 * not actual facilities in the schedule)
	 */
	private class ParentStopFacility {
		String id;
		String name;
		Coord coord;

		Map<Id<Link>, TransitStopFacility> children = new HashMap<>();

		public ParentStopFacility(String id, String name, Coord coord) {
			this.id = id;
			this.name = name;
			this.coord = coord;
		}

		public ParentStopFacility(TransitStopFacility childStopFacility) {
			this.id = getParentId(childStopFacility);
			this.name = childStopFacility.getName();
			this.coord = childStopFacility.getCoord();

			children.put(childStopFacility.getLinkId(), childStopFacility);
		}

		public void getChildStopFacility(TransitStopFacility childStopFacility) {
			children.put(childStopFacility.getLinkId(), childStopFacility);
		}

		/**
		 * Adds a child stop facility for the given refLink, creates
		 * a new one if needed.
		 * @param refLinkId
		 * @return the childStopFacility
		 */
		public TransitStopFacility getChildStopFacility(Id<Link> refLinkId) {
			Id<TransitStopFacility> newChildStopId = createChildStopFacilityId(id, refLinkId.toString());
			TransitStopFacility newChildStopFacilty = schedule.getFacilities().get(newChildStopId);
			if(newChildStopFacilty == null) {
				newChildStopFacilty = createStopFacility(newChildStopId, this.coord, this.name, refLinkId);
				newChildStopFacilty.setLinkId(refLinkId);
				newChildStopFacilty.setStopPostAreaId(this.id);
				schedule.addStopFacility(newChildStopFacilty);
			}
			children.put(newChildStopFacilty.getLinkId(), newChildStopFacilty);
			return newChildStopFacilty;
		}

		public TransitStopFacility getChildStopFacility(String newRefLinkIdStr) {
			return getChildStopFacility(Id.createLinkId(newRefLinkIdStr));
		}
	}
}
