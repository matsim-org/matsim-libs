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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.*;

import java.util.HashMap;
import java.util.Map;

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

	@Override
	public void routePTLines(Network network) {
		log.info("Creating PT lines...");
		linkStationsToNetwork(network);
		createPTRoutes(network);

		// TODO-boescpa Implement pseudoNetwork-Creation for "other" modes...
		// But only if automatic teleportation doesn't work...

		cleanStations();

		log.info("Creating PT lines... done.");
	}

	/**
	 * Link the pt-stations in the schedule to the closest network links.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param network
	 */
	private void linkStationsToNetwork(Network network) {
		log.info("Linking pt stations to network...");

		// TODO-boescpa Implement linkStationsToNetwork...

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				if (route.getTransportMode().equals(BUS)) {
					setMode(BUS);
					linkRouteToNetwork(network, route);
				} else if (route.getTransportMode().equals(TRAM)) {
					setMode(TRAM);
					linkRouteToNetwork(network, route);
				}
			}
		}

		log.info("Linking pt stations to network... done.");
	}

	private void linkRouteToNetwork(Network network, TransitRoute route) {
		//	for each route in each line in schedule:
		TransitRouteStop formerStop = null, currentStop, nextStop;
		for (int i = 0; i < route.getStops().size(); i++) {
			currentStop = route.getStops().get(i);
			nextStop = null;
			if ((i+1) < route.getStops().size()) {
				nextStop = route.getStops().get(i+1);
			}

			//		if stop has not already a link attached to it:
			Map<String, Id<TransitStopFacility>[]> derivativesCurrentStop = linkedStopFacilities.get(currentStop.getStopFacility().getId());
			if (derivativesCurrentStop == null) {
				derivativesCurrentStop = new HashMap<>();
				linkedStopFacilities.put(currentStop.getStopFacility().getId(), derivativesCurrentStop);
			}
			Id<TransitStopFacility>[] modeSpecificDerivativesCurrentStop = derivativesCurrentStop.get(mode);
			if (modeSpecificDerivativesCurrentStop == null) {
				TransitStopFacility newStop = createTypeDependentStopFacility(currentStop);

				//	find closest street-link crossing within search radius of stop-position and, if available, opposite direction link.
				Id<Link> closestLink = findClosestLink(newStop, network);
				Id<Link> oppositeDirectionLink = null;
				if (closestLink != null) {
					oppositeDirectionLink = getOppositeDirection(closestLink, network);
				} else {
					//	if no closest link found, create link linking the stop to the former stop and one to the following link. attach station to the incoming link. do this for both directions!
					if (formerStop != null && nextStop != null) {
						closestLink = createBidirectionallyLinksBetweenStops(formerStop, currentStop);
						oppositeDirectionLink = createBidirectionallyLinksBetweenStops(nextStop, currentStop);
					} else if (formerStop != null) {
						closestLink = createBidirectionallyLinksBetweenStops(formerStop, currentStop);
					} else if (nextStop != null) {
						closestLink = createBidirectionallyLinksBetweenStops(nextStop, currentStop);
					}
				}

				//	link stop-position(s) to the respective link(s).
				if (oppositeDirectionLink == null) {
					newStop.setLinkId(closestLink);
					derivativesCurrentStop.put(mode, new Id[]{newStop.getId()});
				} else {
					//	if street-link has opposite direction, then split stop-position before linking.
					TransitStopFacility[] newStops = dublicateStop(newStop);
					newStops[0].setLinkId(closestLink);
					newStops[1].setLinkId(oppositeDirectionLink);
					derivativesCurrentStop.put(mode, new Id[]{newStops[0].getId(), newStops[1].getId()});
				}
			}

			formerStop = currentStop;
		}
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
		return null;
	}

	/**
	 * Create and add to schedule two new stops with the id-endings "_A" and "_B" by copying the provided stop.
	 * Return those two stops.
	 *
	 * @param stop
	 * @return TransitStopFacilities stopA and stopB
	 */
	private TransitStopFacility[] dublicateStop(TransitStopFacility stop) {
		return null;
	}


	/**
	 * Search for the closest link within search radius that has the current mode as allowed travel mode and returns it.
	 *
	 * @param busStop
	 * @param network
	 * @return
	 */
	private Id<Link> findClosestLink(TransitStopFacility busStop, Network network) {
		// Attention: Search in the network for the link is mode dependent!!!
		double radius = searchRadius;

		return null;
	}

	/**
	 * Looks in the network if the link has an opposite direction link and if so returns it.
	 * In the case of non-street-modes (e.g. TRAM), it looks also geographically because
	 * stations might be large and the opposite direction link therefore further away...
	 *
	 * @param link
	 * @param network
	 * @return Opposite direction link
	 */
	private Id<Link> getOppositeDirection(Id<Link> link, Network network) {
		// Even if none found with Id, if mode tram then have to check if one with same mode geographically...
		// Attention: Search in the network is mode dependent!!
		return null;
	}


	/**
	 * Add new TransitStopFacility to schedule and to the provided stop,
	 * which is a copy of the provided stop.getStopFacility() except for the type is now of vehicleType.
	 *
	 * @param stop
	 * @return Type-dependent stop facility.
	 */
	private TransitStopFacility createTypeDependentStopFacility(TransitRouteStop stop) {
		// Get type by using this.mode...
		return null;
	}

	/**
	 * By applying a routing algorithm (e.g. shortest path or OSM-extraction) route from station to
	 * station for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param network
	 */
	private void createPTRoutes(Network network) {
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
	 * After all lines created, clean all non-linked stations...
	 */
	private void cleanStations() {

	}
}
