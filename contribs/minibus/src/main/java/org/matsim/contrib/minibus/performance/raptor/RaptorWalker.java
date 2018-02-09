/* *********************************************************************** *
 * project: org.matsim.*
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.InitialNode;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitPassengerRoute;
import org.matsim.pt.router.RouteSegment;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Transit route searching algorithm following the idea of the connection scan algorithm as described by e.g.
 * Delling, D. and Pajor, T. and Werneck, R.F., "Round-Based Public Transit Routing" in "Sixth Annual Symposium on Combinatorial Search", 2013.<br>
 * <br>
 * The algorithms had been extended to allow for circle transit route, i.e. routes that serve the same transit route stop more than once.
 * The search itself is based on earliest arrival time only. However, from the set of routes found in each round the one with the least costs is kept.
 * That is, depending on the configuration of {@linkplain RaptorDisutility}, a longer route but with fewer transfers may be returned.<br>
 * <br>
 * Note that
 * <ul>
 * 	<li>The cost (per boarding or per distance) do not respect the route used. In consequence, the algorithm will always return the fastest route although
 *  a cheaper route with the same number of transfers may exist. For example, a high-speed train and a local stop train both offer a direct connection between two cities.
 *  The algorithm returns always the high-speed connection.
 * 	<li>Changing the {@linkplain RaptorDisutility} may not be sufficient in case a) both connections serve the same target stops or b) the target stops are too
 *  close to each other so that transferring from the high-speed stop to the local train stop allows for an earlier arrival time than taking the stop in the first place.
 * 	<li>The algorithm allows for transferring multiple times without having to board a vehicle. Thus, longer transfers are possible if this is indeed faster. In the end, consecutive transfers are merged.
 * </ul>
 * 
 * The algorithm works in rounds. In detail
 * <ol>
 * 	<li>Search for a all transit stop facilities within a given range from the coordinate of the start activity.
 * 	<li>Parse all transit routes serving these transit stop facilities, each starting from the transit route stop associated with the transit stop facility.
 * 	<li>Check all transit stop facilities that could be updated with a earlier arrival time for possible transfers.
 * 	<li>Start again with 2. and check the routes serving the transfer destination stops.
 * </ol>
 * Each route stop has a back pointer pointing to its predecessor (route stop).<br>
 * <br>
 * 
 * In each round, starting with zero transfers, the best route is retrieved by
 * <ol>
 * 	<li>Take all transit stop facilities within a given range of the coordinate of the target activity.
 * 	<li>Backtrace from each destination stop to the starting stops and create a sequence of route segments.
 * 	<li>Score each sequence of route segments.
 * 	<li>Return the sequence with the least costs.
 * </ol>
 * The best route of a round is compared with the best route of the next round. The better one is kept and eventually returned.  
 *
 * The algorithm returns null if no route is found after the given number of max transfers.
 * Once a route is found the algorithm runs for an additional number of rounds specified by the number of grace transfers.
 *  
 * 
 * @author aneumann
 *
 */
public class RaptorWalker {
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(RaptorWalker.class);

	private final RaptorDisutility raptorDisutility;
	private final RaptorSearchData raptorSearchData;
	private final int maxTransfers;
	private final int graceTransfers;

	private final SourcePointer initialSourcePointer;
	
	private final double[] earliestArrivalTimeAtRouteStop;
	private final SourcePointer[] sourcePointerRouteStop;

	private final double[] earliestArrivalTimeAtTransitStop;
	private final SourcePointer[] sourcePointerTransitStops;

	private final boolean[] routeStopsToCheck;
	private final boolean[] transferTransitStopsToCheck;

	public RaptorWalker(RaptorSearchData raptorSearchData, TransitTravelDisutility transitTravelDisutility, int maxTransfers, int graceTransfers) {
		this.raptorDisutility = (RaptorDisutility) transitTravelDisutility;
		this.raptorSearchData = raptorSearchData;
		this.maxTransfers = maxTransfers;
		this.graceTransfers = graceTransfers;
		
		this.initialSourcePointer = new SourcePointer(Double.POSITIVE_INFINITY, -1, null, false);
		
		this.earliestArrivalTimeAtRouteStop = new double[this.raptorSearchData.routeStops.length];
		this.sourcePointerRouteStop = new SourcePointer[this.raptorSearchData.routeStops.length];
		
		this.earliestArrivalTimeAtTransitStop = new double[this.raptorSearchData.stops.length];
		this.sourcePointerTransitStops = new SourcePointer[this.raptorSearchData.stops.length];
		
		this.routeStopsToCheck = new boolean[this.raptorSearchData.routeStops.length];
		this.transferTransitStopsToCheck = new boolean[this.raptorSearchData.stops.length];
	}

	public TransitPassengerRoute calcLeastCostPath(Map<TransitStopFacility, InitialNode> fromTransitStops, Map<TransitStopFacility, InitialNode> toTransitStops) {
		
		TransitPassengerRoute bestRoute = null;

		// init
		Arrays.fill(this.earliestArrivalTimeAtRouteStop, Double.POSITIVE_INFINITY);
		Arrays.fill(this.sourcePointerRouteStop, this.initialSourcePointer);
		Arrays.fill(this.earliestArrivalTimeAtTransitStop, Double.POSITIVE_INFINITY);
		Arrays.fill(this.sourcePointerTransitStops, this.initialSourcePointer);
		Arrays.fill(this.routeStopsToCheck, false);
		Arrays.fill(this.transferTransitStopsToCheck, false);

		// init first stops
		for (TransitStopFacility fromTransitStop : fromTransitStops.keySet()) {
			// (these are all the transit stops initialized by the Multi-Node router)

			double departureTime = fromTransitStops.get(fromTransitStop).initialTime;
			// (get dp time from node (since they vary by multi-node))

			int indexOfFromTransitStop = this.getIndexForTransitStop(fromTransitStop);
			TransitStopEntry transitStopEntry = this.raptorSearchData.stops[indexOfFromTransitStop];
			// (look up index and size of corresponding transit stop entry (fortran))

			for (int indexOfTransferToCheck = transitStopEntry.indexOfFirstTransfer; indexOfTransferToCheck < transitStopEntry.indexOfFirstTransfer + transitStopEntry.numberOfTransfers; indexOfTransferToCheck++) {
				// check all route stops that can be reached from this transit stop
				// ignore transfers that result in a different transit stop facility
				// otherwise we would extend the search radius
				TransferEntry transferEntry = this.raptorSearchData.transfers[indexOfTransferToCheck];
				RouteStopEntry transferRouteStopEntry = this.raptorSearchData.routeStops[transferEntry.indexOfRouteStop];
				if (indexOfFromTransitStop == transferRouteStopEntry.indexOfStopFacility) {
					// this is a route stop of the transit stop facility
					SourcePointer source = new SourcePointer(departureTime, transferEntry.indexOfRouteStop, null, false);
					sourcePointerRouteStop[transferEntry.indexOfRouteStop] = source;
					earliestArrivalTimeAtRouteStop[transferEntry.indexOfRouteStop] = departureTime;
					earliestArrivalTimeAtTransitStop[indexOfFromTransitStop] = departureTime;
					routeStopsToCheck[transferEntry.indexOfRouteStop] = true;
				}
			}
		}

		int graceTransfersLeft = this.graceTransfers;

		// MAIN LOOP:
		for (int nTransfers = 0; nTransfers <= this.maxTransfers; nTransfers++) {
			Arrays.fill(transferTransitStopsToCheck, false);

			this.checkRouteStops();

			TransitPassengerRoute bestRouteOfThisRound = this.getBestRouteFoundSoFar(fromTransitStops, toTransitStops, sourcePointerTransitStops);
			if (bestRouteOfThisRound != null) {
				if (bestRoute == null) {
					// this is the first route found
					bestRoute = bestRouteOfThisRound;
				} else {
					// check if this one is better
					if (bestRouteOfThisRound.getTravelCost() < bestRoute.getTravelCost()) {
						bestRoute = bestRouteOfThisRound;
					}
				}
			}


			if (bestRoute != null) {
				// one route found - check how many additional runs are allowed
				if (graceTransfersLeft == 0) {
					// no more transfers left - abort and return best route found so far
					return bestRoute;
				}
				graceTransfersLeft--;
			}

			Arrays.fill(routeStopsToCheck, false);
			// we have now explored every place without additional transfers

			// increase the transfers and proceed
			if(nTransfers < this.maxTransfers){
//				nTransfers++;

				// yyyy to me, this looks like in the end we are now going 0, 2, 4, 6, since we are incrementing both here and in
				// the for loop.  ????  kai, jun'16
				//right, there is no reason to increase the counter twice. Commenting one. Amit Jan'18

				this.checkTransferTransitStops();
			}
		}

		return bestRoute;
	}

	private void checkRouteStops() {

		boolean atLeastOneRouteStopImproved = false;
		int indexOfLastRouteProcessed = -1;

		// process all start stops
		for (int indexOfStartRouteStop = 0; indexOfStartRouteStop < routeStopsToCheck.length; indexOfStartRouteStop++) {
			// I think that these are all route stops, and the "check" refers to the boolean

			if (routeStopsToCheck[indexOfStartRouteStop] == true) {
				RouteStopEntry startStop = this.raptorSearchData.routeStops[indexOfStartRouteStop];
				RouteEntry routeToCheck = this.raptorSearchData.routes[startStop.indexOfRoute];
				
				// TODO do not proceed if this stop's best arrival time has just got updated in this round
				// updates at this part of the round can only occur by parsing the same route but from one of the upstream stops
				// thus all following downstream stops were already updated as well and have all a better arrival time as possibly this stop's departure would yield

				// this is not entirely true because if one of the intermediate stop get a better arrival time (by transfer).
				// This means, if any of the intermediate stop is not improved, let it run if the same route is encounterd again.
				// doing this by setting "atLeastOneRouteStopImproved = false;" and breaking the process of updating arrival times of rest of the stops of the route. Amit Sep'17

				if (indexOfLastRouteProcessed == startStop.indexOfRoute && atLeastOneRouteStopImproved) {
					// we process the same route again
					// since route stops have increasing indices we only need to process all downstream stops again if the last try could not improve the arrival times

					// do nothing and proceed
				} else {
					// this is a different route - process all stops
					indexOfLastRouteProcessed = startStop.indexOfRoute;
					atLeastOneRouteStopImproved = false;

					double earliestArrivalTimeAtStartRouteStop = sourcePointerRouteStop[indexOfStartRouteStop].earliestArrivalTime;

					int indexOfRouteStopWithinRouteSequence = routeToCheck.numberOfRouteStops - startStop.numberOfRemainingStopsInThisRoute - 1;
					int indexOfEarliestDepartureTime = this.getIndexOfEarliestDepartureTime(earliestArrivalTimeAtStartRouteStop, routeToCheck, indexOfRouteStopWithinRouteSequence);

					// It appears that if departure time is after midnight, it needs to be updated here
					// so that an earliest connection for next day can be looked.
					// However, the arrival time in the array is actual time. Amit Aug'17
					double adjustedEarliestArrivalTimeAtStartRouteStop  = earliestArrivalTimeAtStartRouteStop;

					// transit router may have departures after midnight, so if there is no available departure for rest of the day, take earliest departure next day.
					if (indexOfEarliestDepartureTime <= -1 && adjustedEarliestArrivalTimeAtStartRouteStop >= RaptorDisutility.MIDNIGHT ) {
						adjustedEarliestArrivalTimeAtStartRouteStop = adjustedEarliestArrivalTimeAtStartRouteStop % RaptorDisutility.MIDNIGHT;
					}

					indexOfEarliestDepartureTime = this.getIndexOfEarliestDepartureTime(adjustedEarliestArrivalTimeAtStartRouteStop, routeToCheck, indexOfRouteStopWithinRouteSequence);

					if(indexOfEarliestDepartureTime > -1) {
						// we have found a valid departure time - process all upcoming stops of the route
						for (int indexOfRouteStopToCheck = indexOfStartRouteStop + 1; indexOfRouteStopToCheck < indexOfStartRouteStop + startStop.numberOfRemainingStopsInThisRoute + 1; indexOfRouteStopToCheck++) {

							// we go now one stop further
							indexOfEarliestDepartureTime += routeToCheck.numberOfDepartures;
							RouteStopEntry routeStopToCheck =  this.raptorSearchData.routeStops[indexOfRouteStopToCheck];

							double arrivalTimeAtTheFollowingRouteStop = this.raptorSearchData.arrivalTimes[indexOfEarliestDepartureTime];

							while (arrivalTimeAtTheFollowingRouteStop < earliestArrivalTimeAtStartRouteStop ) {
								arrivalTimeAtTheFollowingRouteStop += RaptorDisutility.MIDNIGHT;
								// (add enough "MIDNIGHT"s until we are _after_ the desired departure time)
							}

							if (arrivalTimeAtTheFollowingRouteStop < earliestArrivalTimeAtRouteStop[routeStopToCheck.indexOfRouteStop]) {
								// this really is better than anything before - set arrival and source and mark the stop to be checked for transfers
								atLeastOneRouteStopImproved = true;

								SourcePointer source = new SourcePointer(arrivalTimeAtTheFollowingRouteStop, routeStopToCheck.indexOfRouteStop, sourcePointerRouteStop[indexOfStartRouteStop], false);
								sourcePointerRouteStop[routeStopToCheck.indexOfRouteStop] = source;
								earliestArrivalTimeAtRouteStop[routeStopToCheck.indexOfRouteStop] = arrivalTimeAtTheFollowingRouteStop;

								// is it also an earlier arrival time at the transit stop
								if (arrivalTimeAtTheFollowingRouteStop < earliestArrivalTimeAtTransitStop[routeStopToCheck.indexOfStopFacility]) {
									earliestArrivalTimeAtTransitStop[routeStopToCheck.indexOfStopFacility] = arrivalTimeAtTheFollowingRouteStop;
									sourcePointerTransitStops[routeStopToCheck.indexOfStopFacility] = source;
									transferTransitStopsToCheck[routeStopToCheck.indexOfStopFacility] = true;
								}
							} else {
								atLeastOneRouteStopImproved = false;
								break;
							}
						}
					} else {
						// there is no further departure
						// TODO search for the earliest departure and add 24h
						// implemented above by adjusting the time; still keeping the comment until all tests are happy. Amit Aug'17

						// another possibility, there is no futher departures on this stop for this route (routeToCheck), but let's say agent is here and now make another transfer.
						// this will increase the effective "maxBeelineWalkConnectionDistance"... Amit Aug'17
						transferTransitStopsToCheck[startStop.indexOfStopFacility] = true;
					}
				}

			}

		}
	}

	private void checkTransferTransitStops() {

		// check all transfer stops
		for (int indexOfTransitStopToCheck = 0; indexOfTransitStopToCheck < transferTransitStopsToCheck.length; indexOfTransitStopToCheck++) {
			if (transferTransitStopsToCheck[indexOfTransitStopToCheck]) {
				TransitStopEntry transitStopEntry = this.raptorSearchData.stops[indexOfTransitStopToCheck];

				for (int indexOfTransferToCheck = transitStopEntry.indexOfFirstTransfer; indexOfTransferToCheck < transitStopEntry.indexOfFirstTransfer + transitStopEntry.numberOfTransfers; indexOfTransferToCheck++) {
					// process all transfers of the stop

					TransferEntry transferDestination = this.raptorSearchData.transfers[indexOfTransferToCheck];
					int indexOfTransferRouteStop = transferDestination.indexOfRouteStop;
					double arrivalTimeAtTheTransferTargetRouteStop = earliestArrivalTimeAtTransitStop[indexOfTransitStopToCheck] + transferDestination.transferTime;

					if (arrivalTimeAtTheTransferTargetRouteStop < earliestArrivalTimeAtRouteStop[indexOfTransferRouteStop]) {
						// this really is better than anything before - set arrival and source and mark the stop to be checked in the next iteration
						SourcePointer source = new SourcePointer(arrivalTimeAtTheTransferTargetRouteStop, indexOfTransferRouteStop, sourcePointerTransitStops[indexOfTransitStopToCheck], true);
						sourcePointerRouteStop[indexOfTransferRouteStop] = source;
						earliestArrivalTimeAtRouteStop[indexOfTransferRouteStop] = arrivalTimeAtTheTransferTargetRouteStop;
						routeStopsToCheck[indexOfTransferRouteStop] = true;

						int indexOfTargetTransitStop = this.raptorSearchData.routeStops[indexOfTransferRouteStop].indexOfStopFacility;
						if (arrivalTimeAtTheTransferTargetRouteStop < earliestArrivalTimeAtTransitStop[indexOfTargetTransitStop]) {
							// this is also the best arrival time for the transit facility of the route stop
							earliestArrivalTimeAtTransitStop[indexOfTargetTransitStop] = arrivalTimeAtTheTransferTargetRouteStop;
							sourcePointerTransitStops[indexOfTargetTransitStop] = source;
						}
					}
				}

			}

		}
	}
	
	private TransitPassengerRoute getBestRouteFoundSoFar(Map<TransitStopFacility, InitialNode> fromTransitStops, Map<TransitStopFacility, InitialNode> toTransitStops, SourcePointer[] sourcePointerTransitStops) {
		// get routes for all n of transfers and return them
		List<List<RouteSegment>> routesFound = new LinkedList<List<RouteSegment>>();
		for (TransitStopFacility toTransitStop : toTransitStops.keySet()) {
			int indexOfToTransitStop = this.getIndexForTransitStop(toTransitStop);
					
			SourcePointer source = sourcePointerTransitStops[indexOfToTransitStop];
			if (source.source != null) {
				// something found - backtrace
				List<RouteSegment> route = this.returnBacktracedRouteFromSourcePointer(source, fromTransitStops);
				if (!route.isEmpty()) {
					// there is a route with this number of transfers to that stop
					routesFound.add(route);
				}
			}
		}
		
		if (!routesFound.isEmpty()) {
			// score and pick best
			return this.scoreRoutesAndReturnBest(routesFound, fromTransitStops, toTransitStops);
		} else {
			return null;
		}
	}
	
//	private int getIndexOfEarliestDepartureTime(double earliestArrivalTimeAtStartRouteStop, RouteEntry routeToCheck, int indexOfRouteStopWithinRouteSequence){
//
//		int indexOfDepartureToCheck = routeToCheck.indexOfFirstDeparture + indexOfRouteStopWithinRouteSequence;
//		for (int i = 0; i < routeToCheck.numberOfDepartures; i++) {
//			double departureTimeCandidate = this.raptorSearchData.stopTimes[indexOfDepartureToCheck].departureTime;
//			if (departureTimeCandidate >= earliestArrivalTimeAtStartRouteStop) {
//				return indexOfDepartureToCheck;
//			}
//			// get to the next departure
//			indexOfDepartureToCheck += routeToCheck.numberOfRouteStops;
//		}
//		
//		return -1;
//	}
	
	/**
	 * adapted from {@link PreparedTransitSchedule}
	 * 
	 */
	private int getIndexOfEarliestDepartureTime(double earliestArrivalTimeAtStartRouteStop, RouteEntry routeToCheck, int indexOfRouteStopWithinRouteSequence){

		int fromIndex = routeToCheck.indexOfFirstDeparture + routeToCheck.numberOfDepartures * indexOfRouteStopWithinRouteSequence;
		int toIndex = fromIndex + routeToCheck.numberOfDepartures;
		
		int pos = Arrays.binarySearch(this.raptorSearchData.departureTimes, fromIndex, toIndex, earliestArrivalTimeAtStartRouteStop);
		if (pos < 0) {
			// (if the departure time is not found _exactly_, binarySearch returns (-(insertion point) - 1).  That is
			// retval = -(insertion point) - 1  or insertion point = -(retval+1) .
			// This will, in fact, be the normal situation, so it is important to understand this.)
			pos = -(pos + 1);
		}
		if (pos >= toIndex) {
//			pos = 0; // there is no later departure time, take the first in the morning
			return -1;
		}
		
		return pos;
		
//		double bestDepartureTime = this.raptorSearchData.stopTimes[pos].departureTime;
//		// (resulting departure time at stop)
//		
//		while (bestDepartureTime < earliestArrivalTimeAtStartRouteStop) {
//			bestDepartureTime += RaptorDisutility.MIDNIGHT;
//			// (add enough "MIDNIGHT"s until we are _after_ the desired departure time)
//		}
//		
//		return bestDepartureTime;
	}
	
	private int getIndexForTransitStop(TransitStopFacility fromTransitStop) {
		return this.raptorSearchData.transitStopFacility2Index.get(fromTransitStop);
	}

	private List<RouteSegment> returnBacktracedRouteFromSourcePointer(SourcePointer sourcePointer, Map<TransitStopFacility, InitialNode> fromTransitStops) {
		List<RouteSegment> route = new LinkedList<>();
		
		SourcePointer currentSourcePointer = sourcePointer;
		RouteSegment lastRouteSegment = null;
		
		while (currentSourcePointer.source != null) {

			RouteStopEntry toRouteStopEntry = this.raptorSearchData.routeStops[currentSourcePointer.indexOfTargetRouteStop];
			TransitStopEntry transitStopEntry = this.raptorSearchData.stops[toRouteStopEntry.indexOfStopFacility];
			TransitStopFacility toTransitStop = transitStopEntry.transitStopFacility;

			if (currentSourcePointer.source.indexOfTargetRouteStop == -1 ){
				if (currentSourcePointer.transfer) {
					// access_stop and transfer
					TransitStopFacility fromTransitStop = null;
					double dist = Double.POSITIVE_INFINITY;
					for (TransitStopFacility possibleFromStop : fromTransitStops.keySet()){ // an alternative would be to get the transfer stops at _toTransitStop_ and match with fromStops. Amit Jan'18
						double tempDist = NetworkUtils.getEuclideanDistance(possibleFromStop.getCoord().getX(), possibleFromStop.getCoord().getY(), toTransitStop.getCoord().getX(), toTransitStop.getCoord().getY());
						if (tempDist < dist) {
							dist = tempDist;
							fromTransitStop = possibleFromStop;
						}
					}

					if (fromTransitStop!=null) {
						RouteSegment routeSegment = new RouteSegment(fromTransitStop,
								toTransitStop,
								this.raptorSearchData.transfers[getIndexForTransitStop(fromTransitStop)].transferTime,
								null,
								null);
						route.add(0,routeSegment);
					}
				} else {
					// this additional condition is required to exclude the case in which origin and destination are same transit stop (source.source.indexOfTargetRouteStop=-1).
					// This can be verified by something like "this.raptorSearchData.routeStops[source.indexOfTargetRouteStop].indexOfStopFacility != indexOfToTransitStop"
					// in getBestRouteFoundSoFar(...) method. However, if above condition is used in "getBestRouteFoundSoFar",
					// it still throws exception here, so, excluding such situations here. Amit Jan'18
				}
			} else {
				RouteStopEntry fromRouteStopEntry = this.raptorSearchData.routeStops[currentSourcePointer.source.indexOfTargetRouteStop];
				TransitStopFacility fromTransitStop = this.raptorSearchData.stops[fromRouteStopEntry.indexOfStopFacility].transitStopFacility;

				double travelTime = currentSourcePointer.earliestArrivalTime - currentSourcePointer.source.earliestArrivalTime;

				RouteSegment routeSegment;
				if (currentSourcePointer.transfer) {
					if (lastRouteSegment != null) {
						if (lastRouteSegment.getRouteTaken() == null) {
							// had been a transfer, too - merge both
							toTransitStop = lastRouteSegment.getToStop();
							travelTime += lastRouteSegment.getTravelTime();
							route.remove(0);
						}
					}
					routeSegment = new RouteSegment(fromTransitStop, toTransitStop, travelTime, null, null);
				} else {
					RouteStopEntry routeStopEntry = this.raptorSearchData.routeStops[currentSourcePointer.indexOfTargetRouteStop];
					RouteEntry routeEntry = this.raptorSearchData.routes[routeStopEntry.indexOfRoute];
					routeSegment = new RouteSegment(fromTransitStop, toTransitStop, travelTime, routeEntry.lineId, routeEntry.routeId);
				}
				route.add(0, routeSegment);
				lastRouteSegment = routeSegment;
			}
			// set pointer to next alighting stop, skip transfers
			currentSourcePointer = currentSourcePointer.source;
		}
		
		return route;
	}

	private TransitPassengerRoute scoreRoutesAndReturnBest(List<List<RouteSegment>> routesFound, Map<TransitStopFacility, InitialNode> fromTransitStops, Map<TransitStopFacility, InitialNode> toTransitStops) {
		
		TransitPassengerRoute bestRouteSoFar = null;
		
		for (List<RouteSegment> route : routesFound) {
			double cost = this.scoreRoute(route, fromTransitStops, toTransitStops);
			if (bestRouteSoFar == null) {
				bestRouteSoFar = new TransitPassengerRoute(cost, route);
			} else if (cost < bestRouteSoFar.getTravelCost()) {
				bestRouteSoFar = new TransitPassengerRoute(cost, route);
			}
		}
		
		return bestRouteSoFar;
	}

	private double scoreRoute(List<RouteSegment> route, Map<TransitStopFacility, InitialNode> fromStops, Map<TransitStopFacility, InitialNode> toStops) {
		
		double cost = 0.0;
		
		for (RouteSegment routeSegment : route) {
			if (routeSegment.getRouteTaken() == null) {
				// handle transfer
				cost += this.raptorDisutility.getTransferCost(routeSegment.getFromStop().getCoord(), routeSegment.getToStop().getCoord());
			} else {
				// pt trip
				cost += this.raptorDisutility.getInVehicleTravelDisutility(routeSegment);
			}
		}
		
		// add cost for getting to the first and last stop
		cost += fromStops.get(route.get(0).getFromStop()).initialCost;
		cost += toStops.get(route.get(route.size() - 1).getToStop()).initialCost;
		
		return cost;
	}

}
