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
import org.matsim.core.utils.geometry.CoordImpl;
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
	private TransitScheduleFactory scheduleFactory;

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
		this.scheduleFactory = this.schedule.getFactory();
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
	 * @param network the stations are linked to.
	 */
	private void linkStationsToNetwork(Network network) {
		log.info("Linking pt stations to network...");

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
				TransitStopFacility newStopFacility = createTypeDependentStopFacility(currentStop);

				//	find closest street-link crossing within search radius of stop-position and, if available, opposite direction link.
				Id<Link> closestLink = findClosestLink(newStopFacility, network);
				Id<Link>[] oppositeDirectionLinks = null;
				if (closestLink != null) {
					oppositeDirectionLinks = getOppositeDirection(closestLink, network);
				} else {
					//	if no closest link found, create link linking the stop to the former stop and one to the following link. attach station to the incoming link. do this for both directions!
					if (formerStop != null && nextStop != null) {
						closestLink = createBidirectionallyLinksBetweenStops(formerStop, currentStop);
						oppositeDirectionLinks = new Id[]{createBidirectionallyLinksBetweenStops(nextStop, currentStop)};
					} else if (formerStop != null) {
						closestLink = createBidirectionallyLinksBetweenStops(formerStop, currentStop);
					} else if (nextStop != null) {
						closestLink = createBidirectionallyLinksBetweenStops(nextStop, currentStop);
					}
				}

				//	link stop-position(s) to the respective link(s).
				if (oppositeDirectionLinks == null) {
					newStopFacility.setLinkId(closestLink);
					derivativesCurrentStop.put(mode, new Id[]{newStopFacility.getId()});
				} else {
					//	if street-link has opposite direction, then split stop-position before linking.
					TransitStopFacility[] newStopFacilities = multiplyStop(newStopFacility, oppositeDirectionLinks.length);
					Id<TransitStopFacility>[] newStopFacilityIds = new Id[newStopFacilities.length];
					newStopFacilities[0].setLinkId(closestLink);
					newStopFacilityIds[0] = newStopFacilities[0].getId();
					for (int j = 0; j < oppositeDirectionLinks.length; j++) {
						newStopFacilities[j+1].setLinkId(oppositeDirectionLinks[j]);
						newStopFacilityIds[j+1] = newStopFacilities[j+1].getId();
					}
					derivativesCurrentStop.put(mode, newStopFacilityIds);
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
		// TODO-boescpa nodes have to be available at both stops to link them... Think about it...

		// TODO-boescpa implement createBidirectionallyLinksBetweenStops
		return null;
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
			TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
					idNewFacility, stop.getCoord(), stop.getIsBlockingLane()
			);
			newFacility.setName(stop.getName());
			// Add new facility to schedule and to array:
			schedule.addStopFacility(newFacility);
			facilities[i] = newFacility;
		}
		return facilities;
	}

	/**
	 * Search for the closest link within search radius that has the current mode as allowed travel mode and returns it.
	 *
	 * @param stopFacility Stop facility to search a link for.
	 * @param network The network within to search for the link.
	 * @return Null if no such link could be found.
	 */
	private Id<Link> findClosestLink(TransitStopFacility stopFacility, Network network) {
		Link nearestLink = NetworkUtils.getNearestLink(network, stopFacility.getCoord());
		if (NetworkUtils.getEuclidianDistance(stopFacility.getCoord(), nearestLink.getCoord()) <= this.searchRadius) {
			if (nearestLink.getAllowedModes().contains(this.mode)) {
				// If nearest link is within radius and has right mode, return it.
				return nearestLink.getId();
			} else {
				// Else, look in circles around the stopFacility for another nearest link, that might fullfil all requirements.
				Set<Link> potentialLinks = getLinksWithinSearchRadius(stopFacility.getCoord(), network);
				if (!potentialLinks.isEmpty()) {
					// Search for the potential Link with the shortest distance:
					double currentRadius = this.searchRadius;
					for (Link potentialLink : potentialLinks) {
						double linkRadius = NetworkUtils.getEuclidianDistance(stopFacility.getCoord(), potentialLink.getCoord());
						if (linkRadius < currentRadius) {
							currentRadius = linkRadius;
							nearestLink = potentialLink;
						}
					}
					return nearestLink.getId();
				}
			}
		}
		// If no nearest link could be found within radius and with right mode allowed, null is returned.
		return null;
	}

	private Set<Link> getLinksWithinSearchRadius(Coord centralCoords, Network network) {
		Set<Link> linksWithinRadius = new HashSet<>();
		for (double r = 0.1; r <= 0.5; r += 0.1) {
            for (double phi = 0; phi < 2 * Math.PI; phi += Math.PI / 4) {
                Coord searchCoords = new CoordImpl(
						centralCoords.getX() + this.searchRadius * r * Math.cos(phi),
						centralCoords.getY() + this.searchRadius * r * Math.sin(phi)
                );
                Link nearestLink = NetworkUtils.getNearestLink(network, searchCoords);
                if (!linksWithinRadius.contains(nearestLink)) {
                    // This means we have actually found a new link...
                    if (NetworkUtils.getEuclidianDistance(centralCoords, nearestLink.getCoord()) <= this.searchRadius
                            && nearestLink.getAllowedModes().contains(this.mode)) {
                        linksWithinRadius.add(nearestLink);
                    }
                }
            }
        }
		return linksWithinRadius;
	}

	/**
	 * Looks in the network if the link has an opposite direction link and if so returns it.
	 * In the case of non-street-modes (e.g. TRAM), it looks also geographically because
	 * stations might be large and the opposite direction link therefore further away. It then returns all
	 * such surronding links.
	 *
	 * @param linkId of the link for which opposite direction links are searched.
	 * @param network in which opposite direction links are searched.
	 * @return Null if no other direction links could be found...
	 */
	private Id<Link>[] getOppositeDirection(Id<Link> linkId, Network network) {
		// If we find one with "opposite" direction, we return only this.
		Link lowerLink = network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) - 1));
		Link link = network.getLinks().get(linkId);
		Link upperLink = network.getLinks().get(Id.createLinkId(Integer.parseInt(linkId.toString()) + 1));
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
		Set<Link> linksWithinRadius = getLinksWithinSearchRadius(link.getCoord(), network);
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

		// No opposite link is found, and not any in the souroundings, null is returned.
		return null;
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
		Id<TransitStopFacility> idNewFacility = Id.create(stop.getStopFacility().getId().toString() + "_" + this.mode, TransitStopFacility.class);
		TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
				idNewFacility, stop.getStopFacility().getCoord(), stop.getStopFacility().getIsBlockingLane()
		);
		newFacility.setName(stop.getStopFacility().getName());
		// Add new facility to schedule and to stop:
		schedule.addStopFacility(newFacility);
		stop.setStopFacility(newFacility);
		// Return new facility:
		return newFacility;
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
