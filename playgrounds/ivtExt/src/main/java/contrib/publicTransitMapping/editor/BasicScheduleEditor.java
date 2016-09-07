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

package contrib.publicTransitMapping.editor;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.transitSchedule.api.*;
import contrib.publicTransitMapping.config.PublicTransitMappingStrings;
import contrib.publicTransitMapping.mapping.PTMapperUtils;
import contrib.publicTransitMapping.mapping.networkRouter.Router;
import contrib.publicTransitMapping.tools.NetworkTools;
import contrib.publicTransitMapping.tools.ScheduleTools;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implemenation of a schedule editor. Provides methods for
 * rerouting and adapting schedules via a csv "command file".
 *
 * @author polettif
 */
public class BasicScheduleEditor implements ScheduleEditor {

	protected static Logger log = Logger.getLogger(RunScheduleEditor.class);

	// fields
	private final Network network;
	private final TransitSchedule schedule;
	private final TransitScheduleFactory scheduleFactory;
	private final NetworkFactory networkFactory;

	private final Map<String, Router> routers;

	private final ParentStops parentStops;

	private static final String SUFFIX_PATTERN = PublicTransitMappingStrings.SUFFIX_CHILD_STOP_FACILITIES_REGEX;
	private static final String SUFFIX = PublicTransitMappingStrings.SUFFIX_CHILD_STOP_FACILITIES;

	// commands
	public static final String RR_VIA_LINK = "rerouteViaLink";
	public static final String RR_FROM_STOP = "rerouteFromStop";
	public static final String REPLACE_STOP_FACILITY = "replaceStopFacility";
	public static final String REFRESH_TRANSIT_ROUTE = "refreshTransitRoute";
	public static final String ALL_TRANSIT_ROUTES_ON_LINK = "allTransitRoutesOnLink";
	public static final String CHANGE_REF_LINK = "changeRefLink";
	public static final String ADD_LINK = "addLink";

	public static final String COMMENT_START = "//";

	public BasicScheduleEditor(TransitSchedule schedule, Network network, Map<String, Router> routers) {
		this.schedule = schedule;
		this.network = network;
		this.scheduleFactory = schedule.getFactory();
		this.networkFactory = network.getFactory();
		this.routers = routers;
		this.parentStops = new ParentStops();
	}


	public BasicScheduleEditor(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.network = network;
		this.scheduleFactory = schedule.getFactory();
		this.networkFactory = network.getFactory();
		this.parentStops = new ParentStops();

		log.info("Guessing routers based on schedule transport modes and used network transport modes.");
		this.routers = NetworkTools.guessRouters(schedule, network);
	}

	public Network getNetwork() {
		return network;
	}

	public TransitSchedule getSchedule() {
		return schedule;
	}

	/**
	 * Parses a command file (csv) and runs the commands specified
	 *
	 * @throws IOException
	 */
	public void parseCommandCsv(String filePath) throws IOException {
		CSVReader reader = new CSVReader(new FileReader(filePath), ';');

		String[] line = reader.readNext();
		while(line != null) {
			log.info(CollectionUtils.arrayToString(line));
			executeCmdLine(line);
			line = reader.readNext();
		}
		reader.close();
	}

	/**
	 * executes a command line
	 */
	public void executeCmdLine(String[] cmd) {
		/**
		 * Reroute TransitRoute via new Link
		 * ["rerouteViaLink"] [TransitLineId] [TransitRouteId] [oldLinkId] [newLinkId]
		 */
		if(RR_VIA_LINK.equals(cmd[0])) {
			if(cmd.length == 5) {
				rerouteViaLink(getTransitRoute(cmd[1], cmd[2]), cmd[3], cmd[4]);
			} else {
				throw new IllegalArgumentException("Incorrect number of arguments for " + cmd[0] + "! 5 needed, " + cmd.length + " given");
			}
		}

		/**
		 * Reroute TransitRoute from a given stop facility
		 * ["rerouteFromStop"] [TransitLineId] [TransitRouteId] [fromStopId] [newLinkId]
		 */
		else if(RR_FROM_STOP.equals(cmd[0])) {
			if(cmd.length == 5) {
				rerouteFromStop(getTransitRoute(cmd[1], cmd[2]), cmd[3], cmd[4]);
			} else {
				throw new IllegalArgumentException("Incorrect number of arguments for " + cmd[0] + "! 5 needed, " + cmd.length + " given");
			}
		}
		/**
		 * Changes the referenced link of a stopfacility. Effectively creates a new child stop facility.
		 * ["changeRefLink"] [StopFacilityId] [newlinkId]
		 * ["changeRefLink"] [TransitLineId] [TransitRouteId] [ParentId] [newlinkId]
		 * ["changeRefLink"] ["allTransitRoutesOnLink"] [linkId] [ParentId] [newlinkId]
		 */
		else if(CHANGE_REF_LINK.equals(cmd[0])) {
			if("".equals(cmd[3])) {
				changeRefLink(cmd[1], cmd[2]);
			} else if(cmd.length == 5) {
				switch (cmd[1]) {
					case ALL_TRANSIT_ROUTES_ON_LINK:
						Set<TransitRoute> tmpTransitRoutes = getTransitRoutesOnLink(Id.createLinkId(cmd[2]));
						for(TransitRoute tr : tmpTransitRoutes) {
							changeRefLink(tr, cmd[3], cmd[4]);
						}
						break;
					default:
						changeRefLink(getTransitRoute(cmd[1], cmd[2]), cmd[3], cmd[4]);
				}
			} else {
				throw new IllegalArgumentException("Incorrect number of arguments for " + cmd[0] + "! 3 or 5 needed, " + cmd.length + " given");
			}
		}

		/**
		 * Adds a link to the network. Uses the attributes (freespeed, nr of lanes, transportModes)
		 * of the attributeLink.
		 * [addLink] [linkId] [fromNodeId] [toNodeId] [attributeLinkId]
		 */
		 else if(ADD_LINK.equals(cmd[0])) {
			if(cmd.length == 5) {
				addLink(cmd[1], cmd[2], cmd[3], cmd[4]);
				refreshSchedule();
			} else {
				throw new IllegalArgumentException("Incorrect number of arguments for " + cmd[0] + "! 5 needed, " + cmd.length + " given");
			}
		}

		/**
		 * Refreshes the given transit route (reroute all paths between referenced stop facility links)
		 * [refreshTransitRoute] [transitLineId] [transitRouteId]
		 */
		else if(REFRESH_TRANSIT_ROUTE.equals(cmd[0])) {
			if(cmd.length >= 3) {
				refreshTransitRoute(getTransitRoute(cmd[1], cmd[2]));
			} else {
				throw new IllegalArgumentException("Incorrect number of arguments for " + cmd[0] + "! 3 needed, " + cmd.length + " given");
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
	 * @param transitRoute the transit route
	 * @param oldLinkId the section between two route stops where this link appears is rerouted
	 * @param newLinkId the section is routed via this link
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
	 *
	 * @param transitRoute  the transit route
	 * @param fromRouteStop the section of the route from this routeStop to the subsequent
	 *                      routeStop is rerouted
	 * @param viaLinkId		the section is routed via this link
	 */
	public void rerouteFromStop(TransitRoute transitRoute, TransitRouteStop fromRouteStop, Id<Link> viaLinkId) {
		Router router = routers.get(transitRoute.getTransportMode());

		List<TransitRouteStop> routeStops = transitRoute.getStops();
		TransitRouteStop toRouteStop = routeStops.get(routeStops.indexOf(fromRouteStop) + 1);

		Id<Link> cutFromLinkId = fromRouteStop.getStopFacility().getLinkId();
		Link cutFromLink = network.getLinks().get(cutFromLinkId);
		Id<Link> cutToLinkId = toRouteStop.getStopFacility().getLinkId();
		Link cutToLink = network.getLinks().get(cutToLinkId);
		Link viaLink = network.getLinks().get(viaLinkId);

		NetworkRoute routeBeforeCut = transitRoute.getRoute().getSubRoute(transitRoute.getRoute().getStartLinkId(), cutFromLinkId);
		NetworkRoute routeAfterCut = transitRoute.getRoute().getSubRoute(cutToLinkId, transitRoute.getRoute().getEndLinkId());

		LeastCostPathCalculator.Path path1 = router.calcLeastCostPath(cutFromLink.getToNode(), viaLink.getFromNode());
		LeastCostPathCalculator.Path path2 = router.calcLeastCostPath(viaLink.getToNode(), cutToLink.getFromNode());

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
	public TransitRouteStop getRouteStop(TransitRoute transitRoute, Id<TransitStopFacility> stopFacilityId) {
		for(TransitRouteStop routeStop : transitRoute.getStops()) {
			if(stopFacilityId.equals(routeStop.getStopFacility().getId())) {
				return routeStop;
			}
		}
		throw new IllegalArgumentException("No child facility for " + stopFacilityId + " found in Transit Route " + transitRoute + ".");
	}
	private TransitRouteStop getRouteStop(TransitRoute transitRoute, String stopFacilityIdStr) {
		return getRouteStop(transitRoute, createStopFacilityId(stopFacilityIdStr));
	}


	/**
	 * Creates a standard child facility id
	 */
	private Id<TransitStopFacility> createChildStopFacilityId(String stopIdStr, String refLinkId) {
		return Id.create(getParentId(stopIdStr) + SUFFIX + refLinkId, TransitStopFacility.class);
	}

	/**
	 * Shortcut to create a stop facility id
	 */
	private Id<TransitStopFacility> createStopFacilityId(String stopFacilityIdStr) {
		return Id.create(stopFacilityIdStr, TransitStopFacility.class);
	}

	/**
	 * Changes the reference of a stop facility (for all routes)
	 */
	public void changeRefLink(Id<TransitStopFacility> stopFacilityId, Id<Link> newRefLinkId) {
		TransitStopFacility oldStopFacility = schedule.getFacilities().get(stopFacilityId);
		TransitStopFacility newChildStopFacility = parentStops.getChildStopFacility(getParentId(stopFacilityId), newRefLinkId.toString());
		replaceStopFacilityInAllRoutes(oldStopFacility, newChildStopFacility);
	}

	private void changeRefLink(String stopFacilityIdStr, String newRefLinkIdStr) {
		TransitStopFacility oldStopFacility = schedule.getFacilities().get(createStopFacilityId(stopFacilityIdStr));
		TransitStopFacility newChildStopFacility = parentStops.getChildStopFacility(getParentId(stopFacilityIdStr), newRefLinkIdStr);
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
	public TransitStopFacility createStopFacility(Id<TransitStopFacility> facilityId, Coord coord, String name, Id<Link> linkId) {
		TransitStopFacility newTransitStopFacility = scheduleFactory.createTransitStopFacility(facilityId, coord, false);
		newTransitStopFacility.setName(name);
		newTransitStopFacility.setLinkId(linkId);
		return newTransitStopFacility;
	}

	/**
	 * Adds a link to the network. Uses the attributes (freespeed, nr of lanes, transportModes)
	 * of the attributeLink.
	 */
	public void addLink(Id<Link> newLinkId, Id<Node> fromNodeId, Id<Node> toNodeId, Id<Link> attributeLinkId) {
		Node fromNode = network.getNodes().get(fromNodeId);
		Node toNode = network.getNodes().get(toNodeId);

		Link newLink = networkFactory.createLink(newLinkId, fromNode, toNode);

		if(attributeLinkId != null) {
			Link attributeLink = network.getLinks().get(attributeLinkId);

			newLink.setAllowedModes(attributeLink.getAllowedModes());
			newLink.setCapacity(attributeLink.getCapacity());
			newLink.setFreespeed(attributeLink.getFreespeed());
			newLink.setNumberOfLanes(attributeLink.getNumberOfLanes());
		}

		network.addLink(newLink);
	}
	private void addLink(String newLinkIdStr, String fromNodeIdStr, String toNodeIdStr, String attributeLinkIdStr) {
		addLink(Id.createLinkId(newLinkIdStr), Id.createNodeId(fromNodeIdStr), Id.createNodeId(toNodeIdStr), Id.createLinkId(attributeLinkIdStr));
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
				if(ScheduleTools.getTransitRouteLinkIds(transitRoute).contains(linkId)) {
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

	private String getParentId(String stopFacilityIdStr) {
		String[] childStopSplit = stopFacilityIdStr.split(SUFFIX_PATTERN);
		return childStopSplit[0];
	}

	private String getParentId(TransitStopFacility stopFacility) {
		return getParentId(stopFacility.getId().toString());
	}

	private String getParentId(Id<TransitStopFacility> stopFacility) {
		return getParentId(stopFacility.toString());
	}

	/**
	 * Container class for all parent stop facilities
	 */
	private class ParentStops {

		final Map<String, ParentStopFacility> fac = new HashMap<>();

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

		private TransitStopFacility getChildStopFacility(String parentIdStr, String newRefLinkIdStr) {
			return fac.get(parentIdStr).getChildStopFacility(newRefLinkIdStr);
		}

		private TransitStopFacility getChildStopFacility(Id<TransitStopFacility> parentId, Id<Link> newRefLinkId) {
			return fac.get(parentId.toString()).getChildStopFacility(newRefLinkId);
		}
	}

	/**
	 * Container class for parent a stop facility (are most likely
	 * not actual facilities in the schedule)
	 */
	private class ParentStopFacility {
		final String id;
		final String name;
		final Coord coord;

		final Map<Id<Link>, TransitStopFacility> children = new HashMap<>();

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
		 * @param refLinkId the id of the ref link
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
