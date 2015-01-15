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

package playground.boescpa.converters.osm.ptRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static playground.boescpa.converters.osm.scheduleCreator.PtRouteFPLAN.BUS;
import static playground.boescpa.converters.osm.scheduleCreator.PtRouteFPLAN.TRAM;

/**
 * Default implementation of PTLinesCreator.
 *
 * @author boescpa
 */
public class PTLineRouterDefault extends PTLineRouter {

	private final static double SEARCH_RADIUS_BUS = 5; //[m]
	private final static double SEARCH_RADIUS_TRAM = 10; //[m]

	private final Map<Id<TransitStopFacility>, Map<String, Id<TransitStopFacility>[]>> linkedStopFacilities = new HashMap<>();

	private String mode;
	private double searchRadius;
	private void setMode(String mode) {
		switch (mode) {
			case BUS: {
				this.mode = BUS;
				this.searchRadius = SEARCH_RADIUS_BUS;
				break;
			}
			case TRAM: {
				this.mode = TRAM;
				this.searchRadius = SEARCH_RADIUS_TRAM;
				break;
			}
			default: {
				log.warn("Mode " + mode + " not available for network assignment.");
				this.mode = null;
				this.searchRadius = 0;
			}
		}
	}

	public PTLineRouterDefault(TransitSchedule schedule) {
		super(schedule);
	}

	public PTLineRouterDefault(TransitSchedule schedule, Network network) {
		super(schedule, network);
	}

	@Override
	public void routePTLines(Network network) {
		setNetwork(network);

		log.info("Creating PT lines...");
		linkStationsToNetwork();
		correctStationLinks();
		createPTRoutes();
		createPseudoNetworkForOtherModes();
		cleanStationsAndNetwork();
		log.info("Creating PT lines... done.");
	}

	/**
	 * Link the pt-stations in the schedule to the closest network links.
	 * Writes the resulting schedule into this.schedule.
	 */
	private void linkStationsToNetwork() {
		log.info("Linking pt stations to network...");

		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
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
	 * Search for the closest link that has the current mode as allowed travel mode and return this link.
	 *
	 * @param stopFacility Stop facility to search a link for.
	 * @return Null if no such link could be found.
	 */
	private Id<Link> findClosestLink(TransitStopFacility stopFacility) {
		Link nearestLink = NetworkUtils.getNearestLink(this.network, stopFacility.getCoord());
		if (nearestLink.getAllowedModes().contains(this.mode)) {
			// If nearest link has right mode, return it.
			return nearestLink.getId();
		} else {
			// Search for the allowed link with the shortest distance:
			nearestLink = null;
			double currentRadius = Double.MAX_VALUE;
			for (Link potentialLink : this.network.getLinks().values()) {
				if (potentialLink.getAllowedModes().contains(this.mode)
						&& NetworkUtils.getEuclidianDistance(stopFacility.getCoord(), potentialLink.getCoord()) < currentRadius) {
					currentRadius = NetworkUtils.getEuclidianDistance(stopFacility.getCoord(), potentialLink.getCoord());
					nearestLink = potentialLink;
				}
			}
			if (nearestLink != null) {
				return nearestLink.getId();
			} else {
				throw new RuntimeException("No link with right mode found in network! Network and schedule will not be functional. Please check configuration and retry.");
				// If no nearest link with right mode could be found in network, error is thrown...
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
		// If we find one with "opposite" direction, we return only this.
		Link lowerLink = this.network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) - 1));
		Link link = this.network.getLinks().get(linkId);
		Link upperLink = this.network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) + 1));
		if (lowerLink != null
				&& lowerLink.getFromNode().getId().toString().equals(link.getToNode().getId().toString())
				&& lowerLink.getToNode().getId().toString().equals(link.getFromNode().getId().toString())
				&& lowerLink.getAllowedModes().contains(this.mode)) {
			return new Id[]{lowerLink.getId()};
		}
		if (upperLink != null
				&& upperLink.getFromNode().getId().toString().equals(link.getToNode().getId().toString())
				&& upperLink.getToNode().getId().toString().equals(link.getFromNode().getId().toString())
				&& upperLink.getAllowedModes().contains(this.mode)) {
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
			if (link.getAllowedModes().contains(this.mode)
					&& NetworkUtils.getEuclidianDistance(centralCoords, link.getCoord()) < this.searchRadius) {
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
		stop.setStopFacility(newFacility);
		// Return new facility:
		return newFacility;
	}

	/**
	 * If euclidan distance between stop and its link is too big (bigger than stop-distance_mode),
	 * create a new node at the stop and reconnect the stop to newly created links connecting the new node with
	 * the existing network (using createBidirectionallyLinksBetweenStops).
	 */
	private void correctStationLinks() {
		// TODO-boescpa Implement correct station links...

		/*
		TransitRouteStop formerStop = null, currentStop, nextStop;
		for (int i = 0; i < route.getStops().size(); i++) {
			currentStop = route.getStops().get(i);
			nextStop = null;
			if ((i+1) < route.getStops().size()) {
				nextStop = route.getStops().get(i+1);
			}

			...

		if (closestLink != null) {
			oppositeDirectionLinks = getOppositeDirection(closestLink, network);
		} else {
			//	if no closest link found, create link linking the stop to the former stop and one to the following link. attach station to the incoming link. do this for both directions!
			if (formerStop != null && nextStop != null) {
				closestLink = createBidirectionallyLinksBetweenStops(network, formerStop, currentStop);
				oppositeDirectionLinks = new Id[]{createBidirectionallyLinksBetweenStops(network, nextStop, currentStop)};
			} else if (formerStop != null) {
				closestLink = createBidirectionallyLinksBetweenStops(network, formerStop, currentStop);
			} else if (nextStop != null) {
				closestLink = createBidirectionallyLinksBetweenStops(network, nextStop, currentStop);
			}
		}
		*/

		/*
		if (NetworkUtils.getEuclidianDistance(stopFacility.getCoord(), attachedLink.getCoord()) <= this.linkRadius) {
			...
		}
		 */
	}

	/**
	 * Creates bidirectionally links between the two stops,
	 * adds them to the network with travel mode "mode"
	 * and returns the from-to-Link.
	 *
	 * @param fromStop
	 * @param toStop
	 * @return Created From-To-Link.
	 */
	private Id<Link> createBidirectionallyLinksBetweenStops(TransitRouteStop fromStop, TransitRouteStop toStop) {
		// TODO-boescpa nodes have to be available at both stops to link them... Think about it...

		/*Id<Link> fromLinkId = fromStop.getStopFacility().getLinkId();
		Id<Link> toLinkId = toStop.getStopFacility().getLinkId();
		Node fromNode = null;
		if (fromLinkId != null) {
			fromNode = network.getLinks().get(fromLinkId).getToNode();
		} else {

		}*/

		// TODO-boescpa implement createBidirectionallyLinksBetweenStops
		return null;
	}

	/**
	 * By applying a routing algorithm (e.g. shortest path or OSM-extraction) route from station to
	 * station for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 */
	private void createPTRoutes() {
		log.info("Creating pt routes...");

		// TODO-boescpa Implement createPTRoutes...

		// 	For tram and bus routes for each line in schedule:
		//		route from each stop to next (respectively from link to link...).
		//		for trams make sure use only "tram"-links.
		//		costFunction: if non-that-line-number-link, then much more expensive...
		//		costFunction: if non-pt-link, then even much more expensive...

		log.info("Creating pt routes... done.");
	}

	/**
	 * All PT-modes that are not connected to the given network, get their own pseudo network integrated
	 * in the given network. This also includes linking the stations to the new links and nodes and routing the
	 * the pt-lines (can all be done at once...).
	 */
	private void createPseudoNetworkForOtherModes() {
		// TODO-boescpa Implement this...
	}

	/**
	 * After all lines created, clean all non-linked stations, all pt-exclusive links (check allowed modes)
	 * and all nodes which are non-linked to any link after the above cleaning...
	 */
	private void cleanStationsAndNetwork() {
		// TODO-boescpa Implement this...
	}
}
