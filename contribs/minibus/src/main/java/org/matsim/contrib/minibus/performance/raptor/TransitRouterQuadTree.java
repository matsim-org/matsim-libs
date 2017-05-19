/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetwork.java
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
 * *********************************************************************** */

package org.matsim.contrib.minibus.performance.raptor;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.transitSchedule.api.*;

/**
 * 
 * @author aneumann
 */
public final class TransitRouterQuadTree {

	private final static Logger log = Logger.getLogger(TransitRouterQuadTree.class);

	private final RaptorDisutility raptorDisutility;
	
	private QuadTree<TransitStopFacility> quadTree;
	private Map<TransitStopFacility, Integer> transitStopFacility2Index = new HashMap<>();

	// These used to be organized as follows (optimization for many route stops but only a few departures
	// Each block contains data for one route. Each route block holds sub-blocks for each departure. Each sub-block holds arrival and departure times sorted in increasing order.
	
	// currently optimized for routes with not so many route stops but many departures (minibuses)
	private double[] arrivalTimes;
	private double[] departureTimes;
	
	// All routes each linking to its departures (stopTimes) and served stops (routeStops).
	private RouteEntry[] routes;
	
	// Each block holds the stops served by a route in the sequence of serving it.
	private RouteStopEntry[] routeStops;
	
	// Each block holds the potential transfers for each stop.
	private TransferEntry[] transfers;
	
	// All stops serving at least one route with links to their transfers (transfers) and routes served (stopRoutes).
//	private TransitRouteStop[] stops;
	
	// All stops serving at least one route with links to their transfers (transfers) and routes served (stopRoutes).
	private TransitStopEntry[] transitStops;
	

	public TransitRouterQuadTree(RaptorDisutility raptorDisutility) {
		this.raptorDisutility = raptorDisutility;
	}

	public Collection<TransitStopFacility> getNearestTransitStopFacilities(final Coord coord, final double distance) {
		return this.quadTree.getDisk(coord.getX(), coord.getY(), distance);
	}

	public TransitStopFacility getNearestTransitStopFacility(final Coord coord) {
		return this.quadTree.getClosest(coord.getX(), coord.getY());
	}
	
	public RaptorSearchData getSearchData(){
		return new RaptorSearchData(this.arrivalTimes, this.departureTimes, this.routes, this.routeStops, this.transfers, this.transitStops, this.transitStopFacility2Index);
	}

	public void initializeFromSchedule(final TransitSchedule transitSchedule, final double maxBeelineWalkConnectionDistance) {
		
		this.fillArrays(transitSchedule, maxBeelineWalkConnectionDistance);
		
		log.info("transit router network statistics:");
		log.info(" # stops:           " + this.transitStops.length);
		log.info(" # routes:          " + this.routes.length);
		log.info(" # transfer links:  " + this.transfers.length);
	}

	private void fillArrays(TransitSchedule transitSchedule, double maxBeelineWalkConnectionDistance) {
		
		// arrays that need to be converted in the end
		ArrayList<Double> departureTimesList = new ArrayList<Double>();
		ArrayList<Double> arrivalTimesList = new ArrayList<Double>();
		ArrayList<RouteEntry> routesList = new ArrayList<RouteEntry>();
		ArrayList<RouteStopEntry> routeStopList = new ArrayList<RouteStopEntry>();
		ArrayList<TransferEntry> transfersList = new ArrayList<TransferEntry>();
		ArrayList<TransitStopEntry> stopsList = new ArrayList<TransitStopEntry>();
		
		// temporary storage
		Map<WrappedTransitRouteStop, Id<TransitRoute>> routeStop2routeId = new HashMap<WrappedTransitRouteStop, Id<TransitRoute>>();
		Map<TransitStopFacility, Set<Id<TransitRoute>>> transitStopFacilities2RouteIdsServed = new HashMap<TransitStopFacility, Set<Id<TransitRoute>>>();
		ArrayList<TransitStopFacility> transitStopsInRightOrder = new ArrayList<TransitStopFacility>();
		HashMap<String, WrappedTransitRouteStop> hash2routeStop = new HashMap<String, WrappedTransitRouteStop>();
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				int position = 0;
				for (TransitRouteStop routeStop : route.getStops()) {
					
					String routeStopHash = this.getHash(line, route, routeStop, position);
					WrappedTransitRouteStop wrappedRouteStop = new WrappedTransitRouteStop(routeStop);
					hash2routeStop.put(routeStopHash, wrappedRouteStop);
					
					position++;
					
					routeStop2routeId.put(wrappedRouteStop, route.getId());
					
					if (transitStopFacilities2RouteIdsServed.get(routeStop.getStopFacility()) == null) {
						transitStopFacilities2RouteIdsServed.put(routeStop.getStopFacility(), new TreeSet<Id<TransitRoute>>());
					}
					transitStopFacilities2RouteIdsServed.get(routeStop.getStopFacility()).add(route.getId());
				}
			}
		}
		
		this.quadTree = this.createTransitStopFacilityQuadTree(transitStopFacilities2RouteIdsServed.keySet());
		Map<TransitStopFacility, HashMap<WrappedTransitRouteStop, TransferEntryPointer>> stop2TransitRouteStop2Transfers = this.createTransfers(transitStopFacilities2RouteIdsServed, routeStop2routeId, maxBeelineWalkConnectionDistance);
		stop2TransitRouteStop2Transfers = filterTransfers(transitSchedule, stop2TransitRouteStop2Transfers, routeStop2routeId);

		for (TransitStopFacility transitStopFacility : stop2TransitRouteStop2Transfers.keySet()) {
			this.transitStopFacility2Index.put(transitStopFacility, transitStopsInRightOrder.size());
			transitStopsInRightOrder.add(transitStopFacility);
		}
		
		Map<WrappedTransitRouteStop, Integer> routeStopEntry2Index = new HashMap<>();
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				
				// fill departures
				final int indexOfFirstDeparture = departureTimesList.size();
				final int numberOfDepartures = route.getDepartures().size();
				double[] departureTimes = new double[numberOfDepartures];

				int i = 0;
				for (Departure dep : route.getDepartures().values()) {
					departureTimes[i++] = dep.getDepartureTime();
				}
				Arrays.sort(departureTimes);

				for (TransitRouteStop stop : route.getStops()) {
					for (int j = 0; j < departureTimes.length; j++) {
						if (stop.getArrivalOffset() == Double.NEGATIVE_INFINITY) {
							// There should always be a valid number set OR the field should not be present - it's optional
							// Take the departure offset as fallback
							arrivalTimesList.add(departureTimes[j] + stop.getDepartureOffset());
						} else {
						arrivalTimesList.add(departureTimes[j] + stop.getArrivalOffset());
						}
						
						departureTimesList.add(departureTimes[j] + stop.getDepartureOffset());
					}
				}
				// So the arrivalTimesList contains | stop1ar1 stop1ar2 ... stop1arLast stop2ar1 stop2ar2 stop2arLast ... |,
				// and this in addition flattened for all routes.
				// It seems that it can reconstruct everything from "indexOfFirstDeparture", "numberOfDepartures", "numberOfRouteSteps".
				
				final int indexOfFirstStop = routeStopList.size();
				final int numberOfRouteStops = route.getStops().size();
				
				final int indexOfRoute = routesList.size();
				
				// fill route
				RouteEntry routeEntry = new RouteEntry(line.getId(), route.getId(), indexOfFirstDeparture, numberOfDepartures, indexOfFirstStop, numberOfRouteStops);
				routesList.add(routeEntry);
				
				
				// fill route stops
				int placeOfCurrentStop = 0;
				for (TransitRouteStop routeStop : route.getStops()) {
					String routeStopHash = getHash(line, route, routeStop, placeOfCurrentStop);
					WrappedTransitRouteStop wrappedRouteStop = hash2routeStop.get(routeStopHash);
					
					placeOfCurrentStop++;

					final int indexOfRouteStop = routeStopList.size();
					final int numberOfRemainingStopsInThisRoute = route.getStops().size() - placeOfCurrentStop;
					
					routeStopList.add(new RouteStopEntry(indexOfRoute, this.transitStopFacility2Index.get(routeStop.getStopFacility()).intValue(), indexOfRouteStop, numberOfRemainingStopsInThisRoute));
					routeStopEntry2Index.put(wrappedRouteStop, indexOfRouteStop);
				}
				

			}
		}
		
		// complete stops and their transfers
		for (TransitStopFacility transitStopFacility : transitStopsInRightOrder) {
			int indexOfFirstTransfer = transfersList.size();
			int numberOfTransfers = 0;
			
			for (TransferEntryPointer transferEntryPointer : stop2TransitRouteStop2Transfers.get(transitStopFacility).values()) {
				int indexOfRouteStop = routeStopEntry2Index.get(transferEntryPointer.transferDestination).intValue();
				transfersList.add(new TransferEntry(indexOfRouteStop, transferEntryPointer.transferTime));
				numberOfTransfers++;
			}
			
			stopsList.add(new TransitStopEntry(transitStopFacility, numberOfTransfers, indexOfFirstTransfer));
		}
		
		// create the final arrays
		this.arrivalTimes = getPrimitiveArrayFromList(arrivalTimesList);
		
		this.departureTimes = getPrimitiveArrayFromList(departureTimesList);

		this.routes = new RouteEntry[routesList.size()];
		routesList.toArray(this.routes);
		
		this.routeStops = new RouteStopEntry[routeStopList.size()];
		routeStopList.toArray(this.routeStops);
		
		this.transfers = new TransferEntry[transfersList.size()];
		transfersList.toArray(this.transfers);

		this.transitStops = new TransitStopEntry[stopsList.size()];
		stopsList.toArray(this.transitStops);
	}

	private Map<TransitStopFacility, HashMap<WrappedTransitRouteStop, TransferEntryPointer>> createTransfers(Map<TransitStopFacility, Set<Id<TransitRoute>>> transitStopFacilities2RouteIdsServed, Map<WrappedTransitRouteStop, Id<TransitRoute>> routeStop2routeId, double maxBeelineWalkConnectionDistance) {
		Map<TransitStopFacility, HashMap<WrappedTransitRouteStop, TransferEntryPointer>> stop2TransitRouteStop2Transfers = new HashMap<>();
		
		QuadTree<WrappedTransitRouteStop> transitRouteStopQuadTree = this.createTransitRouteStopQuadTree(routeStop2routeId.keySet());
		
		// create transfers for all transit route stops if they're located less than beelineWalkConnectionDistance from each other
		for (TransitStopFacility sourceStop : transitStopFacilities2RouteIdsServed.keySet()) {
			// (just goes through all stops)

			HashMap<WrappedTransitRouteStop, TransferEntryPointer> transfersFromThis = new HashMap<WrappedTransitRouteStop, TransferEntryPointer>();
			
			for (WrappedTransitRouteStop destinationStop : transitRouteStopQuadTree.getDisk(sourceStop.getCoord().getX(), sourceStop.getCoord().getY(), maxBeelineWalkConnectionDistance)) {
				// (goes through all stops within walkConnectionDistance)

				double transferTime = this.raptorDisutility.getTransferTime(sourceStop.getCoord(), destinationStop.transitRouteStop.getStopFacility().getCoord());
				transfersFromThis.put(destinationStop, new TransferEntryPointer(-1, transferTime, destinationStop, routeStop2routeId.get(destinationStop)));
				// (memorize: (1) which stop the walk goes to; (2) route id associated with that stop (presumably one stop per route even at same location)
			}
			
			stop2TransitRouteStop2Transfers.put(sourceStop, transfersFromThis);
		}
		
		int transfersAdded = 0;
		for (HashMap<WrappedTransitRouteStop, TransferEntryPointer> routeStop2Transfers : stop2TransitRouteStop2Transfers.values()) {
			transfersAdded += routeStop2Transfers.size();
		}
		log.info("Added " + transfersAdded + " transfers (from each transit stop facility to each other transit route stop within " + maxBeelineWalkConnectionDistance + "m beeline distance.");
		
		return stop2TransitRouteStop2Transfers;
	}

	private Map<TransitStopFacility, HashMap<WrappedTransitRouteStop, TransferEntryPointer>> filterTransfers(TransitSchedule transitSchedule, Map<TransitStopFacility, HashMap<WrappedTransitRouteStop, TransferEntryPointer>> stop2TransitRouteStop2Transfers, Map<WrappedTransitRouteStop, Id<TransitRoute>> routeStop2routeId) {
		Map<TransitStopFacility, Set<Id<TransitRoute>>> transitStopFacilities2RouteIdsThatCanBeTransferedTo = new HashMap<TransitStopFacility, Set<Id<TransitRoute>>>();
		
		for (TransitStopFacility transitStopFacility : stop2TransitRouteStop2Transfers.keySet()) {
			for (WrappedTransitRouteStop routeStopEntry : stop2TransitRouteStop2Transfers.get(transitStopFacility).keySet()) {
				if (transitStopFacilities2RouteIdsThatCanBeTransferedTo.get(transitStopFacility) == null) {
					transitStopFacilities2RouteIdsThatCanBeTransferedTo.put(transitStopFacility, new TreeSet<Id<TransitRoute>>());
				}
				transitStopFacilities2RouteIdsThatCanBeTransferedTo.get(transitStopFacility).add(routeStop2routeId.get(routeStopEntry));
			}
		}
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				Set<Id<TransitRoute>> secondLastRouteIdsServed = null;
				Set<Id<TransitRoute>> lastRouteIdsServed = null;
				Set<Id<TransitRoute>> currentRouteIdsServed = null;
				
				TransitStopFacility lastTransitStopFacility = null;
				
				for (TransitRouteStop stop : route.getStops()) {
					currentRouteIdsServed = transitStopFacilities2RouteIdsThatCanBeTransferedTo.get(stop.getStopFacility());
	
					if (secondLastRouteIdsServed != null) {
						// we now have three stops that can be compared
						// remove all transfers from the last (the middle stop) if all three stops serve at least the same routes as well
						
						boolean atLeastOneStopDoesNotServeAllRoutes = false;
						for (Id<TransitRoute> routeId : lastRouteIdsServed) {
							// check for all routes served by the middle stop if those are also served by its predecessor and the following stop
							if (!secondLastRouteIdsServed.contains(routeId) || !currentRouteIdsServed.contains(routeId)) {
								// one of those two stops does not serve the route
								atLeastOneStopDoesNotServeAllRoutes = true;
								break;
							}
						}
						if (!atLeastOneStopDoesNotServeAllRoutes) {
							// ok, all stops serve the routes
							// remove all transfers for this stop
							stop2TransitRouteStop2Transfers.put(lastTransitStopFacility, new HashMap<WrappedTransitRouteStop, TransferEntryPointer>());
						}
					}
					secondLastRouteIdsServed = lastRouteIdsServed;
					lastRouteIdsServed = currentRouteIdsServed;
					lastTransitStopFacility = stop.getStopFacility();
				}
			}
		}
		
		// count again
		int transfersAdded = 0;
		for (HashMap<WrappedTransitRouteStop, TransferEntryPointer> routeStop2Transfers : stop2TransitRouteStop2Transfers.values()) {
			transfersAdded += routeStop2Transfers.size();
		}
		log.info(transfersAdded + " transfers remain after filtering.");
	
		return stop2TransitRouteStop2Transfers;
	}
	
	private QuadTree<TransitStopFacility> createTransitStopFacilityQuadTree(Set<TransitStopFacility> stops) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
	
		for (TransitStopFacility stop : stops) {
			Coord c = stop.getCoord();
			if (c.getX() < minX) {
				minX = c.getX();
			}
			if (c.getY() < minY) {
				minY = c.getY();
			}
			if (c.getX() > maxX) {
				maxX = c.getX();
			}
			if (c.getY() > maxY) {
				maxY = c.getY();
			}
		}
	
		QuadTree<TransitStopFacility> quadTree = new QuadTree<TransitStopFacility>(minX, minY, maxX, maxY);
		for (TransitStopFacility stop : stops) {
			Coord c = stop.getCoord();
			quadTree.put(c.getX(), c.getY(), stop);
		}
		
		return quadTree;
	}

	private QuadTree<WrappedTransitRouteStop> createTransitRouteStopQuadTree(Set<WrappedTransitRouteStop> routeStops) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
	
		for (WrappedTransitRouteStop stop : routeStops) {
			Coord c = stop.transitRouteStop.getStopFacility().getCoord();
			if (c.getX() < minX) {
				minX = c.getX();
			}
			if (c.getY() < minY) {
				minY = c.getY();
			}
			if (c.getX() > maxX) {
				maxX = c.getX();
			}
			if (c.getY() > maxY) {
				maxY = c.getY();
			}
		}
	
		QuadTree<WrappedTransitRouteStop> quadTree = new QuadTree<WrappedTransitRouteStop>(minX, minY, maxX, maxY);
		for (WrappedTransitRouteStop stop : routeStops) {
			Coord c = stop.transitRouteStop.getStopFacility().getCoord();
			quadTree.put(c.getX(), c.getY(), stop);
		}
		
		return quadTree;
	}
	
	private static double[] getPrimitiveArrayFromList(List<Double> list) {
	    double[] array = new double[list.size()];
	    int i = 0;
	    for (Double entry : list) {
	        array[i++] = entry.doubleValue();
	    }
	    return array;
	}
	
	private static String getHash(TransitLine transitLine, TransitRoute transitRoute, TransitRouteStop transitRouteStop, int position){
		return transitLine.getId().toString() + "-" + transitRoute.getId().toString() + "-" + transitRouteStop.getStopFacility().getId().toString() + "-" + position;
	}
}
