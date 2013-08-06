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

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

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
		class Passenger implements Comparable<Passenger> {
			Id personId;
			Id boardingStopId;
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
		class CEPASTransaction implements Comparable<CEPASTransaction>{
			Id personId;
			public CEPASTransaction(Id personId, Id stopId, String type, int time) {
				super();
				this.personId = personId;
				this.stopId = stopId;
				this.type = type;
				this.time = time;
			}

			Id stopId;			
			String type;
			int time;

			@Override
			public int compareTo(CEPASTransaction o) {
				
				return ((Integer)this.time).compareTo(o.time);
			}
		}
		class StopEvent {
			int arrivalTime;
			int departureTime;
			Id stopId;
			//if the arrial event is triggered by alighting event, mark the stop event, and replace the arrival 
			//time by the first tap-in time, if any tap-ins occurred
			boolean arrivalTriggeredbyAlighting=true;
			
			public StopEvent(int arrivalTime, int departureTime, Id stopId) {
				super();
				this.arrivalTime = arrivalTime;
				this.departureTime = departureTime;
				this.stopId = stopId;
			}

			@Override
			public String toString() {

				return "stop: " + stopId.toString() + ", time: " + arrivalTime + " - " + departureTime;
			}

		}

		// Attributes
		// a matsim vehicle can only do one line & route at a time, but a
		// physical vehicle can switch lines and routes, so can't use the same
		// vehc
		Id vehicleId;
		Id transitLineId;
		EZLinkLine ezlinkLine;
		EZLinkRoute ezlinkRoute;
		Map<Id, TransitRoute> possibleRoutes;
		TreeSet<Id> filteredRouteSelection;
		TransitRoute currentRoute;
		boolean in = false;
		TreeMap<Integer, TreeSet<Passenger>> passengersbyAlightingTime = new TreeMap<Integer, TreeSet<Passenger>>();
		TreeMap<Integer, TreeSet<Passenger>> passengersbyBoardingTime = new TreeMap<Integer, TreeSet<Passenger>>();
		TreeMap<Integer, StopEvent> stopsVisited = new TreeMap<Integer, StopEvent>();
		HashMap<Id,ArrayList<CEPASTransaction>> transactionTimesbyStopId = new HashMap<Id, ArrayList<CEPASTransaction>>();
		int passengerCount = 0;
		double distance;
		Id firstStop;
		Id lastStop;
		int linkEnterTime = 0;

		// Constructors
		public PTVehicle(Id transitLineId, EZLinkRoute ezLinkRoute, Id busRegNumber) {
			this.transitLineId = transitLineId;
			this.ezlinkRoute = ezLinkRoute;
			this.ezlinkLine = ezLinkRoute.line;
			this.vehicleId = busRegNumber;
			try {
				possibleRoutes = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes();

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
			for (Id transitRouteId : possibleRoutes.keySet()) {
				unsortedRouteSizes.put(transitRouteId, possibleRoutes.get(transitRouteId).getStops().size());
			}
			this.filteredRouteSelection = new TreeSet<Id>(new ValueComparator(unsortedRouteSizes));
			this.filteredRouteSelection.addAll(unsortedRouteSizes.keySet());
		}

		// Methods
		public String printStopsVisited() {
			StringBuffer sb = new StringBuffer();
			for (Entry<Integer, StopEvent> entry : stopsVisited.entrySet()) {
				StopEvent stopEvent = entry.getValue();
				sb.append(String.format("%06d --- %06d : %s\n", stopEvent.arrivalTime, stopEvent.departureTime,
						stopEvent.stopId));
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
				if (alightingStop == null)
					// didn't tap out, or stop is not in the schedule, skip this
					// guy
					continue;
				int alightingTime = resultSet.getInt("alighting_time");
				Id personId = new IdImpl(resultSet.getLong("card_id"));
				passenger = new Passenger(personId, boardingStop, alightingStop, boardingTime, alightingTime);
				// find the stop visited at a time less than or equal to
				// boarding and alighting time
				try {
					TreeSet<Passenger> passengersForAlightingTime = this.passengersbyAlightingTime.get(alightingTime);
					passengersForAlightingTime.add(passenger);
				} catch (NullPointerException ne) {
					TreeSet<Passenger> passengersForAlightingTime = new TreeSet<EZLinkToEvents.PTVehicle.Passenger>();
					passengersForAlightingTime.add(passenger);
					this.passengersbyAlightingTime.put(alightingTime, passengersForAlightingTime);
				}
				try {
					TreeSet<Passenger> passengersForBoardingTime = this.passengersbyBoardingTime.get(boardingTime);
					passengersForBoardingTime.add(passenger);
				} catch (NullPointerException ne) {
					TreeSet<Passenger> passengersForBoardingTime = new TreeSet<EZLinkToEvents.PTVehicle.Passenger>();
					passengersForBoardingTime.add(passenger);
					this.passengersbyBoardingTime.put(boardingTime, passengersForBoardingTime);
				}
			}
		}

		
		public void determineStopsAndHandleRoutes(ResultSet resultSet) throws SQLException {
			while (resultSet.next()) {
				Passenger passenger;
				int time = resultSet.getInt("event_time");
				Id stopId;
				try {
					String stoptext = resultSet.getString("stop_id");
					stopId = ezLinkStoptoMatsimStopLookup.get(stoptext);
				} catch (NullPointerException e) {
					// stop is not in the schedule, skip this
					// guy
					System.out.println("stop " + resultSet.getString("stop_id") + " not in the schedule for bus "
							+ this.toString());
					continue;
				}
				try {
					StopEvent candidateStopEvent = this.stopsVisited.floorEntry(time).getValue();
					if (!candidateStopEvent.stopId.equals(stopId)) {
						StopEvent stopEvent = new StopEvent(time, time, stopId);
						double interStopSpeed = getInterStopSpeed(candidateStopEvent, stopEvent);
						// if the speed between events is faster than
						if (interStopSpeed <= 80) {
							this.stopsVisited.put(time, stopEvent);
						}else{
							System.err.println(stopEvent.toString());
						}
					} else {
						candidateStopEvent.departureTime = Math.max(time, candidateStopEvent.departureTime);
					}
				} catch (NullPointerException ne) {
					this.stopsVisited.put(time, new StopEvent(time, time, stopId));
				}
			}
			System.out.println(this.printStopsVisited());
			System.out.println("\n\n\n");
			eliminateDoubleStopEntries();
			System.out.println(this.printStopsVisited());
			assignStopsVisitedToRoutes();
		}

		private double getInterStopSpeed(StopEvent previousStopEvent, StopEvent nextStopEvent) {
			double distance = getInterStopDistance(previousStopEvent.stopId, nextStopEvent.stopId);
			double time = nextStopEvent.arrivalTime - previousStopEvent.departureTime;
			return distance / time * 3.6;
		}

		private double getInterStopDistance(Id stopId, Id stopId2) {
			Id longestRoute = this.filteredRouteSelection.last();
			List<TransitRouteStop> stops = this.possibleRoutes.get(longestRoute).getStops();
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

		private void assignStopsVisitedToRoutes() {
			indexSimilarities();
		}

		private void indexSimilarities() {

		}

		private void eliminateDoubleStopEntries() {
			Entry<Integer, StopEvent> lastEntry = null;
			ArrayList<Integer> entriesToDrop = new ArrayList<Integer>();
			for (Entry<Integer, StopEvent> entry : this.stopsVisited.entrySet()) {
				if (lastEntry == null) {
					lastEntry = entry;
					continue;
				}
				if (entry.getValue().stopId.equals(lastEntry.getValue().stopId)) {
					entriesToDrop.add(entry.getKey());
				} else {
					lastEntry = entry;
				}
			}
			for (int i : entriesToDrop) {
				this.stopsVisited.remove(i);
			}
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
		for (PTVehicle ptVehicle : this.ptVehicles.values()) {
			// TODO: if we don't have this transit line in the schedule, ignore
			if (!ptVehicle.vehicleId.toString().equals("8819"))
				continue;
			if (ptVehicle.possibleRoutes == null)
				continue;
			try {
				String query = String
						.format("select * "
								+ " from %s_board_alight_preprocess where srvc_number = \'%s\' and direction = \'%d\' and bus_reg_num=\'%s\' "
								+ " order by stop_id, event_time", tripTableName, ptVehicle.ezlinkLine.lineId.toString(),
								ptVehicle.ezlinkRoute.direction, ptVehicle.vehicleId.toString());
				ResultSet resultSet = dba.executeQuery(query);
				ptVehicle.determineStopsAndHandleRoutes(resultSet);
				query = String
						.format("select *"
								+ " from %s_passenger_preprocess where srvc_number = \'%s\' and direction = \'%d\' and bus_reg_num=\'%s\' order by boarding_time, alighting_time",
								tripTableName, ptVehicle.ezlinkLine.lineId.toString(), ptVehicle.ezlinkRoute.direction,
								ptVehicle.vehicleId.toString());
				resultSet = dba.executeQuery(query);
				ptVehicle.handlePassengers(resultSet);

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
								
								,
								tripTableName, tripTableName, tripTableName,tripTableName);
				dba.executeStatement(query);
				query = String
						.format("create table %s_passenger_preprocess as select card_id, boarding_stop_stn, alighting_stop_stn, (EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) as boarding_time,"
								+ "((EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) + (60 * ride_time))::INT AS alighting_time, "
								+ "srvc_number, direction, bus_reg_num"
								+ " from %s order by srvc_number, direction, bus_reg_num, boarding_time, alighting_time;"
								+ "alter table %s_passenger_preprocess add column idx serial;",
								tripTableName, tripTableName, tripTableName);
				dba.executeStatement(query);
			}

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
		String transitSchedule = "F:\\data\\sing2.2\\input\\transit\\transitSchedule.xml.gz";
		String networkFile = "F:\\data\\sing2.2\\input\\network\\network100.xml.gz";
		String outputEventsFile = "F:\\data\\sing2.2\\ezlinkevents.xml";
		String tripTableName = "a_lta_ezlink_week.trips11042011";
		String stopLookupTableName = "d_ezlink2events.matsim2ezlink_stop_lookup";
		EZLinkToEvents ezLinkToEvents = new EZLinkToEvents(databaseProperties, transitSchedule, networkFile,
				outputEventsFile, tripTableName, stopLookupTableName);
		ezLinkToEvents.run();
	}

}
