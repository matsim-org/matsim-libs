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

package playground.polettif.multiModalMap.mapping;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRFastAStarLandmarksSimpleRouting;
import playground.polettif.boescpa.converters.osm.ptMapping.PTLRouter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assings a network link to ALL stops, splits links if necessary
 *
 * @author polettif
 */
public class PTMapperV1 extends PTMapper {

	// TODO ensure coordinate system is not WGS84 since this is not suitable for coordinate calculations

	// params TODO move to a config?
	private final static double SEARCH_RADIUS = 500; //[m] 150
	private final static double MAX_FACILITY_NODE_DISTANCE = 50;
	private final static boolean CONNECTION_TO_ALL_LINKS_WITHIN_SEARCH_AREA = false;

	private final static String PREFIX_SPLIT_LINKS = "split_";
	private final static String PREFIX_ARTIFICIAL_LINKS = "pt_";

	private final Map<Id<TransitStopFacility>, List<Id<TransitStopFacility>>> linkedStopFacilitiesTree = new HashMap<>();
	private final Set<Id<TransitStopFacility>> linkedStopFacilities = new HashSet<>();
	private final Map<Id<TransitStopFacility>, ArtificiallyConnectedStopFacility> artificiallyConnectedStopFacilities = new HashMap<>();
	private final Map<Tuple<Node, Node>, Link> artificiallyAddedLinks = new HashMap<>();
	private final Set<String> transitModes = Collections.singleton(TransportMode.pt);
	private PTLRouter router = null;
	private static int linkIdCounter = 0;

	public PTMapperV1(TransitSchedule schedule) {
		super(schedule);
	}

	public PTMapperV1(TransitSchedule schedule, Network network) {
		super(schedule, network);
	}

	@Override
	public void routePTLines(Network network) {
		setNetwork(network);

		// TODO
		/* ONLY BUS NOT TRAM!
		*
		*
		*
		*
		*
		*
		 */

		log.info("Creating PT lines...");
		linkStationsToNetwork();
		new TransitScheduleWriter(schedule).writeFile("C:/Users/polettif/Desktop/output/test/schedule_stopFacilitiesLinked.xml");
		new NetworkWriter(network).write("C:/Users/polettif/Desktop/output/test/network_stopFacilitiesLinked.xml");

		createPTRoutes();
		cleanStationsAndNetwork();
		log.info("Creating PT lines... done.");
	}

	/**
	 * Link the pt-stations in the schedule to the closest network links.
	 * Thereby modifies this.schedule.
	 */
	protected void linkStationsToNetwork() {
		StopFacilityLinker stopFacilityLinker = new StopFacilityLinker(network, PREFIX_SPLIT_LINKS);

		log.info("Linking pt stations to network...");

		Counter counter = new Counter("route # ");
		Set<TransitStopFacility> newFacilities = new HashSet<>();
		for (TransitStopFacility facility : this.schedule.getFacilities().values()) {

			final Link closestLink = Tools.findClosestLink(network, facility.getCoord(), SEARCH_RADIUS);
			if (closestLink != null) {
				Id<Link> refLinkId = closestLink.getId();

				// check first if closest link nodes are too far from stopFacility, if true split link
				// TODO include MAX_FACILITY_LINK_DISTANCE
				if(CoordUtils.calcEuclideanDistance(facility.getCoord(), closestLink.getFromNode().getCoord()) > MAX_FACILITY_NODE_DISTANCE) {
					stopFacilityLinker.split(closestLink, facility.getCoord());
					this.network = stopFacilityLinker.getNetwork();
					log.debug("link " + refLinkId + " split up for stopFacility \"" + facility.getName()+"\"");

					refLinkId = stopFacilityLinker.getNewLinkId();
				}

				//	reference current stopfacilitiy to link
				facility.setLinkId(refLinkId);

				List<Id<TransitStopFacility>> localLinkedFacilities = new ArrayList<>();
				localLinkedFacilities.add(facility.getId());

/*
				//	if street-link has opposite direction or if we searched for all links in area, then split stop-position before linking.
				final List<Id<Link>> oppositeDirectionLinks = getOppositeDirection(closestLink);
				if (oppositeDirectionLinks != null && !oppositeDirectionLinks.isEmpty()) {
					TransitStopFacility[] newStopFacilities = multiplyStop(facility, oppositeDirectionLinks.size());
					for (int i = 0; i < oppositeDirectionLinks.size(); i++) {
						newStopFacilities[i + 1].setLinkId(oppositeDirectionLinks.get(i));
						localLinkedFacilities.add(newStopFacilities[i + 1].getId());
						newFacilities.add(newStopFacilities[i + 1]);
					}
				}
*/
				linkedStopFacilitiesTree.put(facility.getId(), localLinkedFacilities);
			}

			counter.incCounter();
		}

		// relinking facilities if newly created links are closer then the previous link
		for(TransitStopFacility facility : this.schedule.getFacilities().values()) {
			final Link closestLink = Tools.findClosestLink(network, facility.getCoord(), SEARCH_RADIUS);
			if(closestLink != null && !facility.getLinkId().equals(closestLink.getId())) {
				log.debug(facility.getName()+" refLink changed from "+facility.getLinkId()+" to "+closestLink.getId());
				facility.setLinkId(closestLink.getId());
			}
		}


		// add newly created stop facilities to schedule TODO use newFacilities?
		newFacilities.forEach(this.schedule::addStopFacility);

		counter.printCounter();

		log.info("Linking pt stations to network... done.");

		for (List<Id<TransitStopFacility>> facilityList : linkedStopFacilitiesTree.values()) {
			linkedStopFacilities.addAll(facilityList.stream().collect(Collectors.toList()));
		}
	}

	/**
	 * Create and add to schedule new stops with the id-endings "_1" to "_numberOfCopies"
	 * by copying the provided stopFacility.
	 *
	 * @param stopFacility which will be copied. The original becomes the first element of the returned array.
	 * @param numberOfCopies of stopFacility which are created.
	 * @return TransitStopFacilities stopFacility, and copies stop_1 to stop_numberOfCopies
	 */
	private TransitStopFacility[] multiplyStop(TransitStopFacility stopFacility, int numberOfCopies) {
		TransitStopFacility[] facilities = new TransitStopFacility[numberOfCopies + 1];
		facilities[0] = stopFacility;
		for (int i = 1; i <= numberOfCopies; i++) {
			// Copy facility at stopFacility:
			Id<TransitStopFacility> idNewFacility = Id.create(stopFacility.getId().toString() + "_" + i, TransitStopFacility.class);
			TransitStopFacility newFacility = this.scheduleFactory.createTransitStopFacility(
					idNewFacility, stopFacility.getCoord(), stopFacility.getIsBlockingLane()
			);
			newFacility.setName(stopFacility.getName());
			// Add new facility to schedule and to array:
			facilities[i] = newFacility;
		}
		return facilities;
	}

	/**
	 * Within search radius search for the closest link that has the current mode as allowed travel mode
	 * and return this link.
	 *
	 * @param stopFacility Stop facility to search a link for.
	 * @return Null if no such link could be found.
	 */
	@Deprecated
	private Link findClosestLinkViaClosestNode(TransitStopFacility stopFacility, double searchRadius) {
		Link nearestLink = NetworkUtils.getNearestLink(this.network, stopFacility.getCoord());
		if (NetworkUtils.getEuclideanDistance(stopFacility.getCoord(), nearestLink.getToNode().getCoord()) <= searchRadius) {
			// If nearest link is within search radius, return it.
			return nearestLink;
		} else {
			return null;
		}
	}

	/**
	 * Looks in the network if the link has an opposite direction link and if so returns it. (changed: now independent
	 * of ids
	 *
	 * @param link for which opposite direction links are searched.
	 * @return List with the found links (resp. their Ids)...
	 */
	@Deprecated
	private List<Id<Link>> getOppositeDirection(Link link) {
		if (link == null) {
			return null;
		}

		Id<Link> linkId = link.getId();

		List<Id<Link>> oppositeDirectionLinks = new ArrayList<>();

		// If we find one with "opposite" direction, we return only this.
		Link lowerLink = this.network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) - 1));
		Link upperLink = this.network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) + 1));
		if (lowerLink != null
				&& lowerLink.getFromNode().getId().equals(link.getToNode().getId())
				&& lowerLink.getToNode().getId().equals(link.getFromNode().getId())) {
			oppositeDirectionLinks.add(lowerLink.getId());
		}
		if (oppositeDirectionLinks.isEmpty() && upperLink != null
				&& upperLink.getFromNode().getId().equals(link.getToNode().getId())
				&& upperLink.getToNode().getId().equals(link.getFromNode().getId())) {
			oppositeDirectionLinks.add(upperLink.getId());
		}

		if (CONNECTION_TO_ALL_LINKS_WITHIN_SEARCH_AREA) {
			Set<Link> linksWithinRadius = getLinksWithinSearchRadius(link.getCoord());
			if (linksWithinRadius != null) {
				for (Link presentLink : linksWithinRadius) {
					if (!presentLink.getId().toString().equals(link.getId().toString())
							&& oppositeDirectionLinks.contains(presentLink.getId())) {
						oppositeDirectionLinks.add(presentLink.getId());
					}
				}
			}
		}

		return oppositeDirectionLinks;

	}

	@Deprecated
	private Set<Link> getLinksWithinSearchRadius(Coord centralCoords) {
		Set<Link> linksWithinRadius = new HashSet<>();
		for (Link link : this.network.getLinks().values()) {
			if (NetworkUtils.getEuclideanDistance(centralCoords, link.getToNode().getCoord()) < SEARCH_RADIUS) {
				linksWithinRadius.add(link);
			}
		}
		return linksWithinRadius;
	}


	/**
	 * By applying a routing algorithm (e.g. shortest path or OSM-extraction) route from station to
	 * station for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 */
	// TODO
	protected void createPTRoutes() {
		log.info("Creating pt routes...");

		Counter counter = new Counter("route # ");
		this.router = new PTLRFastAStarLandmarksSimpleRouting(this.network); // TODO param for routing algorithm
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				counter.incCounter();
				assignRoute(route);
			}
		}
		counter.printCounter();

		log.info("  Add artificial links and nodes...");
		for (ArtificiallyConnectedStopFacility newFacility : artificiallyConnectedStopFacilities.values()) {
			this.network.addNode(newFacility.myNode);
		}
		for (ArtificiallyConnectedStopFacility newFacility : artificiallyConnectedStopFacilities.values()) {
			this.network.addLink(newFacility.myLink);
			for (Link newLink : newFacility.getLinks()) {
				this.network.addLink(newLink);
			}
		}
		for (Link newLink : artificiallyAddedLinks.values()) {
			this.network.addLink(newLink);
		}
		log.info("  Add artificial links and nodes... done.");

		log.info("Creating pt routes... done.");
	}

	private void assignRoute(TransitRoute route) {

		List<Id<Link>> linkSequence = new ArrayList<>();
		int i = 0;
		while (i < (route.getStops().size())) {
			TransitRouteStop presentStop = route.getStops().get(i);
			TransitRouteStop nextStop = (i < route.getStops().size()-1) ? route.getStops().get(i + 1) : null;

			if (nextStop != null) {

				// 	Case 1: For both stops a link assigned, then just route between the two.
				if (linkedStopFacilities.contains(presentStop.getStopFacility().getId())
						&& linkedStopFacilities.contains(nextStop.getStopFacility().getId())) {
					linkSequence.add(presentStop.getStopFacility().getLinkId());

					Node startNode =null;
					Node endNode=null;

					if(presentStop.getStopFacility() != null && presentStop.getStopFacility().getLinkId() != null && network.getLinks().get(presentStop.getStopFacility().getLinkId()) != null) {
						startNode = this.network.getLinks().get(presentStop.getStopFacility().getLinkId()).getToNode();
					}

					if(nextStop.getStopFacility() != null && nextStop.getStopFacility().getLinkId() != null && network.getLinks().get(nextStop.getStopFacility().getLinkId()) != null) {
						endNode = this.network.getLinks().get(nextStop.getStopFacility().getLinkId()).getFromNode();
					}
					else {
						Id<Link> stopLinkId = nextStop.getStopFacility().getLinkId();
						Map<Id<Link>, ? extends Link> nlinks = network.getLinks();
					}

//					LeastCostPathCalculator.Path path = getShortestPath(startNode, nextStop);
					LeastCostPathCalculator.Path path = this.router.calcLeastCostPath(startNode, endNode, null, null );

					if (path != null) {
						for (Link link : path.links) {
							linkSequence.add(link.getId());
						}
					} else {
						Node fromNode = network.getLinks().get(presentStop.getStopFacility().getLinkId()).getToNode();
						Node toNode = network.getLinks().get(nextStop.getStopFacility().getLinkId()).getFromNode();
						Link artificialLink = (artificiallyAddedLinks.containsKey(new Tuple<>(fromNode, toNode))) ?
								artificiallyAddedLinks.get(new Tuple<>(fromNode, toNode)) :
								createNewLink(fromNode, toNode);
						linkSequence.add(artificialLink.getId());
						artificiallyAddedLinks.put(new Tuple<>(fromNode, toNode), artificialLink);
					}

				// Case 2: PresentStop has no link, but NextStop has link then create link to closest network node and route between that node and link of follow-stop.
				} else if (!linkedStopFacilities.contains(presentStop.getStopFacility().getId())
						&& linkedStopFacilities.contains(nextStop.getStopFacility().getId())) {

					ArtificiallyConnectedStopFacility thisStopFacility = getArtificiallyConnectedStopFacility(presentStop.getStopFacility());
					Node toNode = network.getLinks().get(nextStop.getStopFacility().getLinkId()).getFromNode();

					linkSequence.add(thisStopFacility.myLink.getId());
					linkSequence.add(thisStopFacility.getLinkToNode(toNode).getId());

				// Case 3: PresentStop has link, but NextStop has no link then create link to closest network node for follow stop and then route between the two.
				} else if (linkedStopFacilities.contains(presentStop.getStopFacility().getId())
						&& !linkedStopFacilities.contains(nextStop.getStopFacility().getId())) {

					ArtificiallyConnectedStopFacility nextStopFacility = getArtificiallyConnectedStopFacility(nextStop.getStopFacility());
					Node fromNode = network.getLinks().get(presentStop.getStopFacility().getLinkId()).getToNode();

					linkSequence.add(presentStop.getStopFacility().getLinkId());
					linkSequence.add(nextStopFacility.getLinkFromNode(fromNode).getId());

				// Case 4: Neither PresentStop nor NextStop has link then standard link creation as with Marcel's network creator.
				} else {
					ArtificiallyConnectedStopFacility thisStopFacility = getArtificiallyConnectedStopFacility(presentStop.getStopFacility());
					ArtificiallyConnectedStopFacility nextStopFacility = getArtificiallyConnectedStopFacility(nextStop.getStopFacility());

					linkSequence.add(thisStopFacility.myLink.getId());
					linkSequence.add(nextStopFacility.getLinkFromNode(thisStopFacility.myNode).getId());
				}

			// If nextStop was null, this means we have reached the end of the route and just add the final link.
			} else {
				if (linkedStopFacilities.contains(presentStop.getStopFacility().getId())) {
					linkSequence.add(presentStop.getStopFacility().getLinkId());
				} else {
					ArtificiallyConnectedStopFacility thisStopFacility = getArtificiallyConnectedStopFacility(presentStop.getStopFacility());
					linkSequence.add(thisStopFacility.myLink.getId());
				}
			}

			i++;
		}
		if (linkSequence.size() > 0) {
			route.setRoute(RouteUtils.createNetworkRoute(linkSequence, this.network));
		} else {
			log.warn("No route found for transit route " + route.toString() + ". No route assigned.");
		}
	}

	private ArtificiallyConnectedStopFacility getArtificiallyConnectedStopFacility(TransitStopFacility facility) {
		if (!artificiallyConnectedStopFacilities.containsKey(facility.getId())) {
			artificiallyConnectedStopFacilities.put(facility.getId(),
					new ArtificiallyConnectedStopFacility(facility));
		}
		return artificiallyConnectedStopFacilities.get(facility.getId());
	}

	/**
	 * FromNodes -> thisNode -> ToNodes
	 */
	private class ArtificiallyConnectedStopFacility {

		final Link myLink;
		final Node myNode;
		private final Map<Node, Link> fromNodes = new HashMap<>();
		private final Map<Node, Link> toNodes = new HashMap<>();

		ArtificiallyConnectedStopFacility(TransitStopFacility facility) {
			myNode = networkFactory.createNode(Id.create(PREFIX_ARTIFICIAL_LINKS + facility.getId(), Node.class), facility.getCoord());
			myLink = createNewLink(myNode, myNode);
			facility.setLinkId(myLink.getId());
		}

		List<Link> getLinks() {
			List<Link> links = new ArrayList<>();
			links.addAll(fromNodes.values());
			links.addAll(toNodes.values());
			return links;
		}

		Link getLinkFromNode(Node fromNode) {
			if (!fromNodes.containsKey(fromNode)) {
				fromNodes.put(fromNode, createNewLink(fromNode, myNode));
			}
			return fromNodes.get(fromNode);
		}

		Link getLinkToNode (Node toNode) {
			if (!toNodes.containsKey(toNode)) {
				toNodes.put(toNode, createNewLink(myNode, toNode));
			}
			return toNodes.get(toNode);
		}
	}

	private Link createNewLink(Node fromNode, Node toNode) {
		Link link = networkFactory.createLink(Id.create(PREFIX_ARTIFICIAL_LINKS + linkIdCounter++, Link.class), fromNode, toNode);
		if (fromNode == toNode) {
			link.setLength(50);
		} else {
			link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
		}
		link.setFreespeed(80.0 / 3.6);
		link.setCapacity(10000);
		link.setNumberOfLanes(1);
		link.setAllowedModes(this.transitModes);
		return link;
	}


	private LeastCostPathCalculator.Path getShortestPath(Node startNode, TransitRouteStop toStop) {
		LeastCostPathCalculator.Path shortestPath = null;

		for (Id<TransitStopFacility> toStopFacilityId : linkedStopFacilitiesTree.get(toStop.getStopFacility().getId())) {
			TransitStopFacility facility = this.schedule.getFacilities().get(toStopFacilityId);
			Id<Link> linkId = facility.getLinkId();
			Link link = this.network.getLinks().get(linkId);
			Node endNode = link.getFromNode();
			LeastCostPathCalculator.Path tempShortestPath = this.router.calcLeastCostPath(startNode, endNode, "", "");
			if (tempShortestPath != null && (shortestPath == null || (tempShortestPath.travelCost < shortestPath.travelCost))) {
				shortestPath = tempShortestPath;
				toStop.setStopFacility(this.schedule.getFacilities().get(toStopFacilityId));
			}
		}

		return shortestPath;
	}

	/**
	 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
	 * and all nodes which are non-linked to any link after the above cleaning...
	 * Clean also the allowed modes for only the modes, no line-number any more...
	 */
	protected void cleanStationsAndNetwork() {
		log.info("Clean Stations and Network...");
		cleanSchedule();
		prepareNetwork();
		removeNonUsedStopFacilities();
		setConnectedStopFacilitiesToIsBlocking();
		log.info("Clean Stations and Network... done.");
	}

	private void setConnectedStopFacilitiesToIsBlocking() {
		Set<TransitStopFacility> facilitiesToExchange = new HashSet<>();
		for (TransitStopFacility oldFacility : this.schedule.getFacilities().values()) {
			if (this.network.getLinks().get(oldFacility.getLinkId()).getAllowedModes().contains(TransportMode.car)) {
				TransitStopFacility newFacility = this.scheduleFactory.createTransitStopFacility(
						oldFacility.getId(), oldFacility.getCoord(), true);
				newFacility.setName(oldFacility.getName());
				newFacility.setLinkId(oldFacility.getLinkId());
				newFacility.setStopPostAreaId(oldFacility.getStopPostAreaId());
				facilitiesToExchange.add(newFacility);
			}
		}
		for (TransitStopFacility facility : facilitiesToExchange) {
			TransitStopFacility facilityToRemove = this.schedule.getFacilities().get(facility.getId());
			this.schedule.removeStopFacility(facilityToRemove);
			this.schedule.addStopFacility(facility);
		}
	}

	private void cleanSchedule() {
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			Set<TransitRoute> toRemove = new HashSet<>();
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();
				if (networkRoute.getStartLinkId() == null || networkRoute.getEndLinkId() == null) {
					removeRoute = true;
				}
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					if (linkId == null) {
						removeRoute = true;
					}
				}
				if (removeRoute) {
					log.error("NetworkRoute for " + transitRoute.getId().toString() + " incomplete. Remove route.");
					toRemove.add(transitRoute);
				}
			}
			if (!toRemove.isEmpty()) {
				for (TransitRoute transitRoute : toRemove) {
					line.removeRoute(transitRoute);
				}
			}
		}
	}

	/**
	 * Add to any link that is passed by any route a "pt" in the modes, if it hasn't already one...
	 */
	private void prepareNetwork() {
		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinks = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();
				transitLinks.add(networkRoute.getStartLinkId());
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					transitLinks.add(linkId);
				}
				transitLinks.add(networkRoute.getEndLinkId());
			}
		}
		for (Id<Link> transitLinkId : transitLinks) {
			Link transitLink = networkLinks.get(transitLinkId);
			if (!transitLink.getAllowedModes().contains(TransportMode.pt)) {
				Set<String> modes = new HashSet<>();
				modes.addAll(transitLink.getAllowedModes());
				modes.add(TransportMode.pt);
				transitLink.setAllowedModes(modes);
			}
		}
	}

	private void removeNonUsedStopFacilities() {
		// Collect all used stop facilities:
		Set<Id<TransitStopFacility>> usedStopFacilities = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					usedStopFacilities.add(stop.getStopFacility().getId());
				}
			}
		}
		// Check all stop facilities if not used:
		Set<TransitStopFacility> unusedStopFacilites = new HashSet<>();
		for (Id<TransitStopFacility> facilityId : this.schedule.getFacilities().keySet()) {
			if (!usedStopFacilities.contains(facilityId)) {
				unusedStopFacilites.add(this.schedule.getFacilities().get(facilityId));
			}
		}
		// Remove all stop facilities not used:
		for (TransitStopFacility facility : unusedStopFacilites) {
			this.schedule.removeStopFacility(facility);
		}
	}


}
