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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class EZLinkToEvents {
	// internal classes
	private class PTVehicle {
		// Attributes
		// a matsim vehicle can only do one line & route at a time, but a
		// physical vehicle can switch lines and routes
		Id vehicleId;
		Id busRegNumber;
		Id transitLineId;
		Id ezLinkRouteId;
		TreeSet<Integer> passengerRemovalTimes = new TreeSet<Integer>();
		HashMap<Id, Boolean> transitRouteEvaluator = new HashMap<Id, Boolean>();
		Map<Id, TransitRoute> possibleRoutes;
		TreeMap<Id, Integer> stopsVisitedAtWhatTime = new TreeMap<Id, Integer>();
		boolean in = false;
		TreeMap<Id, Integer> passengers = new TreeMap<Id, Integer>();
		double distance;
		Id firstStop;
		Id lastStop;
		int linkEnterTime = 0;

		// Constructors
		public PTVehicle(Id transitLineId) {
			this.transitLineId = transitLineId;
			possibleRoutes = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes();
		}

		// Methods

		public void addPassenger(Id stopId, Id passengerId, int time) {
			passengers.put(passengerId, time);
		}

		public double removePassenger(Id passengerId) {
			return distance - passengers.remove(passengerId);
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

	private class EZLinkRoute {
		public EZLinkRoute(int direction, EZLinkLine ezLinkLine) {
			super();
			this.direction = direction;
			this.line = ezLinkLine;
		}

		int direction;
		EZLinkLine line;
		HashSet<Id> busRegistrationNumbers = new HashSet<Id>();

		public String toString() {
			return (line.lineId.toString() + " : " + direction + " : " + busRegistrationNumbers + "\n");
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
	private String serviceTableName;

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
	}

	// non-static public methods
	public void processQueue() {
		for (Event event : eventQueue) {
			eventsManager.processEvent(event);
		}
	}

	public void run() throws SQLException, NoConnectionException {
		getLinesFromEZlink();
		
	}

	/**
	 * @param trimServiceNumber
	 *            - sometimes the service number needs to be trimmed of
	 *            whitespace. only needs to be done once.
	 * @throws SQLException
	 * @throws NoConnectionException
	 */
	private void getLinesFromEZlink() throws SQLException, NoConnectionException {
		this.ezlinkLines = new HashMap<Id, EZLinkToEvents.EZLinkLine>();
		this.serviceTableName = this.tripTableName + "_services_by_vehicle";
		try {
			dba.executeQuery("select distinct srvc_number from " + this.serviceTableName
					+ " where srvc_number is not null");
		} catch (SQLException se) {
			//  necessary to create a summary table
			System.out.println("Indexing....");
			dba.executeUpdate("update " + this.tripTableName + " set srvc_number = trim(srvc_number);");
			dba.executeUpdate("create index " + tripTableName.split("\\.")[1] + "_idx on " + this.tripTableName
					+ "(srvc_number, direction, bus_reg_num)");
			dba.executeStatement("create table " + serviceTableName
					+ " as select distinct srvc_number, direction, bus_reg_num from "
					+ this.tripTableName + " where srvc_number is not null");
		}
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
		resultSet = dba.executeQuery("select distinct srvc_number, direction, bus_reg_num from " + this.serviceTableName
				+ " where srvc_number is not null");
		while (resultSet.next()) {
			IdImpl lineId = new IdImpl(resultSet.getString(1));
			EZLinkLine ezLinkLine = this.ezlinkLines.get(lineId);
			EZLinkRoute ezLinkRoute = ezLinkLine.routes.get(resultSet.getInt(2));
			ezLinkRoute.busRegistrationNumbers.add(new IdImpl(resultSet.getString(3)));
		}

		System.out.println(this.ezlinkLines);
	}

	// static methods
	public static void main(String[] args) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SQLException, NoConnectionException {
		String databaseProperties = "f:/data/matsim2postgres.properties";
		String transitSchedule = "F:\\data\\sing2.2\\input\\transit\\transitSchedule.xml.gz";
		String networkFile = "F:\\data\\sing2.2\\input\\network\\network100.xml.gz";
		String outputEventsFile = "F:\\data\\sing2.2\\ezlinkevents.xml";
		String tripTableName = "a_lta_ezlink_week.trips12042011";
		String stopLookupTableName = "d_ezlink2events.matsim2ezlink_stop_lookup";
		EZLinkToEvents ezLinkToEvents = new EZLinkToEvents(databaseProperties, transitSchedule, networkFile,
				outputEventsFile, tripTableName, stopLookupTableName);
		ezLinkToEvents.run();
	}

}
