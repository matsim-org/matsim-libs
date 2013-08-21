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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.hits.MyTransitRouterConfig;

public class EZLinkToEvents {
	// internal classes
	class ValueComparator implements Comparator<Id> {

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

	private class PTVehicle {
		class PtVehicleDwellEvent implements Comparable<PtVehicleDwellEvent> {
			private static final int deltaTapTimeLimit = 8;
			private static final int minDwellTime = 15;
			private static final int minClusterSize = 4;
			int arrivalTime;
			int departureTime;
			int dwellTime;
			PtVehicleDwellEvent nextDwellEvent;
			PtVehicleDwellEvent previousDwellEvent;
			Id stopId;
			TransitRoute assignedRoute;
			boolean routeAssignmentConfirmed = false;
			boolean interpolated = false;
			boolean routeStart;
			// if this stop is passed through without any boardings and
			// alightings,
			// its dwell time should be zero
			boolean passThrough = false;
			/*
			 * if the arrival event is triggered by alighting event, mark the
			 * stop event, and replace the arrival time by the first tap-in
			 * time, if any tap-ins occurred
			 */

			ArrayList<CEPASTransaction> transactions = new ArrayList<CEPASTransaction>();

			public int getDwellTime() {
				return departureTime - arrivalTime;
			}

			public PtVehicleDwellEvent(int arrivalTime, int departureTime, Id stopId) {
				super();
				this.arrivalTime = arrivalTime;
				this.departureTime = departureTime;
				this.stopId = stopId;
			}

			/**
			 * For cases where there are too few transactions to register a
			 * realistic dwell time, or where there are early tap-ins and
			 * tap-outs, or GPS errors.
			 */
			public void simpleDwellTimeAdjustment() {
				CEPASTransaction lastAlighting = null;
				CEPASTransaction firstBoarding = null;
				for (CEPASTransaction transaction : transactions) {
					if (transaction.type.equals(CEPASTransactionType.alighting))
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
				if (transactions.size() <= minClusterSize) {
					simpleDwellTimeAdjustment();
					return;
				}
				/*
				 * cluster the transactions based on the deltaTapTimeLimit
				 */
				int deltaTime = 0;
				int lastTime = transactions.get(0).time;

				ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
				deltaTimes.add(0);
				ArrayList<ArrayList<CEPASTransaction>> transactionClusters = new ArrayList<ArrayList<CEPASTransaction>>();
				for (int i = 1; i < transactions.size(); i++) {
					CEPASTransaction transaction = transactions.get(i);
					deltaTime = transaction.time - lastTime;
					deltaTimes.add(deltaTime);
					lastTime = transaction.time;
				}
				ArrayList<CEPASTransaction> transactionCluster = new ArrayList<CEPASTransaction>();
				transactionClusters.add(transactionCluster);
				for (int i = 0; i < deltaTimes.size(); i++) {
					deltaTime = deltaTimes.get(i);
					CEPASTransaction transaction = transactions.get(i);
					if (deltaTime < deltaTapTimeLimit || i == 0) {
						transactionCluster.add(transactions.get(i));
					} else {
						transactionCluster = new ArrayList<CEPASTransaction>();
						transactionCluster.add(transactions.get(i));
						transactionClusters.add(transactionCluster);
					}
				}
				// find the biggest cluster
				ArrayList<CEPASTransaction> targetCluster = null;
				int maxSize = 1;
				for (ArrayList<CEPASTransaction> cluster : transactionClusters) {
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
				for (CEPASTransaction transaction : transactions) {
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
			public int compareTo(PtVehicleDwellEvent o) {
				return (this.arrivalTime < o.arrivalTime) ? -1 : ((this.arrivalTime == o.arrivalTime) ? 0 : 1);
			}

		}

		class Passenger implements Comparable<Passenger> {
			Id personId;
			Id boardingStopId;
			PtVehicleDwellEvent boardingStopEvent;
			PtVehicleDwellEvent alightingStopEvent;
			Id alightingStopId;
			int boardingTime;
			int alightingTime;

			public Passenger(Id personId, Id boardingStopId, Id alightingStopId, int boardingTime, int alightingTime) {
				super();
				this.personId = personId;
				this.boardingStopId = boardingStopId;
				this.alightingStopId = alightingStopId;
				this.boardingTime = boardingTime;
				this.alightingTime = alightingTime;
			}

			@Override
			public int compareTo(Passenger o) {

				return this.personId.compareTo(o.personId);
			}
		}

		private class CEPASTransaction implements Comparable<CEPASTransaction> {
			Passenger passenger;

			public CEPASTransaction(Passenger passenger, Id stopId, CEPASTransactionType type, int time) {
				super();
				this.passenger = passenger;
				this.stopId = stopId;
				this.type = type;
				this.time = time;
			}

			Id stopId;
			CEPASTransactionType type;
			int time;

			@Override
			public int compareTo(CEPASTransaction o) {

				return ((Integer) this.time).compareTo(o.time);
			}
		}

		// when looking at transactions for each stop, the time required
		// between two consecutive transactions to recognize it as two separate
		// events
		private static final int interstopTimeLimit = 200;
		// Attributes
		// a matsim vehicle can only do one line & route at a time, but a
		// physical vehicle can switch lines and routes, so can't use the same
		// vehc
		Id vehicleId;
		Id transitLineId;
		EZLinkLine ezlinkLine;
		EZLinkRoute ezlinkRoute;
		Map<Id, TransitRoute> unsortedRoutes;
		TreeSet<Id> routesSortedBynumberOfStops;
		HashMap<Id, ArrayList<Id>> routeIdToStopIdSequence;
		TransitRoute currentRoute;
		boolean in = false;
		TreeMap<Integer, TreeSet<Passenger>> passengersbyAlightingTime = new TreeMap<Integer, TreeSet<Passenger>>();
		TreeMap<Integer, TreeSet<Passenger>> passengersbyBoardingTime = new TreeMap<Integer, TreeSet<Passenger>>();
		TreeMap<Integer, PtVehicleDwellEvent> orderedDwellEvents = new TreeMap<Integer, PtVehicleDwellEvent>();
		HashMap<Id, ArrayList<CEPASTransaction>> transactionTimesbyStopId = new HashMap<Id, ArrayList<CEPASTransaction>>();
		int passengerCount = 0;
		double distance;
		Id firstStop;
		Id lastStop;
		int linkEnterTime = 0;
		private TreeSet<Id> routesSortedByNumberOfTransactions;

		// Constructors
		public PTVehicle(Id transitLineId, EZLinkRoute ezLinkRoute, Id busRegNumber) {
			this.transitLineId = transitLineId;
			this.ezlinkRoute = ezLinkRoute;
			this.ezlinkLine = ezLinkRoute.line;
			this.vehicleId = busRegNumber;
			try {
				unsortedRoutes = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes();

			} catch (NullPointerException ne) {
				// this route doesn't exist in the transit schedule
				// TODO write an exception handler for this case,
				// for now, just ignore these and report their number of events
				System.out.println("line " + transitLineId.toString() + " does not exist in transit schedule.");
				return;
			}
			createFilteredRouteSelection();
		}

		/**
		 * orders the possible routes for this vehicle in ascending number of
		 * stops. if a stop requested by a transaction doesn't appear in the
		 * first route, pop that route element and evaluate all the stops
		 * visited so far to see if they appear in the next route.
		 * 
		 * <P>
		 * continue until only one remains. if the stop doesn't appear in this
		 * final encapsulating route, ignore the transaction and write it to the
		 * log as an exception, but stick to the last route as the route to use
		 */
		private void createFilteredRouteSelection() {
			HashMap<Id, Integer> unsortedRouteSizes = new HashMap<Id, Integer>();
			for (Id transitRouteId : unsortedRoutes.keySet()) {
				unsortedRouteSizes.put(transitRouteId, unsortedRoutes.get(transitRouteId).getStops().size());
			}
			this.routesSortedBynumberOfStops = new TreeSet<Id>(new ValueComparator(unsortedRouteSizes));
			this.routesSortedBynumberOfStops.addAll(unsortedRouteSizes.keySet());
			// add all the stop facility ids so we dont need to do this when we
			// repeatedly iterate through the ids
			
			routeIdToStopIdSequence = new HashMap<Id, ArrayList<Id>>();
			for (Id routeId : routesSortedBynumberOfStops) {
				ArrayList<Id> stopIds = new ArrayList<Id>();
				ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
				stops.addAll(this.unsortedRoutes.get(routeId).getStops());
				for (TransitRouteStop trStop : stops) {
					stopIds.add(trStop.getStopFacility().getId());
				}
				routeIdToStopIdSequence.put(routeId, stopIds);
			}
		}

		// Methods
		public String printStopsVisited() {
			StringBuffer sb = new StringBuffer("line\tdirection\tbus_reg_num\tstop_id\ttime\ttype\tspeed\n");
			try{
				PtVehicleDwellEvent prevStop = orderedDwellEvents.firstEntry().getValue();
				for (Entry<Integer, PtVehicleDwellEvent> entry : orderedDwellEvents.entrySet()) {
					PtVehicleDwellEvent stopEvent = entry.getValue();
					sb.append(String.format("%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n", 
							this.ezlinkLine.lineId.toString(),this.ezlinkRoute.direction,this.vehicleId.toString(),
							stopEvent.stopId,
							stopEvent.arrivalTime, getInterStopSpeed(prevStop, stopEvent), 
							this.ezlinkLine.lineId.toString(),this.ezlinkRoute.direction,this.vehicleId.toString(),
							stopEvent.stopId,
							stopEvent.departureTime, 0.0));
					prevStop = stopEvent;
				}
				
			}catch(NullPointerException ne){
				sb.append(String.format("%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n", 
						this.ezlinkLine.lineId.toString(),this.ezlinkRoute.direction,this.vehicleId.toString(),
						"NO DATA",
						-1,-1.0, 
						this.ezlinkLine.lineId.toString(),this.ezlinkRoute.direction,this.vehicleId.toString(),
						"NO DATA",
						-1,-1.0));
			}

			return sb.toString();
		}

		public void handlePassengers(ResultSet resultSet) throws SQLException {
			while (resultSet.next()) {
				Passenger passenger;
				int boardingTime = resultSet.getInt("boarding_time");
				Id boardingStop;
				try {
					boardingStop = ezLinkStoptoMatsimStopLookup.get(resultSet.getString("boarding_stop_stn"));
				} catch (NullPointerException e) {
					// stop is not in the schedule, skip this
					// guy
					continue;
				}
				Id alightingStop = ezLinkStoptoMatsimStopLookup.get(resultSet.getString("alighting_stop_stn"));
				if (alightingStop == null) {
					// didn't tap out, or stop is not in the schedule, skip this
					// guy{
					try {
						this.transactionTimesbyStopId.get(boardingStop).add(
								new CEPASTransaction(null, boardingStop, CEPASTransactionType.boarding, boardingTime));
					} catch (NullPointerException ne) {
						this.transactionTimesbyStopId.put(boardingStop, new ArrayList<CEPASTransaction>());
						this.transactionTimesbyStopId.get(boardingStop).add(
								new CEPASTransaction(null, boardingStop, CEPASTransactionType.boarding, boardingTime));
					}
					continue;
				}
				int alightingTime = resultSet.getInt("alighting_time");
				Id personId = new IdImpl(resultSet.getLong("card_id"));
				passenger = new Passenger(personId, boardingStop, alightingStop, boardingTime, alightingTime);
				try {
					this.transactionTimesbyStopId.get(boardingStop).add(
							new CEPASTransaction(passenger, boardingStop, CEPASTransactionType.boarding, boardingTime));
				} catch (NullPointerException ne) {
					this.transactionTimesbyStopId.put(boardingStop, new ArrayList<CEPASTransaction>());
					this.transactionTimesbyStopId.get(boardingStop).add(
							new CEPASTransaction(passenger, boardingStop, CEPASTransactionType.boarding, boardingTime));
				}
				try {
					this.transactionTimesbyStopId.get(alightingStop).add(
							new CEPASTransaction(passenger, alightingStop, CEPASTransactionType.alighting,
									alightingTime));
				} catch (NullPointerException ne) {
					this.transactionTimesbyStopId.put(alightingStop, new ArrayList<CEPASTransaction>());
					this.transactionTimesbyStopId.get(alightingStop).add(
							new CEPASTransaction(passenger, alightingStop, CEPASTransactionType.alighting,
									alightingTime));
				}
				// find the stop visited at a time less than or equal to
				// boarding and alighting time
				try {
					TreeSet<Passenger> passengersForAlightingTime = this.passengersbyAlightingTime.get(alightingTime);
					passengersForAlightingTime.add(passenger);
				} catch (NullPointerException ne) {
					TreeSet<Passenger> passengersForAlightingTime = new TreeSet<Passenger>();
					passengersForAlightingTime.add(passenger);
					this.passengersbyAlightingTime.put(alightingTime, passengersForAlightingTime);
				}
				try {
					TreeSet<Passenger> passengersForBoardingTime = this.passengersbyBoardingTime.get(boardingTime);
					passengersForBoardingTime.add(passenger);
				} catch (NullPointerException ne) {
					TreeSet<Passenger> passengersForBoardingTime = new TreeSet<Passenger>();
					passengersForBoardingTime.add(passenger);
					this.passengersbyBoardingTime.put(boardingTime, passengersForBoardingTime);
				}
			}
		}

		public void createDwellEventsFromTransactions() {
			for (Id stopId : this.transactionTimesbyStopId.keySet()) {
				ArrayList<CEPASTransaction> transactions = this.transactionTimesbyStopId.get(stopId);
				Collections.sort(transactions);
				int deltaTime = 0;
				int lastTime = 0;
				ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
				for (int i = 0; i < transactions.size(); i++) {
					CEPASTransaction transaction = transactions.get(i);
					deltaTime = transaction.time - lastTime;
					deltaTimes.add(deltaTime);
					lastTime = transaction.time;
				}
				ArrayList<PtVehicleDwellEvent> dwellEvents = new ArrayList<PtVehicleDwellEvent>();
				PtVehicleDwellEvent dwellEvent = null;
				for (int i = 0; i < deltaTimes.size(); i++) {
					deltaTime = deltaTimes.get(i);
					CEPASTransaction transaction = transactions.get(i);
					if (deltaTime > interstopTimeLimit || i == 0) {
						if (dwellEvent != null) {
							dwellEvents.add(dwellEvent);
							dwellEvent.findTrueDwellTime();
						}
						dwellEvent = new PtVehicleDwellEvent(transaction.time, transaction.time, stopId);
						dwellEvent.transactions.add(transaction);
					} else {
						dwellEvent.transactions.add(transaction);
						dwellEvent.departureTime = transaction.time;
					}
				}
				for (PtVehicleDwellEvent stopEvent1 : dwellEvents) {
					this.orderedDwellEvents.put(stopEvent1.arrivalTime, stopEvent1);
				}

			}
			System.out.println(this.printStopsVisited());
			// assignDwellEventsToRoutes();
			// adjustDwellEventTimingsAccordingToInterStopSpeeds();
		}

		private void adjustDwellEventTimingsAccordingToInterStopSpeeds() {
			// TODO Auto-generated method stub

		}
		
		/**
		 * every possible route gets a score for the number of transactions registering on it
		 */
		private void assignRouteScoresByNumberOfTransactions(){
			HashMap<Id, Integer> correlationCount = new HashMap<Id, Integer>();
			for(Id routeId:this.routeIdToStopIdSequence.keySet()){	
				ArrayList<Id> stopList = routeIdToStopIdSequence.get(routeId);
				int score=0;
				for(Id stopId:transactionTimesbyStopId.keySet()){
					if(stopList.contains(stopId))
						score++;
				}
				correlationCount.put(routeId, score);
			}
			routesSortedByNumberOfTransactions = new TreeSet<Id>(new ValueComparator(correlationCount));
			this.routesSortedByNumberOfTransactions.addAll(correlationCount.keySet());
		}

		/**
		 * method needs to check for GPS errors where adjacent stops cause dwell
		 * events to appear to be in the wrong order, else they'll be assigned
		 * to different routes
		 */
		private void assignDwellEventsToRoutes() {
			// start with the shortest route
			ArrayList<PtVehicleDwellEvent> dwellEventsList = new ArrayList<PtVehicleDwellEvent>();
			dwellEventsList.addAll(this.orderedDwellEvents.values());
			while (!allDwellEventsAssignedToRoutes()) {
				int dwellEvtIdx;
				boolean startOfRoute = true;
				// find the first dwell event in a new route, work from there
				FIND_ROUTE_START: for (dwellEvtIdx = 0; dwellEvtIdx < dwellEventsList.size(); dwellEvtIdx++) {
					if (dwellEventsList.get(dwellEvtIdx).routeAssignmentConfirmed)
						continue FIND_ROUTE_START;
				}
				int routeStart = dwellEvtIdx;
				// go through the routes in order from the shortest to the
				// longest, and check if the stops appear in order
				while (dwellEvtIdx < dwellEventsList.size()) {
					for (Id rtId : this.routesSortedBynumberOfStops) {
						TransitRoute route = this.unsortedRoutes.get(rtId);
						int j = 0;
						ArrayList<Id> stopList = routeIdToStopIdSequence.get(rtId);
						TRANSITSTOPS: while (j < stopList.size()) {
							// if(dwellEventsList.get(dwellEvtIdx).stopId)
							j++;
						}
					}
					dwellEvtIdx++;
				}
			}
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
			List<TransitRouteStop> transitStops = unsortedRoutes.get(this.routesSortedByNumberOfTransactions.last())
					.getStops();
			ArrayList<Id> stopList = routeIdToStopIdSequence.get(this.routesSortedByNumberOfTransactions.last());
			ArrayList<PtVehicleDwellEvent> dwellEventsList = new ArrayList<PtVehicleDwellEvent>();
			dwellEventsList.addAll(this.orderedDwellEvents.values());
			ITERATE_DWELL_EVENTS: while (!allDwellEventsInterpolated()) {
				// find the first dwell event in a new route, work from there

				if (dwellEventsList.get(dwellEvtIdx).interpolated) {
					dwellEvtIdx++;
					continue ITERATE_DWELL_EVENTS;
				}

				PtVehicleDwellEvent origDwellEvent = dwellEventsList.get(dwellEvtIdx);
				PtVehicleDwellEvent destDwellEvent = null;
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
				TRANSITSTOPS: while (origStopIdx < stopList.size() && destStopIdx < stopList.size()) {
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
								PtVehicleDwellEvent dwellEvent = new PtVehicleDwellEvent(dwellTime, dwellTime,
										stopsToVisit.get(i));
								this.orderedDwellEvents.put(dwellTime, dwellEvent);
								dwellEvent.interpolated = true;
							}
							// all done, continue from the destination
							origDwellEvent.interpolated = true;
							origStopIdx = destStopIdx;
							destStopIdx++;
							dwellEventsList = new ArrayList<PtVehicleDwellEvent>();
							dwellEventsList.addAll(this.orderedDwellEvents.values());
							continue ITERATE_DWELL_EVENTS;
						}
					}
				}
			}
			System.out.println(this.printStopsVisited());
		}

		private boolean allDwellEventsAssignedToRoutes() {
			for (PtVehicleDwellEvent dwellEvent : this.orderedDwellEvents.values()) {
				if (dwellEvent.assignedRoute == null || !dwellEvent.routeAssignmentConfirmed)
					return false;
			}
			return true;
		}

		private boolean allDwellEventsInterpolated() {
			for (PtVehicleDwellEvent dwellEvent : this.orderedDwellEvents.values()) {
				if (dwellEvent.assignedRoute == null || !dwellEvent.routeAssignmentConfirmed)
					return false;
			}
			return true;
		}

		// public void determineStopsAndHandleRoutes(ResultSet resultSet) throws
		// SQLException {
		// while (resultSet.next()) {
		// Passenger passenger;
		// int time = resultSet.getInt("event_time");
		// Id stopId;
		// try {
		// String stoptext = resultSet.getString("stop_id");
		// stopId = ezLinkStoptoMatsimStopLookup.get(stoptext);
		// } catch (NullPointerException e) {
		// // stop is not in the schedule, skip this
		// // guy
		// System.out.println("stop " + resultSet.getString("stop_id") +
		// " not in the schedule for bus "
		// + this.toString());
		// continue;
		// }
		// try {
		// PtVehicleDwellEvent candidateStopEvent =
		// this.orderedDwellEvents.floorEntry(time).getValue();
		// if (!candidateStopEvent.stopId.equals(stopId)) {
		// PtVehicleDwellEvent stopEvent = new PtVehicleDwellEvent(time, time,
		// stopId);
		// double interStopSpeed = getInterStopSpeed(candidateStopEvent,
		// stopEvent);
		// // if the speed between events is faster than
		// if (interStopSpeed <= 80) {
		// this.orderedDwellEvents.put(time, stopEvent);
		// } else {
		// System.err.println(stopEvent.toString());
		// }
		// } else {
		// candidateStopEvent.departureTime = Math.max(time,
		// candidateStopEvent.departureTime);
		// }
		// } catch (NullPointerException ne) {
		// this.orderedDwellEvents.put(time, new PtVehicleDwellEvent(time, time,
		// stopId));
		// }
		// }
		// System.out.println(this.printStopsVisited());
		// System.out.println("\n\n\n");
		// eliminateDoubleStopEntries();
		// System.out.println(this.printStopsVisited());
		// assignStopsVisitedToRoutes();
		// }

		private double getInterStopSpeed(PtVehicleDwellEvent previousStopEvent, PtVehicleDwellEvent nextStopEvent) {
			double distance = getInterStopDistance(previousStopEvent.stopId, nextStopEvent.stopId);
			double time = nextStopEvent.arrivalTime - previousStopEvent.departureTime;
			double speed=distance / time * 3.6;
			if(speed<0){
				return -1;
			}else{
				
				return Math.min(speed,1000);
			}
		}

		private double getInterStopDistance(Id stopId, Id stopId2) {
			Id likeliestRoute = this.routesSortedByNumberOfTransactions.last();
			List<TransitRouteStop> stops = this.unsortedRoutes.get(likeliestRoute).getStops();
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
			else
				return shortestPathCalculator
						.calcLeastCostPath(fromLink.getToNode(), toLink.getToNode(), 0, null, null).travelCost;

		}

		private double getInterStopExpectedTime(Id stopId, Id stopId2) {
			Id longestRoute = this.routesSortedBynumberOfStops.last();
			List<TransitRouteStop> stops = this.unsortedRoutes.get(longestRoute).getStops();
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
			else
				return shortestPathCalculator
						.calcLeastCostPath(fromLink.getToNode(), toLink.getToNode(), 0, null, null).travelTime;

		}





		@Override
		public String toString() {
			String out = String.format("line %s, bus reg %s", this.transitLineId.toString(), this.vehicleId.toString());
			return out;
		}

	}

	private class EZLinkRoute {
		public EZLinkRoute(int direction, EZLinkLine ezLinkLine) {
			super();
			this.direction = direction;
			this.line = ezLinkLine;
		}

		int direction;
		EZLinkLine line;
		HashMap<Id, PTVehicle> buses = new HashMap<Id, EZLinkToEvents.PTVehicle>();

		public String toString() {
			return (line.lineId.toString() + " : " + direction + " : " + buses.keySet() + "\n");
		}
	}

	private class EZLinkLine {
		public EZLinkLine(Id lineId) {
			super();
			this.lineId = lineId;
		}

		Id lineId;
		HashMap<Integer, EZLinkRoute> routes = new HashMap<Integer, EZLinkToEvents.EZLinkRoute>();

		public String toString() {
			return (routes.values().toString());
		}
	}

	enum CEPASTransactionType {
		boarding, alighting
	}

	// fields
	DataBaseAdmin dba;
	Scenario scenario;
	String outputEventsFile;
	String stopLookupTableName;
	String tripTableName;
	Queue<Event> eventQueue;
	EventsManager eventsManager;
	private HashMap<Id, EZLinkLine> ezlinkLines;
	private HashMap<String, PTVehicle> ptVehicles = new HashMap<String, EZLinkToEvents.PTVehicle>();
	private String serviceTableName;
	int eventTimeIndex = 0;
	int[] eventTimes;
	HashMap<String, Id> ezLinkStoptoMatsimStopLookup = new HashMap<String, Id>();
	private Dijkstra shortestPathCalculator;
	private TransitRouterImpl transitScheduleRouter;
	private String routeLookupTableName;

	// constructors
	public EZLinkToEvents(String databaseProperties, String transitSchedule, String networkFile,
			String outputEventsFile, String tripTableName, String lookupTableName) throws InstantiationException,
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
		this.stopLookupTableName = lookupTableName;
		TravelDisutility travelMinCost = new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength();
			}
		};
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(scenario.getNetwork());
		TravelTime timeFunction = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}
		};
		shortestPathCalculator = new Dijkstra(scenario.getNetwork(), travelMinCost, timeFunction, preProcessData);

		// find paths through the transit network
		// MyTransitRouterConfig transitRouterConfig = new
		// MyTransitRouterConfig(scenario.getConfig().planCalcScore(),
		// scenario.getConfig().plansCalcRoute(),
		// scenario.getConfig().transitRouter(), scenario.getConfig()
		// .vspExperimental());
		// PreparedTransitSchedule preparedTransitSchedule = new
		// PreparedTransitSchedule(scenario.getTransitSchedule());
		// TransitRouterNetwork transitScheduleRouterNetwork =
		// TransitRouterNetwork.createFromSchedule(
		// scenario.getTransitSchedule(),
		// transitRouterConfig.beelineWalkConnectionDistance);
		// TransitRouterNetworkTravelTimeAndDisutility
		// routerNetworkTravelTimeAndDisutility = new
		// TransitRouterNetworkTravelTimeAndDisutility(
		// transitRouterConfig, preparedTransitSchedule);
		// transitScheduleRouter = new TransitRouterImpl(transitRouterConfig,
		// preparedTransitSchedule,
		// transitScheduleRouterNetwork, routerNetworkTravelTimeAndDisutility,
		// routerNetworkTravelTimeAndDisutility);
	}

	// non-static public methods
	public void processQueue() {
		for (Event event : eventQueue) {
			eventsManager.processEvent(event);
		}
	}

	public void run() throws SQLException, NoConnectionException {
		createVehiclesByEZLinkLineDirectionAndBusRegNum();
		createStopLookupTable();
		getEventSeconds();
		// processTimes();
		System.out.println(new Date());
		processLines();
		System.out.println(new Date());
	}

	private void createStopLookupTable() throws SQLException, NoConnectionException {
		ResultSet resultSet = dba.executeQuery("select *  from " + this.stopLookupTableName
				+ " where matsim_stop is not null and ezlink_stop is not null");
		while (resultSet.next()) {
			String ezlinkid = resultSet.getString("ezlink_stop");
			Id matsimid = new IdImpl(resultSet.getString("matsim_stop"));
			this.ezLinkStoptoMatsimStopLookup.put(ezlinkid, matsimid);
		}

	}

	private void processLines() throws SQLException, NoConnectionException {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("scripts/speeds.csv"));
			int vehicles = 0;
			for (PTVehicle ptVehicle : this.ptVehicles.values()) {
				// TODO: if we don't have this transit line in the schedule,
				// ignore
//				if (!ptVehicle.vehicleId.toString().equals("8819"))
//					continue;
//				if (!ptVehicle.vehicleId.toString().equals("8225"))
//				continue;
				if (ptVehicle.unsortedRoutes == null)
					continue;
				// line 812 does a double loop on itself, currently can't cope
				// with
				// it
				if (ptVehicle.ezlinkLine.lineId.toString().equals("812"))
					continue;
				// String query = String
				// .format("select * "
				// +
				// " from %s_board_alight_preprocess where srvc_number = \'%s\' and direction = \'%d\' and bus_reg_num=\'%s\' "
				// + " order by stop_id, event_time", tripTableName,
				// ptVehicle.ezlinkLine.lineId.toString(),
				// ptVehicle.ezlinkRoute.direction,
				// ptVehicle.vehicleId.toString());
				// ResultSet resultSet = dba.executeQuery(query);
				// ptVehicle.determineStopsAndHandleRoutes(resultSet);
				String query = String
						.format("select *"
								+ " from %s_passenger_preprocess where srvc_number = \'%s\' and direction = \'%d\' and bus_reg_num=\'%s\' "
								+ " and boarding_time > 10000 and alighting_time >10000"
								+ " order by boarding_time, alighting_time",
								tripTableName, ptVehicle.ezlinkLine.lineId.toString(), ptVehicle.ezlinkRoute.direction,
								ptVehicle.vehicleId.toString());
				System.out.println(query);
				ResultSet resultSet = dba.executeQuery(query);
				ptVehicle.handlePassengers(resultSet);
				ptVehicle.assignRouteScoresByNumberOfTransactions();
				ptVehicle.createDwellEventsFromTransactions();
				// ptVehicle.interpolateDwellEvents();

				writer.write(ptVehicle.printStopsVisited());
				vehicles++;
				if(vehicles>1000)
					break;
			}
			writer.close();
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private void processTimes() throws SQLException, NoConnectionException {
		while (this.eventTimeIndex < this.eventTimes.length) {
			ResultSet boardings = dba.executeQuery("select * from " + tripTableName + " where ride_start_time = \'"
					+ Time.writeTime((double) eventTimes[eventTimeIndex], ":") + "\'");
			processBoardings(boardings);
		}

	}

	private void processBoardings(ResultSet boardings) {
		// TODO Auto-generated method stub

	}

	private void getEventSeconds() throws SQLException, NoConnectionException {
		// create event times table if it doesn't exist

		try {
			ResultSet resultSet = this.dba.executeQuery("select count(*) from " + tripTableName + "_event_times;");
			resultSet.next();
			this.eventTimes = new int[resultSet.getInt(1)];
			resultSet = this.dba.executeQuery("select * from " + tripTableName + "_event_times;");
			int i = 0;
			while (resultSet.next()) {
				this.eventTimes[i] = resultSet.getInt(1);
				i++;
			}
			System.out.println(this.eventTimes);

		} catch (SQLException se) {
			this.dba.executeStatement("create table "
					+ tripTableName
					+ "_event_times AS "
					+ "SELECT DISTINCT time_in_seconds FROM "
					+ "(SELECT "
					+ "(EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) AS time_in_seconds	"
					+ " FROM a_lta_ezlink_week.trips11042011	"
					+ "UNION ALL	"
					+ "SELECT ((EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) + (60 * ride_time))::INT AS time_in_seconds	"
					+ "FROM a_lta_ezlink_week.trips11042011	) AS j "
					+ "WHERE time_in_seconds IS NOT NULL ORDER BY time_in_seconds");
			getEventSeconds();
			return;
		}
	}

	/**
	 * @param trimServiceNumber
	 *            - sometimes the service number needs to be trimmed of
	 *            whitespace. only needs to be done once.
	 * @throws SQLException
	 * @throws NoConnectionException
	 */
	private void createVehiclesByEZLinkLineDirectionAndBusRegNum() throws SQLException, NoConnectionException {
		this.ezlinkLines = new HashMap<Id, EZLinkToEvents.EZLinkLine>();
		this.serviceTableName = this.tripTableName + "_services_by_vehicle";
		try {
			ResultSet resultSet = dba.executeQuery("select distinct srvc_number from " + this.serviceTableName
					+ " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				EZLinkLine ezLinkLine = new EZLinkLine(lineId);
				this.ezlinkLines.put(lineId, ezLinkLine);
			}
			resultSet = dba.executeQuery("select distinct srvc_number, direction from " + this.serviceTableName
					+ " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				EZLinkLine ezLinkLine = this.ezlinkLines.get(lineId);
				EZLinkRoute ezLinkRoute = new EZLinkRoute(resultSet.getInt(2), ezLinkLine);
				ezLinkLine.routes.put(resultSet.getInt(2), ezLinkRoute);
			}
			resultSet = dba.executeQuery("select distinct srvc_number, direction, bus_reg_num from "
					+ this.serviceTableName + " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				EZLinkLine ezLinkLine = this.ezlinkLines.get(lineId);
				EZLinkRoute ezLinkRoute = ezLinkLine.routes.get(resultSet.getInt(2));
				Id ptVehicleId = new IdImpl(resultSet.getString(3));
				PTVehicle ptVehicle = new PTVehicle(lineId, ezLinkRoute, ptVehicleId);
				ezLinkRoute.buses.put(ptVehicleId, ptVehicle);
				this.ptVehicles.put(ptVehicleId.toString(), ptVehicle);
			}

			System.out.println(this.ezlinkLines);
		} catch (SQLException se) {
			// necessary to create a summary table
			System.out.println("Indexing....");
			dba.executeUpdate("update " + this.tripTableName + " set srvc_number = trim(srvc_number);");
			dba.executeUpdate("create index " + tripTableName.split("\\.")[1] + "_idx on " + this.tripTableName
					+ "(srvc_number, direction, bus_reg_num)");
			dba.executeStatement("create table " + serviceTableName
					+ " as select distinct srvc_number, direction, bus_reg_num from " + this.tripTableName
					+ " where srvc_number is not null");
			createVehiclesByEZLinkLineDirectionAndBusRegNum();
			return;
		}

	}

	// static methods
	public static void main(String[] args) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SQLException, NoConnectionException {
		String databaseProperties = "f:/data/matsim2postgres.properties";
		String transitSchedule = "F:\\data\\sing2.2\\input\\transit\\transitSchedule.xml";
		String networkFile = "F:\\data\\sing2.2\\input\\network\\network100by4TL.xml.gz";
		String outputEventsFile = "F:\\data\\sing2.2\\ezlinkevents.xml";
		String tripTableName = "a_lta_ezlink_week.trips11042011";
		String stopLookupTableName = "d_ezlink2events.matsim2ezlink_stop_lookup";
		EZLinkToEvents ezLinkToEvents = new EZLinkToEvents(databaseProperties, transitSchedule, networkFile,
				outputEventsFile, tripTableName, stopLookupTableName);
		ezLinkToEvents.run();
	}

}
