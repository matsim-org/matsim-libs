/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.singapore.utils.events;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cyberneko.html.HTMLScanner.PlaybackInputStream;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class CepasToEvents {
	// internal classes
	private class ValueComparator implements Comparator<Id> {

		Map<Id, Integer> base;

		public ValueComparator(Map<Id, Integer> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with
		// equals.
		public int compare(Id a, Id b) {
			if (base.get(a) >= base.get(b)) {
				return 1;
			} else {
				return -1;
			} // returning 0 would merge keys
		}
	}

	/**
	 * A Cepas line has the same id as the corresponding MATSim line from the
	 * transit schedule. It contains a maximum of two routes, traveling in
	 * opposite directions.
	 * 
	 */
	private class CepasLine {
		public CepasLine(Id lineId) {
			super();
			this.lineId = lineId;
		}

		Id lineId;
		/**
		 * a Cepas line has a maximum of two routes, traveling in opposite
		 * directions, distinguished by 0 and 1.
		 */
		HashMap<Integer, CepasRoute> routes = new HashMap<Integer, CepasToEvents.CepasRoute>();

		public String toString() {
			return (routes.values().toString());
		}
	}

	/**
	 * 
	 * A Cepas route can refer to different MATSim routes (as per GTFS). In the
	 * tap-in/tap-out data, the only distinguishing attribute is the line id,
	 * and the direction. Each CepasRoute has a number of PTVehicles (buses only
	 * at this stage, because we don't know which line the MRT passengers use,
	 * nor which vehicle.
	 */
	private class CepasRoute {
		/**
		 * @param direction
		 *            , values are 0 and 1
		 * @param cepasLine
		 *            , the line id which corresponds to the MATSim line id
		 */
		public CepasRoute(int direction, CepasLine cepasLine) {
			super();
			this.direction = direction;
			this.line = cepasLine;
		}

		int direction;
		CepasLine line;
		HashMap<Id, CepasVehicle> vehicles = new HashMap<Id, CepasToEvents.CepasVehicle>();

		public String toString() {
			return (line.lineId.toString() + " : " + direction + " : " + vehicles.keySet() + "\n");
		}
	}

	private class CepasVehicle {
		private Id vehicleId;
		private Id transitLineId;
		private CepasLine cepasLine;
		private CepasRoute cepasRoute;
		/**
		 * All possible routes in both directions from the transit schedule.
		 */
		private Map<Id, TransitRoute> possibleMatsimRoutes;
		private TreeSet<Id> possibleMatsimRoutesSortedBynumberOfStops;
		private TreeMap<Integer, TreeSet<CepasVehiclePassenger>> passengersbyAlightingTime = new TreeMap<Integer, TreeSet<CepasVehiclePassenger>>();
		private TreeMap<Integer, TreeSet<CepasVehiclePassenger>> passengersbyBoardingTime = new TreeMap<Integer, TreeSet<CepasVehiclePassenger>>();
		/**
		 * Dwell events, ordered by arrival time.
		 */
		private TreeMap<Integer, CepasVehicleDwellEvent> orderedDwellEvents = new TreeMap<Integer, CepasVehicleDwellEvent>();
		/**
		 * Created by handlePassengers method, then used to create dwell events.
		 */
		private HashMap<Id, ArrayList<CepasTransaction>> cepasTransactionsByStopId = new HashMap<Id, ArrayList<CepasTransaction>>();
		private TreeSet<Id> routesSortedByNumberOfTransactions;
		private List<CepasVehicleDwellEventCluster> dwellEventClusters;
		private ArrayList<CepasTransaction> cepasTransactions = new ArrayList<CepasToEvents.CepasTransaction>();

		// Constructors
		public CepasVehicle(Id matsimTransitLineId, CepasRoute cepasRoute, Id busRegNumber) {
			this.transitLineId = matsimTransitLineId;
			this.cepasRoute = cepasRoute;
			this.cepasLine = cepasRoute.line;
			this.vehicleId = busRegNumber;
			sortMatsimRoutesbyNumberOfStops();
		}

		/**
		 * For each bus, line and direction combo, the boarding and alighting
		 * transactions are recorded by stop id. Passengers are created for all
		 * records that have both a boarding and alighting time.
		 * 
		 * @param resultSet
		 * @throws SQLException
		 */
		public void handlePassengers(ResultSet resultSet) throws SQLException {
			while (resultSet.next()) {
				CepasTransaction boardingTransaction = null;
				CepasTransaction alightingTransaction = null;
				CepasVehiclePassenger passenger;
				int boardingTime = resultSet.getInt("boarding_time");
				Id boardingStop;
				try {
					boardingStop = cepasStoptoMatsimStopLookup.get(resultSet.getString("boarding_stop_stn"));
				} catch (NullPointerException e) {
					// stop is not in the schedule, skip this
					// guy
					continue;
				}
				Id alightingStop = cepasStoptoMatsimStopLookup.get(resultSet.getString("alighting_stop_stn"));
				if (alightingStop == null) {
					// didn't tap out, or stop is not in the schedule, skip this
					// guy{
					boardingTransaction = new CepasTransaction(null, CepasTransactionType.boarding, boardingTime,
							boardingStop);
					this.cepasTransactions.add(boardingTransaction);
					continue;
				}
				int alightingTime = resultSet.getInt("alighting_time");
				Id personId = new IdImpl(resultSet.getLong("card_id"));
				passenger = new CepasVehiclePassenger(personId, boardingStop, alightingStop, boardingTime,
						alightingTime);

				boardingTransaction = new CepasTransaction(passenger, CepasTransactionType.boarding, boardingTime,
						boardingStop);
				this.cepasTransactions.add(boardingTransaction);

				alightingTransaction = new CepasTransaction(passenger, CepasTransactionType.alighting, alightingTime,
						alightingStop);
				this.cepasTransactions.add(alightingTransaction);

				// find the stop visited at a time less than or equal to
				// boarding and alighting time
				try {
					TreeSet<CepasVehiclePassenger> passengersForAlightingTime = this.passengersbyAlightingTime
							.get(alightingTime);
					passengersForAlightingTime.add(passenger);
				} catch (NullPointerException ne) {
					TreeSet<CepasVehiclePassenger> passengersForAlightingTime = new TreeSet<CepasVehiclePassenger>();
					passengersForAlightingTime.add(passenger);
					this.passengersbyAlightingTime.put(alightingTime, passengersForAlightingTime);
				}
				try {
					TreeSet<CepasVehiclePassenger> passengersForBoardingTime = this.passengersbyBoardingTime
							.get(boardingTime);
					passengersForBoardingTime.add(passenger);
				} catch (NullPointerException ne) {
					TreeSet<CepasVehiclePassenger> passengersForBoardingTime = new TreeSet<CepasVehiclePassenger>();
					passengersForBoardingTime.add(passenger);
					this.passengersbyBoardingTime.put(boardingTime, passengersForBoardingTime);
				}
			}
		}

		public void createDwellEventsFromTransactions() {
			for (Id stopId : this.cepasTransactionsByStopId.keySet()) {
				ArrayList<CepasTransaction> transactions = this.cepasTransactionsByStopId.get(stopId);
				Collections.sort(transactions);
				int deltaTime = 0;
				int lastTime = 0;
				ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
				for (int i = 0; i < transactions.size(); i++) {
					CepasTransaction transaction = transactions.get(i);
					deltaTime = transaction.time - lastTime;
					deltaTimes.add(deltaTime);
					lastTime = transaction.time;
				}
				ArrayList<CepasVehicleDwellEvent> dwellEvents = new ArrayList<CepasVehicleDwellEvent>();
				CepasVehicleDwellEvent dwellEvent = null;
				for (int i = 0; i < deltaTimes.size(); i++) {
					deltaTime = deltaTimes.get(i);
					CepasTransaction transaction = transactions.get(i);
					if (deltaTime > interDwellEventTimeForTheSameStopTimeLimit || i == 0) {
						if (dwellEvent != null) {
							dwellEvents.add(dwellEvent);
							dwellEvent.findTrueDwellTime();
						}
						dwellEvent = new CepasVehicleDwellEvent(transaction.time, transaction.time, stopId);
					} else {
						dwellEvent.departureTime = transaction.time;
					}
					dwellEvent.cepasTransactions.add(transaction);
					if (transaction.type.equals(CepasTransactionType.boarding)) {
					} else {
						if (transaction.passenger != null) {
						}
					}
				}
				for (CepasVehicleDwellEvent stopEvent1 : dwellEvents) {
					this.orderedDwellEvents.put(stopEvent1.arrivalTime, stopEvent1);
				}

			}
			// System.out.println(this.printStopsVisited());
		}

		/**
		 * Goes through the list of ordered dwell events, and checks if stops
		 * are visited in order. If dwell event n is at a stop out of the route
		 * order, it first checks if the dwell event n+1 is in order, in which
		 * case the bad dwell event n is erased.
		 * <P>
		 * If dwell event n+1 also visits a stop out of order, it then resets
		 * the iterator for stops, and creates a new cluster of dwell events.
		 */
		public void clusterDwellEventsIntoRoutes() {
			Id likeliestRoute = this.routesSortedByNumberOfTransactions.last();
			LinkedList<Id> stopIds = new LinkedList<Id>();
			stopIds.addAll(routeIdToStopIdSequence.get(likeliestRoute));
			boolean circleRoute = stopIds.getFirst().equals(stopIds.getLast());
			ArrayList<CepasVehicleDwellEvent> dwellEventsList = new ArrayList<CepasVehicleDwellEvent>();
			dwellEventsList.addAll(orderedDwellEvents.values());
			ArrayList<Integer> dwellEventsAsStopIndexList = new ArrayList<Integer>();
			for (CepasVehicleDwellEvent de : dwellEventsList) {
				dwellEventsAsStopIndexList.add(stopIds.indexOf(de.stopId));
			}
			LinkedList<CepasVehicleDwellEventCluster> clusters = new LinkedList<CepasVehicleDwellEventCluster>();
			List<CepasVehicleDwellEvent> currentCluster = new ArrayList<CepasVehicleDwellEvent>();
			currentCluster.add(dwellEventsList.get(0));
			boolean success = true;
			for (int i = 1; i < dwellEventsAsStopIndexList.size(); i++) {
				// if (dwellEventsAsStopIndexList.get(i) == 0
				// && dwellEventsAsStopIndexList.get(i) ==
				// dwellEventsAsStopIndexList.get(i - 1) && circleRoute) {
				// // exception where the travel time to the last stop in the
				// // route was longer, maybe due to skipped stops, and now we
				// // have a repeat dwellevent at the same stop
				// clusters.getLast().getOrderedDwellEvents()
				// .put(dwellEventsList.get(i - 1).arrivalTime,
				// dwellEventsList.get(i - 1));
				// currentCluster = new ArrayList<CepasVehicleDwellEvent>();
				// currentCluster.add(dwellEventsList.get(i));
				// continue;
				// }
				if (dwellEventsAsStopIndexList.get(i) < dwellEventsAsStopIndexList.get(i - 1)) {
					if (i < dwellEventsAsStopIndexList.size() - 1) {

						 if (circleRoute && dwellEventsAsStopIndexList.get(i) == 0) {
							if (getInterDwellEventSpeed(dwellEventsList.get(i - 1), dwellEventsList.get(i)) > getInterDwellEventSpeed(
									dwellEventsList.get(i), dwellEventsList.get(i + 1))) {
								// the dwell event is closer to the previous
								// one
								// than to the next
								// this is actually an end stop; cluster it
								dwellEventsAsStopIndexList.set(i, stopIds.size() - 1);
								currentCluster.add(dwellEventsList.get(i));
							} else {
								// start a new cluster
								clusters.add(new CepasVehicleDwellEventCluster(currentCluster));
								currentCluster = new ArrayList<CepasVehicleDwellEvent>();
								currentCluster.add(dwellEventsList.get(i));
							}
						}else if (getInterDwellEventSpeed(dwellEventsList.get(i - 1), dwellEventsList.get(i + 1)) > getInterDwellEventSpeed(
								dwellEventsList.get(i - 1), dwellEventsList.get(i))) {
							// It's quicker to travel to the next dwell event
							// than
							// to this one, so this dwell event must be
							// erroneous
							orderedDwellEvents.remove(dwellEventsList.get(i).arrivalTime);
							success = false;
							break;
						} else{
//							this is a new cluster that doesn't start from the first stop
							clusters.add(new CepasVehicleDwellEventCluster(currentCluster));
							currentCluster = new ArrayList<CepasVehicleDwellEvent>();
							currentCluster.add(dwellEventsList.get(i));
						}
					} else {
						if (circleRoute && dwellEventsAsStopIndexList.get(i) == 0) {
							// dealing with the last dwell event
							currentCluster.add(dwellEventsList.get(i));
						}
					}
				} else {
					// part of the same cluster
					currentCluster.add(dwellEventsList.get(i));
				}
			}
			// add the last cluster
			if (currentCluster.size() > 1) {
				clusters.add(new CepasVehicleDwellEventCluster(currentCluster));
			}
			if (success) {
				this.dwellEventClusters = clusters;

			} else {
				clusterDwellEventsIntoRoutes();
			}
			// Id firstStopId = null;
			// Id secondStopId = null;
			// CepasVehicleDwellEvent firstDwellEvent = null;
			// CepasVehicleDwellEvent secondDwellEvent = null;
			// int dwellEvtIdx = -1;
			// int stopIdIdx = 0;
			// Iterator<Id> stopIdIterator = stopIds.iterator();
			// while (dwellEvtIdx < orderedDwellEvents.size() - 1) {
			// dwellEvtIdx++;
			// if (stopIdIterator.hasNext()) {
			// firstStopId = stopIdIterator.next();
			// }
			// firstStopId = stopIdIterator.next();
			// firstDwellEvent = dwellEventsList.get(dwellEvtIdx);
			// secondDwellEvent = dwellEventsList.get(dwellEvtIdx + 1);
			// if (firstStopId.equals(firstDwellEvent.stopId)) {
			// dwellEvents.put(firstDwellEvent.arrivalTime, firstDwellEvent);
			// } else {
			// }
			// }

		}

		public void dropDwellEventsNotInRoute() {
			Set<Id> stopIdsInRoute = new HashSet<Id>();
			stopIdsInRoute.addAll(routeIdToStopIdSequence.get(routesSortedByNumberOfTransactions.last()));
			List<Integer> dwellEventsToRemove = new ArrayList<Integer>();
			for (int i : orderedDwellEvents.keySet()) {
				if (!stopIdsInRoute.contains(orderedDwellEvents.get(i).stopId)) {
					dwellEventsToRemove.add(i);
				}
			}
			for (int j : dwellEventsToRemove) {
				orderedDwellEvents.remove(j);
			}
		}

		// Methods
		public String printClusters() {
			StringBuffer sb = new StringBuffer("line\tdirection\tbus_reg_num\tcluster\tstop_id\ttime\ttype\tspeed\n");
			for (CepasVehicleDwellEventCluster dec : this.dwellEventClusters) {
				try {
					CepasVehicleDwellEvent prevStop = dec.getOrderedDwellEvents().firstEntry().getValue();
					for (Entry<Integer, CepasVehicleDwellEvent> entry : dec.getOrderedDwellEvents().entrySet()) {
						CepasVehicleDwellEvent stopEvent = entry.getValue();
						sb.append(String.format(
								"%s\t%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n",
								this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
								this.dwellEventClusters.indexOf(dec), stopEvent.stopId, stopEvent.arrivalTime,
								getInterDwellEventSpeed(prevStop, stopEvent), this.cepasLine.lineId.toString(),
								this.cepasRoute.direction, this.vehicleId.toString(),
								this.dwellEventClusters.indexOf(dec), stopEvent.stopId, stopEvent.departureTime, 0.0));
						prevStop = stopEvent;
					}

				} catch (NullPointerException ne) {
					sb.append(String.format(
							"%s\t%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n",
							this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
							this.dwellEventClusters.indexOf(dec), "NO DATA", -1, -1.0,
							this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
							"NO DATA", -1, -1.0));
				}

			}
			return sb.toString();
		}

		public String printStopsVisited() {
			StringBuffer sb = new StringBuffer("line\tdirection\tbus_reg_num\tstop_id\ttime\ttype\tspeed\n");

			try {
				for (Entry<Integer, CepasVehicleDwellEvent> entry : orderedDwellEvents.entrySet()) {
					CepasVehicleDwellEvent stopEvent = entry.getValue();
					sb.append(String.format("%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n",
							this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
							stopEvent.stopId, stopEvent.arrivalTime, 0.0, this.cepasLine.lineId.toString(),
							this.cepasRoute.direction, this.vehicleId.toString(), stopEvent.stopId,
							stopEvent.departureTime, 0.0));
				}

			} catch (NullPointerException ne) {
				sb.append(String.format("%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%06d\tdeparture\t%f\n",
						this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
						"NO DATA", -1, -1.0, this.cepasLine.lineId.toString(), this.cepasRoute.direction,
						this.vehicleId.toString(), "NO DATA", -1, -1.0));
			}

			return sb.toString();
		}

		/**
		 * orders the possible routes for this vehicle in ascending number of
		 * stops.
		 */
		private void sortMatsimRoutesbyNumberOfStops() {
			try {
				possibleMatsimRoutes = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes();
			} catch (NullPointerException ne) {
				// this route doesn't exist in the transit schedule
				// TODO write an exception handler for this case,
				// for now, just ignore these and report their number of events
				System.out.println("line " + transitLineId.toString() + " does not exist in transit schedule.");
				return;
			}
			HashMap<Id, Integer> unsortedRouteSizes = new HashMap<Id, Integer>();
			for (Id transitRouteId : possibleMatsimRoutes.keySet()) {
				unsortedRouteSizes.put(transitRouteId, possibleMatsimRoutes.get(transitRouteId).getStops().size());
			}
			this.possibleMatsimRoutesSortedBynumberOfStops = new TreeSet<Id>(new ValueComparator(unsortedRouteSizes));
			this.possibleMatsimRoutesSortedBynumberOfStops.addAll(unsortedRouteSizes.keySet());
		}

		/**
		 * Orders the possible Matsim routes for this line and direction by the
		 * number of transactions associated with the stops in the route
		 */
		private void assignRouteScoresByNumberOfTransactions() {
			HashMap<Id, Integer> correlationCount = new HashMap<Id, Integer>();
			for (Id routeId : possibleMatsimRoutesSortedBynumberOfStops) {
				ArrayList<Id> stopList = routeIdToStopIdSequence.get(routeId);
				int score = 0;
				for (Id stopId : cepasTransactionsByStopId.keySet()) {
					if (stopList.contains(stopId))
						score += cepasTransactionsByStopId.get(stopId).size();
				}
				correlationCount.put(routeId, score);
			}
			routesSortedByNumberOfTransactions = new TreeSet<Id>(new ValueComparator(correlationCount));
			routesSortedByNumberOfTransactions.addAll(correlationCount.keySet());
		}

		/**
		 * assume all stops belong to the longest route. interpolate stops not
		 * visited
		 */
		private void interpolateDwellEvents() {
			// start with the shortest route
			int dwellEvtIdx = 0;
			int origStopIdx = 0;
			int destStopIdx = 1;
			int recurringOrigStopidx = 0;
			List<TransitRouteStop> transitStops = possibleMatsimRoutes.get(
					this.routesSortedByNumberOfTransactions.last()).getStops();
			ArrayList<Id> stopList = routeIdToStopIdSequence.get(this.routesSortedByNumberOfTransactions.last());
			ArrayList<CepasVehicleDwellEvent> dwellEventsList = new ArrayList<CepasVehicleDwellEvent>();
			dwellEventsList.addAll(this.orderedDwellEvents.values());
			ITERATE_DWELL_EVENTS: while (!allDwellEventsInterpolated()) {
				// find the first dwell event in a new route, work from there

				if (dwellEventsList.get(dwellEvtIdx).interpolated) {
					dwellEvtIdx++;
					continue ITERATE_DWELL_EVENTS;
				}

				CepasVehicleDwellEvent origDwellEvent = dwellEventsList.get(dwellEvtIdx);
				CepasVehicleDwellEvent destDwellEvent = null;
				if (dwellEvtIdx == dwellEventsList.size() - 1) {
					// either the start and end of a loop or all done
					origDwellEvent.interpolated = true;
					origStopIdx = 0;
					continue ITERATE_DWELL_EVENTS;
				} else {
					destDwellEvent = dwellEventsList.get(dwellEvtIdx + 1);
				}
				TransitRouteStop origStop = null;
				TransitRouteStop destStop = null;
				while (origStopIdx < stopList.size() && destStopIdx < stopList.size()) {
					if (recurringOrigStopidx > 0) {
						origStopIdx = recurringOrigStopidx;
						recurringOrigStopidx = 0;
					}
					if (origStopIdx > destStopIdx) {
						// we have come to the end of a route
						origDwellEvent.interpolated = true;
						int tempIdx = origStopIdx;
						origStopIdx = destStopIdx;
						destStopIdx = tempIdx;
						continue ITERATE_DWELL_EVENTS;
					}
					// skip both these tests if the origin stop has been found
					if (origStop == null && origDwellEvent.stopId.equals(stopList.get(origStopIdx))) {
						// if (dwellEvtIdx > 0 && origStopIdx > 0
						// && dwellEventsList.get(dwellEvtIdx -
						// 1).stopId.equals(stopList.get(origStopIdx)))
						// origStop = transitStops.get(origStopIdx);
						// else if (dwellEvtIdx ==0 && origStopIdx == 0)
						origStop = transitStops.get(origStopIdx);
						// else
					} else if (origStop == null) {
						origStopIdx++;
					}
					// if we are here, the origin stop has been found
					if (destDwellEvent.stopId.equals(stopList.get(destStopIdx))) {
						destStop = transitStops.get(destStopIdx);
					} else if (destStop == null) {
						destStopIdx++;
					}
					if (origStopIdx >= stopList.size()) {
						origStopIdx = 0;
						destStopIdx = 1;
						recurringOrigStopidx = 0;
					}
					if (destStopIdx >= stopList.size()) {
						destStopIdx = 0;
					}
					// check for routes that visit the same stop internally more
					// than once
					if (origDwellEvent.stopId.equals(stopList.get(destStopIdx))) {
						recurringOrigStopidx = destStopIdx;
					}
					if (destStop != null) {
						if (destStopIdx - origStopIdx == 1) {
							// no interpolation necessary
							origDwellEvent.interpolated = true;
							origStopIdx = destStopIdx;
							destStopIdx++;
							continue ITERATE_DWELL_EVENTS;
						} else {
							// the meaty bit where the actual interpolation of
							// dwell times happens
							double availableTime = destDwellEvent.arrivalTime - origDwellEvent.departureTime;
							ArrayList<Id> stopsToVisit = new ArrayList<Id>();
							stopsToVisit.add(stopList.get(origStopIdx));
							int numberOfStopsInterpolated = 0;
							ArrayList<Double> travelDistancesBetweenStops = new ArrayList<Double>();
							ArrayList<Double> timeWeights = new ArrayList<Double>();
							while (origStopIdx < destStopIdx) {
								origStopIdx++;
								numberOfStopsInterpolated++;
								stopsToVisit.add(stopList.get(origStopIdx));
								travelDistancesBetweenStops.add(getInterStopDistance(
										stopsToVisit.get(numberOfStopsInterpolated - 1),
										stopsToVisit.get(numberOfStopsInterpolated)));
							}
							// weight by the distances between stops over the
							// total distance
							double totalTravelDistanceBetweenStops = 0;
							for (double distance : travelDistancesBetweenStops) {
								totalTravelDistanceBetweenStops += distance;
							}
							for (double distance : travelDistancesBetweenStops) {
								timeWeights.add(distance / totalTravelDistanceBetweenStops);
							}
							int dwellTime = origDwellEvent.departureTime;
							// recall that the first stop is the origin; we
							// already have a dwell event for that
							for (int i = 1; i < numberOfStopsInterpolated; i++) {
								dwellTime += (int) (availableTime * timeWeights.get(i - 1));
								CepasVehicleDwellEvent dwellEvent = new CepasVehicleDwellEvent(dwellTime, dwellTime,
										stopsToVisit.get(i));
								this.orderedDwellEvents.put(dwellTime, dwellEvent);
								dwellEvent.interpolated = true;
							}
							// all done, continue from the destination
							origDwellEvent.interpolated = true;
							origStopIdx = destStopIdx;
							destStopIdx++;
							dwellEventsList = new ArrayList<CepasVehicleDwellEvent>();
							dwellEventsList.addAll(this.orderedDwellEvents.values());
							continue ITERATE_DWELL_EVENTS;
						}
					}
				}
			}
			System.out.println(this.printStopsVisited());
		}

		private boolean allDwellEventsInterpolated() {
			for (CepasVehicleDwellEvent dwellEvent : this.orderedDwellEvents.values()) {
				if (dwellEvent.assignedRoute == null || !dwellEvent.routeAssignmentConfirmed)
					return false;
			}
			return true;
		}

		private double getInterDwellEventSpeed(CepasVehicleDwellEvent previousDwellEvent,
				CepasVehicleDwellEvent nextDwellEvent) {
			double distance = getInterStopDistance(previousDwellEvent.stopId, nextDwellEvent.stopId);
			double time = nextDwellEvent.arrivalTime - previousDwellEvent.departureTime;
			double speed = distance / time * 3.6;
			if (speed < 0) {
				return -1;
			} else {

				return Math.min(speed, 1000);
			}
		}

		private double getInterStopDistance(Id stopId, Id stopId2) {
			Id likeliestRoute = this.routesSortedByNumberOfTransactions.last();
			List<TransitRouteStop> stops = this.possibleMatsimRoutes.get(likeliestRoute).getStops();
			Link fromLink = null;
			Link toLink = null;
			for (TransitRouteStop tss : stops) {
				if (tss.getStopFacility().getId().equals(stopId))
					fromLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
				if (tss.getStopFacility().getId().equals(stopId2))
					toLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
			}
			if (fromLink == null || toLink == null)
				return Double.POSITIVE_INFINITY;
			else {
				NetworkRoute route = scenario.getTransitSchedule().getTransitLines().get(this.transitLineId)
						.getRoutes().get(likeliestRoute).getRoute();
				try {
					NetworkRoute subRoute = route.getSubRoute(fromLink.getId(), toLink.getId());
					return RouteUtils.calcDistance(subRoute, scenario.getNetwork());

				} catch (IllegalArgumentException e) {
					double distance = RouteUtils.calcDistance(
							route.getSubRoute(fromLink.getId(), route.getEndLinkId()), scenario.getNetwork());
					return distance
							+ RouteUtils.calcDistance(route.getSubRoute(route.getStartLinkId(), toLink.getId()),
									scenario.getNetwork());

				}

			}

		}

		@Override
		public String toString() {
			String out = String.format("line %s, bus reg %s", this.transitLineId.toString(), this.vehicleId.toString());
			return out;
		}

		public void correctGPSErrors() {
			Collections.sort(this.cepasTransactions);
			int deltaTime = 0;
			int lastTime = 0;
			ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
			for (int i = 0; i < cepasTransactions.size(); i++) {
				CepasTransaction transaction = cepasTransactions.get(i);
				deltaTime = transaction.time - lastTime;
				deltaTimes.add(deltaTime);
				lastTime = transaction.time;
			}
			Id stopId = null;
			Id prevStopId = null;
			CepasTransaction referenceTransaction = null;
			CepasTransaction suspectTransaction = null;
			ArrayList<CepasTransaction> localCluster = null;
			for (int i = 0; i < deltaTimes.size(); i++) {
				deltaTime = deltaTimes.get(i);
				CepasTransaction transaction = cepasTransactions.get(i);
				if (deltaTime > minimumTravelTimeBetweenConsecutiveStops || i == 0) {
					if (i > 0) {
						prevStopId = stopId;
					}
					stopId = transaction.stopId;
					referenceTransaction = transaction;
					localCluster = new ArrayList<CepasToEvents.CepasTransaction>();
					localCluster.add(referenceTransaction);
				} else {
					if (transaction.stopId.equals(stopId)) {
						localCluster.add(transaction);
					} else {
						suspectTransaction = transaction;
						System.err.println("Found a suspect for " + this.vehicleId + " at time "
								+ suspectTransaction.time);
						if (localCluster.size() < minTransactionClusterSize) {
							// fill the localCluster up until you reach the
							// deltaTapTimeLimit
							int j = i + 1;
							while (deltaTimes.get(j) < minimumTravelTimeBetweenConsecutiveStops) {
								localCluster.add(transaction);
								j++;
							}
							// iterate through the local cluster and see what is
							// the majority opinion
							int incumbentStopIdScore = 0;
							int candidateStopIdScore = 0;
							for (CepasTransaction lcTransaction : localCluster) {
								if (lcTransaction.stopId.equals(stopId))
									incumbentStopIdScore++;
								else if (lcTransaction.stopId.equals(suspectTransaction.stopId))
									candidateStopIdScore++;
								else
									System.err.println("Trouble classifying seemingly erroneous transactions for "
											+ this.vehicleId + " at time " + suspectTransaction.time);
							}
							if (incumbentStopIdScore > candidateStopIdScore)
								transaction.stopId = stopId;
							else if (incumbentStopIdScore < candidateStopIdScore) {
								referenceTransaction.stopId = transaction.stopId;
								i = cepasTransactions.indexOf(referenceTransaction) - 1;
								localCluster = new ArrayList<CepasToEvents.CepasTransaction>();
							} else {
								// the two scores are equal, check stopOrder in
								// the matsim route to determine
								try {
									getInterStopDistance(stopId, prevStopId);
									// if we get this far, the stops are in
									// order, and the current point was
									// misclassified
									transaction.stopId = stopId;
								} catch (Exception e) {
									referenceTransaction.stopId = transaction.stopId;
									i = cepasTransactions.indexOf(referenceTransaction) - 1;
									localCluster = new ArrayList<CepasToEvents.CepasTransaction>();
								}
							}

						}
					}
				}
			}

		}

		public void sortCepasTransactionsByStopId() {
			for (CepasTransaction transaction : this.cepasTransactions) {
				try {
					this.cepasTransactionsByStopId.get(transaction.stopId).add(transaction);
				} catch (NullPointerException ne) {
					this.cepasTransactionsByStopId.put(transaction.stopId, new ArrayList<CepasTransaction>());
					this.cepasTransactionsByStopId.get(transaction.stopId).add(transaction);
				}
			}

		}

		public String printTransactions() {
			StringBuffer sb = new StringBuffer("stop_id\ttransaction_type\ttime\n");
			for (CepasTransaction transaction : this.cepasTransactions) {
				sb.append(transaction.toString() + "\n");
			}

			return sb.toString();
		}

	}

	private enum CepasTransactionType {
		boarding, alighting
	}

	private class CepasTransaction implements Comparable<CepasTransaction> {
		CepasVehiclePassenger passenger;
		Id stopId;

		public CepasTransaction(CepasVehiclePassenger passenger, CepasTransactionType type, int time, Id stopId) {
			super();
			this.passenger = passenger;
			this.stopId = stopId;
			this.type = type;
			this.time = time;
		}

		CepasTransactionType type;
		int time;

		@Override
		public int compareTo(CepasTransaction o) {

			return ((Integer) this.time).compareTo(o.time);
		}

		/**
		 * @return stopId, typeOfTransaction, time
		 */
		@Override
		public String toString() {

			return String.format("\"%s\"\t%s\t%d", this.stopId.toString(), this.type.toString(), this.time);
		}
	}

	private class CepasVehicleDwellEvent implements Comparable<CepasVehicleDwellEvent> {
		int arrivalTime;
		int departureTime;
		Id stopId;
		TransitRoute assignedRoute;
		boolean routeAssignmentConfirmed = false;
		boolean interpolated = false;

		/*
		 * if the arrival event is triggered by alighting event, mark the stop
		 * event, and replace the arrival time by the first tap-in time, if any
		 * tap-ins occurred
		 */

		ArrayList<CepasTransaction> cepasTransactions = new ArrayList<CepasTransaction>();

		public int getDwellTime() {
			return departureTime - arrivalTime;
		}

		public CepasVehicleDwellEvent(int arrivalTime, int departureTime, Id stopId) {
			super();
			this.arrivalTime = arrivalTime;
			this.departureTime = departureTime;
			this.stopId = stopId;
		}

		/**
		 * For cases where there are too few transactions to register a
		 * realistic dwell time, or where there are early tap-ins and tap-outs,
		 * or GPS errors.
		 */
		public void simpleDwellTimeAdjustment() {
			CepasTransaction lastAlighting = null;
			CepasTransaction firstBoarding = null;
			for (CepasTransaction transaction : cepasTransactions) {
				if (transaction.type.equals(CepasTransactionType.alighting))
					lastAlighting = transaction;
				else {
					if (firstBoarding == null)
						firstBoarding = transaction;
				}
			}
			if (firstBoarding != null) {
				this.arrivalTime = firstBoarding.time;
			}
			if (lastAlighting != null) {
				this.departureTime = lastAlighting.time;
			}
			if (departureTime <= arrivalTime) {
				departureTime = (departureTime + arrivalTime) / 2;
				arrivalTime = departureTime;
				arrivalTime -= minDwellTime / 2;
				departureTime += minDwellTime / 2;
				return;
			}
			if (getDwellTime() < minDwellTime) {
				int avgtime = (arrivalTime + departureTime) / 2;
				arrivalTime = avgtime - minDwellTime / 2;
				departureTime = avgtime + minDwellTime / 2;
				return;
			}
		}

		public void findTrueDwellTime() {
			if (getDwellTime() < minDwellTime) {
				int avgtime = (arrivalTime + departureTime) / 2;
				arrivalTime = avgtime - minDwellTime / 2;
				departureTime = avgtime + minDwellTime / 2;
				return;
			}

			// if we have only 2-4 transactions,and we're over the minimum
			// dwell time, we need to first find the median time, then find
			// the last alighting after that time
			// and set that as the departure time, and/or the first boarding
			// and
			// set that as the arrival time (only if it is less than the
			// arrival time)
			if (cepasTransactions.size() <= minTransactionClusterSize) {
				simpleDwellTimeAdjustment();
				return;
			}
			/*
			 * cluster the transactions based on the deltaTapTimeLimit
			 */
			int deltaTime = 0;
			int lastTime = cepasTransactions.get(0).time;

			ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
			deltaTimes.add(0);
			ArrayList<ArrayList<CepasTransaction>> transactionClusters = new ArrayList<ArrayList<CepasTransaction>>();
			for (int i = 1; i < cepasTransactions.size(); i++) {
				CepasTransaction transaction = cepasTransactions.get(i);
				deltaTime = transaction.time - lastTime;
				deltaTimes.add(deltaTime);
				lastTime = transaction.time;
			}
			ArrayList<CepasTransaction> transactionCluster = new ArrayList<CepasTransaction>();
			transactionClusters.add(transactionCluster);
			for (int i = 0; i < deltaTimes.size(); i++) {
				deltaTime = deltaTimes.get(i);
				CepasTransaction transaction = cepasTransactions.get(i);
				if (deltaTime < maximumDeltaTapTimeForTransactionsBelongingToOneCluster || i == 0) {
					transactionCluster.add(transaction);
				} else {
					transactionCluster = new ArrayList<CepasTransaction>();
					transactionCluster.add(transaction);
					transactionClusters.add(transactionCluster);
				}
			}
			// find the biggest cluster
			ArrayList<CepasTransaction> targetCluster = null;
			int maxSize = 1;
			for (ArrayList<CepasTransaction> cluster : transactionClusters) {
				if (cluster.size() > maxSize) {
					targetCluster = cluster;
					maxSize = cluster.size();
				}
			}
			if (targetCluster == null) {
				// no clusters bigger than 1, run the simplified procedure;
				simpleDwellTimeAdjustment();
				return;
			} else {
				this.arrivalTime = targetCluster.get(0).time;
				this.departureTime = targetCluster.get(targetCluster.size() - 1).time;
				if (getDwellTime() < minDwellTime) {
					int avgtime = (arrivalTime + departureTime) / 2;
					arrivalTime = avgtime - minDwellTime / 2;
					departureTime = avgtime + minDwellTime / 2;
				}
				return;
			}
		}

		private void updateTransactionTimes() {
			for (CepasTransaction transaction : cepasTransactions) {
				if (transaction.time < arrivalTime) {
					transaction.time = arrivalTime;
				}
				if (transaction.time > departureTime) {
					transaction.time = departureTime;
				}

			}

		}

		@Override
		public String toString() {

			return "stop: " + stopId.toString() + ", time: " + arrivalTime + " - " + departureTime + "\n";
		}

		@Override
		public int compareTo(CepasVehicleDwellEvent o) {
			return (this.arrivalTime < o.arrivalTime) ? -1 : ((this.arrivalTime == o.arrivalTime) ? 0 : 1);
		}

	}

	/**
	 * Clusters dwell events together into a route.
	 */
	private class CepasVehicleDwellEventCluster {
		private TreeMap<Integer, CepasVehicleDwellEvent> orderedDwellEvents = new TreeMap<Integer, CepasToEvents.CepasVehicleDwellEvent>();

		public CepasVehicleDwellEventCluster(List<CepasVehicleDwellEvent> orderedDwellEvents) {
			super();
			for (CepasVehicleDwellEvent de : orderedDwellEvents) {
				this.orderedDwellEvents.put(de.arrivalTime, de);
			}
		}

		public TreeMap<Integer, CepasVehicleDwellEvent> getOrderedDwellEvents() {
			return orderedDwellEvents;
		}
	}

	private class CepasVehiclePassenger implements Comparable<CepasVehiclePassenger> {
		Id personId;

		public CepasVehiclePassenger(Id personId, Id boardingStopId, Id alightingStopId, int boardingTime,
				int alightingTime) {
			super();
			this.personId = personId;
		}

		@Override
		public int compareTo(CepasVehiclePassenger o) {

			return this.personId.compareTo(o.personId);
		}
	}

	// when looking at transactions for each stop, the time required
	// between two consecutive transactions to recognize it as two separate
	// events
	/**
	 * When transactions are evaluated for each stop, this constant is used as
	 * the minimum time between two consecutive transactions that tells us that
	 * they belong to separate dwell events.
	 * <P>
	 * So, if a route starts and ends at the same stop, this is the minimum rest
	 * time between two runs of the route by the same vehicle.
	 */
	private static final int interDwellEventTimeForTheSameStopTimeLimit = 100;
	private static final int minimumTravelTimeBetweenConsecutiveStops = 15;
	private static final int maximumDeltaTapTimeForTransactionsBelongingToOneCluster = 15;
	private static final int minDwellTime = 10;
	private static final int minTransactionClusterSize = 4;
	// fields
	DataBaseAdmin dba;
	Scenario scenario;
	String outputEventsFile;
	private String stopLookupTableName;
	private String tripTableName;
	Queue<Event> eventQueue;
	EventsManager eventsManager;
	/**
	 * This map of lines, organized by id, should correspond to the lines in the
	 * transit schedule.
	 */
	private HashMap<Id, CepasLine> cepasLines;
	/**
	 * Cepas vehicles distinguished by an id composed as line_route_busRegNum.
	 */
	private HashMap<String, CepasVehicle> cepasVehicles = new HashMap<String, CepasToEvents.CepasVehicle>();
	private String serviceTableName;
	private HashMap<String, Id> cepasStoptoMatsimStopLookup = new HashMap<String, Id>();
	/**
	 * Iterate through the stops in a route quite often, so easier to have the
	 * ids in a map than have to call getId() every time
	 */
	private HashMap<Id, ArrayList<Id>> routeIdToStopIdSequence;

	// constructors
	/**
	 * @param databaseProperties
	 *            : a properties file used by sergio's {@link DataBaseAdmin}
	 *            class
	 * @param transitSchedule
	 * @param networkFile
	 * @param outputEventsFile
	 * @param tripTableName
	 *            : where in the database to find the list of Cepas records,
	 *            given as schema.table
	 * @param stopLookupTableName
	 *            : a table in the database that equates each Cepas stop as
	 *            given in the Cepas record with a stop id given in the matsim
	 *            transit schedule
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public CepasToEvents(String databaseProperties, String transitSchedule, String networkFile,
			String outputEventsFile, String tripTableName, String stopLookupTableName) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException, SQLException {

		this.dba = new DataBaseAdmin(new File(databaseProperties));
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario);
		nwr.readFile(networkFile);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		TransitScheduleReader tsr = new TransitScheduleReader(scenario);
		tsr.readFile(transitSchedule);
		eventsManager = EventsUtils.createEventsManager();
		EventWriterXML ewx = new EventWriterXML(outputEventsFile);
		eventsManager.addHandler(ewx);
		eventQueue = new LinkedList<Event>();
		this.tripTableName = tripTableName;
		this.stopLookupTableName = stopLookupTableName;

	}

	// non-static public methods
	public void processEventsQueue() {
		for (Event event : eventQueue) {
			eventsManager.processEvent(event);
		}
	}

	public void run() throws SQLException, NoConnectionException {
		generateRouteIdToStopIdSequence();
		createVehiclesByCepasLineDirectionAndBusRegNum();
		createCepasToMatsimStopLookupTable();
		System.out.println(new Date());
		processCepasTransactionRecordsByLineDirectionBusRegNum();
		System.out.println(new Date());
	}

	private void generateRouteIdToStopIdSequence() {
		routeIdToStopIdSequence = new HashMap<Id, ArrayList<Id>>();
		for (Id lineId : scenario.getTransitSchedule().getTransitLines().keySet()) {
			Map<Id, TransitRoute> routes = scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes();
			for (TransitRoute route : routes.values()) {
				ArrayList<Id> stopIds = new ArrayList<Id>();
				ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
				stops.addAll(route.getStops());
				for (TransitRouteStop trStop : stops) {
					stopIds.add(trStop.getStopFacility().getId());
				}
				routeIdToStopIdSequence.put(route.getId(), stopIds);
			}
		}
	}

	private void createCepasToMatsimStopLookupTable() throws SQLException, NoConnectionException {
		ResultSet resultSet = dba.executeQuery("select *  from " + this.stopLookupTableName
				+ " where matsim_stop is not null and ezlink_stop is not null");
		while (resultSet.next()) {
			String cepasId = resultSet.getString("ezlink_stop");
			Id matsimId = new IdImpl(resultSet.getString("matsim_stop"));
			this.cepasStoptoMatsimStopLookup.put(cepasId, matsimId);
		}

	}

	private void processCepasTransactionRecordsByLineDirectionBusRegNum() throws SQLException, NoConnectionException {
		try {
			BufferedWriter transactionsBeforeWriter = new BufferedWriter(new FileWriter(
					"transactionsBeforeGPSCorrection.csv"));
			BufferedWriter transactionsAfterWriter = new BufferedWriter(new FileWriter(
					"transactionsAfterGPSCorrection.csv"));
			BufferedWriter clusterWriter = new BufferedWriter(new FileWriter("clusters.csv"));
			BufferedWriter dwellEventWriter = new BufferedWriter(new FileWriter("dwellEvents.csv"));
			int vehicles = 0;
			for (CepasVehicle ptVehicle : this.cepasVehicles.values()) {

				if (!ptVehicle.vehicleId.toString().equals("184_1_804"))
					continue;
				if (ptVehicle.possibleMatsimRoutes == null)
					// TODO: if we don't have this transit line in the schedule,
					// ignore
					continue;

				String query = String
						.format("select *"
								+ " from %s_passenger_preprocess where srvc_number = \'%s\' and direction = \'%d\' and bus_reg_num=\'%s\' "
								+ " and boarding_time > 10000 and alighting_time > 10000"
								+ " order by boarding_time, alighting_time", tripTableName,
								ptVehicle.cepasLine.lineId.toString(), ptVehicle.cepasRoute.direction,
								ptVehicle.vehicleId.toString().split("_")[2]);
				System.out.println(query);
				ResultSet resultSet = dba.executeQuery(query);
				ptVehicle.handlePassengers(resultSet);
				transactionsBeforeWriter.write(ptVehicle.printTransactions());
				ptVehicle.assignRouteScoresByNumberOfTransactions();
				ptVehicle.correctGPSErrors();
				transactionsAfterWriter.write(ptVehicle.printTransactions());
				ptVehicle.sortCepasTransactionsByStopId();
				ptVehicle.createDwellEventsFromTransactions();
				ptVehicle.dropDwellEventsNotInRoute();
				ptVehicle.clusterDwellEventsIntoRoutes();
				// ptVehicle.interpolateDwellEvents();

				clusterWriter.write(ptVehicle.printClusters());
				dwellEventWriter.write(ptVehicle.printStopsVisited());
				vehicles++;
				if (vehicles > 100)
					break;
			}
			transactionsBeforeWriter.close();
			transactionsAfterWriter.close();
			clusterWriter.close();
			dwellEventWriter.close();
		} catch (SQLException se) {

			String query = String
					.format("create table %s_board_alight_preprocess as select * from (select card_id, boarding_stop_stn as stop_id, "
							+ "(EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) as event_time,"
							+ "\'boarding\' as type,"
							+ "srvc_number, direction, bus_reg_num"
							+ " from %s "
							+ " union "
							+ "select card_id, alighting_stop_stn as stop_id, "
							+ "((EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) + (60 * ride_time))::INT AS event_time,"
							+ "\'alighting\' as type, "
							+ "srvc_number, direction, bus_reg_num"
							+ " from %s "
							+ " ) as prequery where event_time is not null order by srvc_number, direction, bus_reg_num, event_time;"
							+ "alter table %s_board_alight_preprocess add column idx serial;"
							+ "alter table %s_board_alight_preprocess add column deltatime int;"

					, tripTableName, tripTableName, tripTableName, tripTableName);
			dba.executeStatement(query);
			query = String
					.format("create table %s_passenger_preprocess as select card_id, boarding_stop_stn, alighting_stop_stn, (EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) as boarding_time,"
							+ "((EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) + (60 * ride_time))::INT AS alighting_time, "
							+ "srvc_number, direction, bus_reg_num"
							+ " from %s order by srvc_number, direction, bus_reg_num, boarding_time, alighting_time;"
							+ "alter table %s_passenger_preprocess add column idx serial;", tripTableName,
							tripTableName, tripTableName);
			dba.executeStatement(query);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * This goes through the Cepas lines and routes one by one and creates a
	 * unique vehicle assigned to each Cepas route. It also creates the list of
	 * Cepas lines on the fly from whatever records are contained in the Cepas
	 * transaction table. Each vehicle is put in a map, distinguished by
	 * line_route_busRegNum.
	 * <P>
	 * This method is recursive: if the supporting table doesn't exist, it
	 * creates it and call itself again, to make future calls faster.
	 * 
	 * @throws SQLException
	 * @throws NoConnectionException
	 */
	private void createVehiclesByCepasLineDirectionAndBusRegNum() throws SQLException, NoConnectionException {
		this.cepasLines = new HashMap<Id, CepasToEvents.CepasLine>();
		this.serviceTableName = this.tripTableName + "_services_by_vehicle";
		try {
			ResultSet resultSet = dba.executeQuery("select distinct srvc_number from " + this.serviceTableName
					+ " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				CepasLine cepasLine = new CepasLine(lineId);
				this.cepasLines.put(lineId, cepasLine);
			}
			resultSet = dba.executeQuery("select distinct srvc_number, direction from " + this.serviceTableName
					+ " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				CepasLine cepasLine = this.cepasLines.get(lineId);
				CepasRoute cepasRoute = new CepasRoute(resultSet.getInt(2), cepasLine);
				cepasLine.routes.put(resultSet.getInt(2), cepasRoute);
			}
			resultSet = dba.executeQuery("select distinct srvc_number, direction, bus_reg_num from "
					+ this.serviceTableName + " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				CepasLine cepasLine = this.cepasLines.get(lineId);
				CepasRoute cepasRoute = cepasLine.routes.get(resultSet.getInt(2));
				Id ptVehicleId = new IdImpl(lineId.toString() + "_" + resultSet.getInt(2) + "_"
						+ resultSet.getString(3));
				CepasVehicle ptVehicle = new CepasVehicle(lineId, cepasRoute, ptVehicleId);
				cepasRoute.vehicles.put(ptVehicleId, ptVehicle);
				this.cepasVehicles.put(ptVehicleId.toString(), ptVehicle);
			}

			System.out.println(this.cepasLines);
		} catch (SQLException se) {
			// necessary to create a summary table
			System.out.println("Indexing....");
			dba.executeUpdate("update " + this.tripTableName + " set srvc_number = trim(srvc_number);");
			dba.executeUpdate("create index " + tripTableName.split("\\.")[1] + "_idx on " + this.tripTableName
					+ "(srvc_number, direction, bus_reg_num)");
			dba.executeStatement("create table " + serviceTableName
					+ " as select distinct srvc_number, direction, bus_reg_num from " + this.tripTableName
					+ " where srvc_number is not null");
			createVehiclesByCepasLineDirectionAndBusRegNum();
			return;
		}

	}

	// static methods
	public static void main(String[] args) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SQLException, NoConnectionException {
		String databaseProperties = "f:/data/matsim2postgres.properties";
		String transitSchedule = "f:\\data\\sing2.2\\input\\transit\\transitSchedule.xml";
		String networkFile = "f:\\data\\sing2.2\\input\\network\\network100by4TL.xml.gz";
		String outputEventsFile = "f:\\data\\sing2.2\\cepasevents.xml";
		String tripTableName = "a_lta_ezlink_week.trips11042011";
		String stopLookupTableName = "d_ezlink2events.matsim2ezlink_stop_lookup";
		CepasToEvents cepasToEvents = new CepasToEvents(databaseProperties, transitSchedule, networkFile,
				outputEventsFile, tripTableName, stopLookupTableName);
		cepasToEvents.run();
	}

}
