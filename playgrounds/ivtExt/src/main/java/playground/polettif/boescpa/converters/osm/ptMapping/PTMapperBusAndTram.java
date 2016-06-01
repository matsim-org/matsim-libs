/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.polettif.boescpa.converters.osm.ptMapping;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

import static playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator.PtRouteFPLAN.BUS;
import static playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator.PtRouteFPLAN.TRAM;

/**
 * Default implementation of PTLinesCreator.
 *
 * @author boescpa
 */
public class PTMapperBusAndTram extends PTMapper {

	private final static double SEARCH_RADIUS_BUS = 50; //[m]
	private final static double SEARCH_RADIUS_TRAM = 50; //[m]

	private final Map<Id<TransitStopFacility>, Map<String, Id<TransitStopFacility>[]>> linkedStopFacilities = new HashMap<>();
	private PTLRouter router = null;
	private PseudoNetworkCreatorBusAndTram pseudoNetworkCreatorBusAndTram = null;

	public PTMapperBusAndTram(TransitSchedule schedule) {
		super(schedule);
	}

	public PTMapperBusAndTram(TransitSchedule schedule, Network network) {
		super(schedule, network);
	}

	private String mode;
	private String networkMode;
	private double searchRadius;
	private void setMode(String mode) {
		switch (mode) {
			case BUS: {
				this.mode = BUS;
				this.networkMode = "car";
				this.searchRadius = SEARCH_RADIUS_BUS;
				break;
			}
			case TRAM: {
				this.mode = TRAM;
				this.networkMode = "tram";
				this.searchRadius = SEARCH_RADIUS_TRAM;
				break;
			}
			default: {
				log.warn("Mode " + mode + " not available for network assignment.");
				this.mode = null;
				this.networkMode = null;
				this.searchRadius = 0;
			}
		}
	}

	@Override
	public void routePTLines(Network network) {
		setNetwork(network);

		log.info("Creating PT lines...");
		linkStationsToNetwork();
		createPTRoutes();
		cleanStationsAndNetwork();
		log.info("Creating PT lines... done.");
	}

	/**
	 * Link the pt-stations in the schedule to the closest network links.
	 * Writes the resulting schedule into this.schedule.
	 */
	protected void linkStationsToNetwork() {
		log.info("Linking pt stations to network...");

		Counter counter = new Counter("route # ");
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				counter.incCounter();
				if (route.getTransportMode().equals(BUS)) {
					setMode(BUS);
					linkRouteToNetwork(route);
				} else if (route.getTransportMode().equals(TRAM)) {
					setMode(TRAM);
					linkRouteToNetwork(route);
				} else {
					// Other modes (e.g. train or ship) are not linked to the network. An own network is created for them.
				}
			}
		}
		counter.printCounter();

		log.info("Linking pt stations to network... done.");
	}

	private void linkRouteToNetwork(TransitRoute route) {
		//	for each route in each line in schedule:
		for (TransitRouteStop currentStop : route.getStops()) {
			// if stop has not already a link attached to it:
			Map<String, Id<TransitStopFacility>[]> derivativesCurrentStop
					= this.linkedStopFacilities.get(currentStop.getStopFacility().getId());
			if (derivativesCurrentStop == null) {
				derivativesCurrentStop = new HashMap<>();
				this.linkedStopFacilities.put(currentStop.getStopFacility().getId(), derivativesCurrentStop);
			}
			Id<TransitStopFacility>[] modeSpecificDerivativesCurrentStop = derivativesCurrentStop.get(this.mode);
			if (modeSpecificDerivativesCurrentStop == null) {
				TransitStopFacility newStopFacility = createTypeDependentStopFacility(currentStop);

				//	find street-link closest to stop-position and, if available, opposite direction link.
				//	for trams get all tram-links within a given radius as "oppositeDirectionLinks".
				Id<Link> closestLink = findClosestLink(newStopFacility);
				Id<Link>[] oppositeDirectionLinks = getOppositeDirection(closestLink);

				//	link stop-position(s) to the respective link(s).
				if (oppositeDirectionLinks == null) {
					newStopFacility.setLinkId(closestLink);
					derivativesCurrentStop.put(this.mode, new Id[]{newStopFacility.getId()});
				} else {
					//	if street-link has opposite direction, then split stop-position before linking.
					TransitStopFacility[] newStopFacilities = multiplyStop(newStopFacility, oppositeDirectionLinks.length);
					Id<TransitStopFacility>[] newStopFacilityIds = new Id[newStopFacilities.length];
					newStopFacilities[0].setLinkId(closestLink);
					newStopFacilityIds[0] = newStopFacilities[0].getId();
					for (int i = 0; i < oppositeDirectionLinks.length; i++) {
						newStopFacilities[i + 1].setLinkId(oppositeDirectionLinks[i]);
						newStopFacilityIds[i + 1] = newStopFacilities[i + 1].getId();
					}
					derivativesCurrentStop.put(this.mode, newStopFacilityIds);
				}
			} else {
				// if a modeSpecificDerivativesCurrentStop already exists, we have the according stopFacility
				// already connected to the network and we might continue...
			}
		}
	}

	/**
	 * Create and add to schedule new stops with the id-endings "_0" to "_numberOfCopies"
	 * by copying the provided stop.
	 *
	 * @param stop which will be copied. The original remains unchanged.
	 * @param numberOfCopies of stop plus a zero-copy of the stop which are created.
	 * @return TransitStopFacilities stop_0 to stop_numberOfCopies
	 */
	private TransitStopFacility[] multiplyStop(TransitStopFacility stop, int numberOfCopies) {
		TransitStopFacility[] facilities = new TransitStopFacility[numberOfCopies + 1];
		for (int i = 0; i <= numberOfCopies; i++) {
			// Copy facility at stop:
			Id<TransitStopFacility> idNewFacility = Id.create(stop.getId().toString() + "_" + i, TransitStopFacility.class);
			TransitStopFacility newFacility = this.scheduleFactory.createTransitStopFacility(
					idNewFacility, stop.getCoord(), stop.getIsBlockingLane()
			);
			newFacility.setName(stop.getName());
			// Add new facility to schedule and to array:
			this.schedule.addStopFacility(newFacility);
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
	private Id<Link> findClosestLink(TransitStopFacility stopFacility) {
		Link nearestLink = NetworkUtils.getNearestLink(this.network, stopFacility.getCoord());
		if (nearestLink.getAllowedModes().contains(this.networkMode)
				&& NetworkUtils.getEuclideanDistance(stopFacility.getCoord(), nearestLink.getCoord()) <= this.searchRadius) {
			// If nearest link has right mode and is within search radius, return it.
			return nearestLink.getId();
		} else {
			// Search for the allowed link with the shortest distance:
			nearestLink = null;
			double currentRadius = this.searchRadius;
			for (Link potentialLink : this.network.getLinks().values()) {
				if (potentialLink.getAllowedModes().contains(this.networkMode)
						&& NetworkUtils.getEuclideanDistance(stopFacility.getCoord(), potentialLink.getCoord()) < currentRadius) {
					currentRadius = NetworkUtils.getEuclideanDistance(stopFacility.getCoord(), potentialLink.getCoord());
					nearestLink = potentialLink;
				}
			}
			if (nearestLink != null) {
				return nearestLink.getId();
			} else {
				return null;
			}
		}
	}

	/**
	 * Looks in the network if the link has an opposite direction link and if so returns it.
	 * In the case of non-street-modes (e.g. TRAM), it looks also geographically because
	 * stations might be large and the opposite direction link therefore further away. It then returns all
	 * such surronding links.
	 *
	 * @param linkId of the link for which opposite direction links are searched.
	 * @return Null if no other direction links could be found...
	 */
	private Id<Link>[] getOppositeDirection(Id<Link> linkId) {
		if (linkId == null) {
			return null;
		}

		// If we find one with "opposite" direction, we return only this.
		Link lowerLink = this.network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) - 1));
		Link link = this.network.getLinks().get(linkId);
		Link upperLink = this.network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) + 1));
		if (lowerLink != null
				&& lowerLink.getFromNode().getId().toString().equals(link.getToNode().getId().toString())
				&& lowerLink.getToNode().getId().toString().equals(link.getFromNode().getId().toString())
				&& lowerLink.getAllowedModes().contains(this.networkMode)) {
			return new Id[]{lowerLink.getId()};
		}
		if (upperLink != null
				&& upperLink.getFromNode().getId().toString().equals(link.getToNode().getId().toString())
				&& upperLink.getToNode().getId().toString().equals(link.getFromNode().getId().toString())
				&& upperLink.getAllowedModes().contains(this.networkMode)) {
			return new Id[]{upperLink.getId()};
		}

		// Else we return all links we find within search radius around link.
		Set<Link> linksWithinRadius = getLinksWithinSearchRadius(link.getCoord());
		if (linksWithinRadius != null) {
			Id<Link>[] links = new Id[linksWithinRadius.size()-1];
			int i = 0;
			for (Link presentLink : linksWithinRadius) {
				if (!presentLink.getId().toString().equals(link.getId().toString())) {
					links[i] = presentLink.getId();
					i++;
				}
			}
			return links;
		}

		// No opposite link is found, and not any in the surroundings, null is returned.
		return null;
	}

	private Set<Link> getLinksWithinSearchRadius(Coord centralCoords) {
		Set<Link> linksWithinRadius = new HashSet<>();
		for (Link link : this.network.getLinks().values()) {
			if (link.getAllowedModes().contains(this.networkMode)
					&& NetworkUtils.getEuclideanDistance(centralCoords, link.getCoord()) < this.searchRadius) {
				linksWithinRadius.add(link);
			}
		}
		return linksWithinRadius;
	}

	/**
	 * Add new TransitStopFacility to schedule and to the provided stop,
	 * which is a copy of the provided stop.getStopFacility() except for the type is now of vehicleType.
	 *
	 * @param stop which is dublicated with a mode dependent version.
	 * @return Type-dependent stop facility.
	 */
	private TransitStopFacility createTypeDependentStopFacility(TransitRouteStop stop) {
		// Copy facility at stop:
		Id<TransitStopFacility> idNewFacility
				= Id.create(stop.getStopFacility().getId().toString() + "_" + this.mode, TransitStopFacility.class);
		TransitStopFacility newFacility = this.scheduleFactory.createTransitStopFacility(
				idNewFacility, stop.getStopFacility().getCoord(), stop.getStopFacility().getIsBlockingLane()
		);
		newFacility.setName(stop.getStopFacility().getName());
		// Add new facility to schedule and to stop:
		this.schedule.addStopFacility(newFacility);
		// Return new facility:
		return newFacility;
	}

	/**
	 * By applying a routing algorithm (e.g. shortest path or OSM-extraction) route from station to
	 * station for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 */
	protected void createPTRoutes() {
		log.info("Creating pt routes...");

		Counter counter = new Counter("route # ");
		this.router = new PTLRFastAStarLandmarksWeighting(this.network);
		this.pseudoNetworkCreatorBusAndTram = new PseudoNetworkCreatorBusAndTram(this.schedule, this.network, "PseudoNetwork_");
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				counter.incCounter();
				if (route.getTransportMode().equals(BUS)) {
					setMode(BUS);
					routeLine(route);
				} else if (route.getTransportMode().equals(TRAM)) {
					setMode(TRAM);
					routeLine(route);
				} else {
					// Other modes (e.g. train or ship) are not linked to the network. An own network is created for them.
					this.pseudoNetworkCreatorBusAndTram.createLine(route);
				}
			}
		}
		counter.printCounter();

		log.info("Creating pt routes... done.");
	}

	private void routeLine(TransitRoute route) {
		Tuple<TransitRouteStop, TransitRouteStop> linkToAdd = null;
		List<Id<Link>> links = new ArrayList<>();
		int i = 0;
		while (i < (route.getStops().size()-1)) {
			TransitRouteStop fromStop = route.getStops().get(i);
			TransitRouteStop toStop = route.getStops().get(i+1);
			LeastCostPathCalculator.Path path = getShortestPath(fromStop, toStop, route.getId().toString());
			if (linkToAdd != null) {
				Id<Link> replacedLink = fromStop.getStopFacility().getLinkId();
				Link link = this.pseudoNetworkCreatorBusAndTram.getNetworkLink(linkToAdd.getFirst(), linkToAdd.getSecond());
				links.add(link.getId());
				links.add(replacedLink);
				linkToAdd = null;
			}
			if (path != null) {
				links.add(fromStop.getStopFacility().getLinkId());
				for (Link link : path.links) {
					links.add(link.getId());
				}
			} else {
				links.add(fromStop.getStopFacility().getLinkId());
				Link link = this.pseudoNetworkCreatorBusAndTram.getNetworkLink(fromStop, toStop);
				links.add(link.getId());
				i++;
				if (i < (route.getStops().size()-1)) {
					fromStop = route.getStops().get(i);
					toStop = route.getStops().get(i+1);
					linkToAdd = new Tuple<>(fromStop, toStop);
				}
			}
			i++;
		}
		if (links.size() > 0) {
			links.add(route.getStops().get(route.getStops().size() - 1).getStopFacility().getLinkId());
			route.setRoute(RouteUtils.createNetworkRoute(links, this.network));
		} else {
			log.warn("No route found for transit route " + route.toString() + ". No route assigned.");
		}
	}

	private LeastCostPathCalculator.Path getShortestPath(TransitRouteStop fromStop, TransitRouteStop toStop, String routeId) {
		LeastCostPathCalculator.Path shortestPath = null;

		Map<String, Id<TransitStopFacility>[]> derivativesFromStop
				= this.linkedStopFacilities.get(fromStop.getStopFacility().getId());
		Map<String, Id<TransitStopFacility>[]> derivativesToStop
				= this.linkedStopFacilities.get(toStop.getStopFacility().getId());
		// If derivatives is non-empty, this means that the stop wasn't assigned a mode-specific facility yet and ergo that we have to check all possible facilities. Else we just take the assigned facilities.

		if (derivativesFromStop != null) {
			for (Id<TransitStopFacility> fromStopFacilityId : derivativesFromStop.get(this.mode)) {
				Node fromNode = getNodeForStopFacility(fromStopFacilityId, true);
				if (derivativesToStop != null) {
					// We have to loop over both, fromStops and toStops, and have to set both as soon as found...
					for (Id<TransitStopFacility> toStopFacilityId : derivativesToStop.get(this.mode)) {
						Node toNode = getNodeForStopFacility(toStopFacilityId, false);
						LeastCostPathCalculator.Path tempShortestPath = this.router.calcLeastCostPath(fromNode, toNode, this.mode, routeId);
						if (tempShortestPath != null && (shortestPath == null || (tempShortestPath.travelCost < shortestPath.travelCost))) {
							shortestPath = tempShortestPath;
							fromStop.setStopFacility(this.schedule.getFacilities().get(fromStopFacilityId));
							toStop.setStopFacility(this.schedule.getFacilities().get(toStopFacilityId));
						}
					}
				} else {
					// We have to loop over fromStops and assign it, but not over toStop. (This should actually never be the case...)
					Node toNode = getNodeForStopFacility(toStop.getStopFacility().getId(), false);
					LeastCostPathCalculator.Path tempShortestPath = this.router.calcLeastCostPath(fromNode, toNode, this.mode, routeId);
					if (tempShortestPath != null && (shortestPath == null || (tempShortestPath.travelCost < shortestPath.travelCost))) {
						shortestPath = tempShortestPath;
						fromStop.setStopFacility(this.schedule.getFacilities().get(fromStopFacilityId));
					}
				}
			}
		} else {
			Node fromNode = getNodeForStopFacility(fromStop.getStopFacility().getId(), true);
			if (derivativesToStop != null) {
				// We have to loop over toStops and assign it, but not over fromStop. (This should be the standard case...)
				for (Id<TransitStopFacility> toStopFacilityId : derivativesToStop.get(this.mode)) {
					Node toNode = getNodeForStopFacility(toStopFacilityId, false);
					LeastCostPathCalculator.Path tempShortestPath = this.router.calcLeastCostPath(fromNode, toNode, this.mode, routeId);
					if (tempShortestPath != null && (shortestPath == null || (tempShortestPath.travelCost < shortestPath.travelCost))) {
						shortestPath = tempShortestPath;
						toStop.setStopFacility(this.schedule.getFacilities().get(toStopFacilityId));
					}
				}
			} else {
				// We have to loop over none of the two...
				Node toNode = getNodeForStopFacility(toStop.getStopFacility().getId(), false);
				shortestPath = this.router.calcLeastCostPath(fromNode, toNode, this.mode, routeId);
			}
		}

		return shortestPath;
	}

	/**
	 * @param stopFacilityId for which the attached node is returned
	 * @param toNode True returns toNode, False returns fromNode
	 * @return Node if a link is attached to facility, null else.
	 */
	private Node getNodeForStopFacility(Id<TransitStopFacility> stopFacilityId, boolean toNode) {
		if (this.schedule.getFacilities().get(stopFacilityId).getLinkId() != null) {
			if (toNode) {
				return this.network.getLinks().get(this.schedule.getFacilities().get(stopFacilityId).getLinkId()).getToNode();
			} else {
				return this.network.getLinks().get(this.schedule.getFacilities().get(stopFacilityId).getLinkId()).getFromNode();
			}
		}
		return null;
	}

	/**
	 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
	 * and all nodes which are non-linked to any link after the above cleaning...
	 * Clean also the allowed modes for only the modes, no line-number any more...
	 */
	protected void cleanStationsAndNetwork() {
		log.info("Clean Stations and Network...");
		cleanSchedule();
		removeNonUsedStopFacilities();
		cleanModes();
		removeNonUsedLinks();
		log.info("Clean Stations and Network... done.");
	}

	private void cleanSchedule() {
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			Set<TransitRoute> toRemove = new HashSet<>();
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();

				// TODO-boescpa: This check here should not be necessary. Find reason!!!
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

	private void cleanModes() {
		// Collect all pt-links:
		Set<Id<Link>> usedPTLinks = new HashSet<>();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				usedPTLinks.add(transitRoute.getRoute().getStartLinkId());
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					usedPTLinks.add(linkId);
				}
				usedPTLinks.add(transitRoute.getRoute().getEndLinkId());
			}
		}
		// Set new modes:
		for (Link link : this.network.getLinks().values()) {
			Set<String> modes = new HashSet<>();
			if (link.getAllowedModes().contains("car") && !link.getId().toString().contains("Pseudo")) {
				modes.add("car");
			}
			if (usedPTLinks.contains(link.getId())) {
				modes.add("pt");
			}
			if (modes.isEmpty()) {
				modes.add("remove");
			}
			link.setAllowedModes(modes);
		}
	}

	private void removeNonUsedLinks() {
		// Collect all non-used links:
		Set<Link> unusedLinks = new HashSet<>();
		for (Link link : this.network.getLinks().values()) {
			if (link.getAllowedModes().contains("remove")) {
				unusedLinks.add(link);
			}
		}
		// Remove non-used pt-exclusive links and any nodes not used anymore because of removal:
		for (Link link : unusedLinks) {
			if (this.network.getLinks().containsKey(link.getId())) {
				this.network.removeLink(link.getId());
				removeUnusedNode(link.getFromNode().getId());
				removeUnusedNode(link.getToNode().getId());
			}
		}
	}

	private void removeUnusedNode(Id<Node> nodeId) {
		if (nodeId != null && this.network.getNodes().containsKey(nodeId)) {
			if (this.network.getNodes().get(nodeId).getInLinks().isEmpty() &&
					this.network.getNodes().get(nodeId).getOutLinks().isEmpty()) {
				this.network.removeNode(nodeId);
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
