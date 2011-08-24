/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToDB.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.energyflows.analysis;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

public class EventsToDB {

	private static final Logger log = Logger.getLogger(EventsToDB.class);
	
	private Config config;
	private Scenario scenario;
	private Map<Id, Tuple<Id, String>> homeFacilities;	// Map<PersonId, Tuple<FacilityId, ActivityType>>
	
	private DBConnectionTool dbConnectionTool;
	private String tableName = "events";
	private String runId = "1";
	
	public static void main(String[] args) throws SQLException {
		
		if (args.length != 5) {
			log.error("Unexpected number of input arguments. Expected 4: network file, facilities file, population file, events file, password for DB.");
			System.exit(0);
		}
		new EventsToDB(args[0], args[1], args[2], args[3], args[4]);	
	}
	
	public EventsToDB(String networkFile, String facilitiesFile, String populationFile, String eventsFile, String password) throws SQLException {
		dbConnectionTool = new DBConnectionTool();
		
		log.info("loading scenario...");
		loadScenario(networkFile, facilitiesFile, populationFile);
		log.info("done.");
		
		log.info("getting home facilities...");
		getHomeFacilities();
		log.info("done.");
		
		connectToDB(password);
		
		log.info("writing home facilities events to the DB...");
		writeHomeFacilitiesToDB();
		log.info("done.");
		
		log.info("reading events from file and write them to the DB...");
		readEventsFile(eventsFile);
		log.info("done.");
		
		disconnectFromDB();
	}
	
	public void connectToDB(String password) throws SQLException {
		dbConnectionTool.connectToDB(password);
	}
	
	public void disconnectFromDB() throws SQLException {
		dbConnectionTool.disconnectFromDB();
	}
	
	private void loadScenario(String networkFile, String facilitiesFile, String populationFile) {
		config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.plans().setInputFile(populationFile);
		scenario = ScenarioUtils.loadScenario(config);
	}
	
	private void readEventsFile(String eventsFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsToDBEventsHandler(dbConnectionTool, tableName, runId));
		
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
	}
	
	/*
	 * Get the home location of all persons to be able to write their initial position
	 * to the database. In general we take the location of the first activity, which
	 * should be "home". For TTA persons it should be "tta".
	 */
	private void getHomeFacilities() {
		
		homeFacilities = new HashMap<Id, Tuple<Id, String>>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			
			if (activity.getType().equals("home")) {
				homeFacilities.put(person.getId(), new Tuple<Id, String>(activity.getFacilityId(), activity.getType()));
			} else if (activity.getType().equals("tta")) {
				homeFacilities.put(person.getId(), new Tuple<Id, String>(activity.getFacilityId(), activity.getType()));
			} else {
				log.warn("Unexpected first activity type. Found: " + activity.getType());
			}
		}
	}
	
	/*
	 * Write the home location to the DB. All further locations will be created based on the events.
	 * However, there is no ActivityStartEvent at 00:00:00, therefore we create a dummy event.
	 */
	private void writeHomeFacilitiesToDB() throws SQLException {
		Counter counter = new  Counter("written home facility events...");
		
		for (Entry<Id, Tuple<Id, String>> entry : homeFacilities.entrySet()) {
			Id personId = entry.getKey();
			Id facilityId = entry.getValue().getFirst();
			double time = 0.0;
			String activityType = entry.getValue().getSecond();
			String eventType = "actstart";
			
			StringBuffer sbColumns = new StringBuffer();
			StringBuffer sbValues = new StringBuffer();
			sbColumns.append("INSERT INTO ");
			sbColumns.append(tableName);
			sbColumns.append(" (");
			
			sbColumns.append("runId,");
			sbValues.append(runId + ",");
			sbColumns.append("time,");
			sbValues.append(time + ",");
			sbColumns.append("facilityId,");
			sbValues.append("'" + facilityId.toString() + "',");
			sbColumns.append("activityType,");
			sbValues.append("'" + activityType + "',");
			sbColumns.append("eventType");
			sbValues.append("'" + eventType + "'");
			
			sbColumns.append(") VALUES (");
			sbColumns.append(sbValues);
			sbColumns.append(")");
			
			dbConnectionTool.executeUpdate(sbColumns.toString());
			counter.incCounter();
		}
		counter.printCounter();
	}
		
	private static class DBConnectionTool {
	
		private Connection connection;

		public void connectToDB(String password) throws SQLException {

			connection = DriverManager.getConnection("jdbc:postgresql://lesopbpc27.epfl.ch:5432/zurich", "ethz", password);
			DatabaseMetaData dbmd = connection.getMetaData();

			// confirm connection
			log.info("Connection to " + dbmd.getDatabaseProductName() + " " + dbmd.getDatabaseProductVersion() + " successful.\n");
		}
		
		public void executeUpdate(String query) throws SQLException {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(query);
		}
		
		public ResultSet executeQuery(String query) throws SQLException {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
				
			return rs;
		}
		
		public void disconnectFromDB() throws SQLException {
			connection.close();
			connection = null;
		}
		
	}
	
	private static class EventsToDBEventsHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
		
		private Counter counter = new  Counter("written events to the DB...");
		private DBConnectionTool dbConnectionTool;
		private String tableName;
		private String runId;
		
		public EventsToDBEventsHandler(DBConnectionTool dbConnectionTool, String tableName, String runId) {
			this.dbConnectionTool = dbConnectionTool;
			this.tableName = tableName;
			this.runId = runId;
		}
		
		@Override
		public void reset(int iteration) {
						
		}
		
		@Override
		public void handleEvent(ActivityStartEvent event) {
			writeEventToDB(event);
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {			
			writeEventToDB(event);
		}
		
		private void writeEventToDB(ActivityEvent event) {
			try {
				Id facilityId = event.getFacilityId();
				double time = event.getTime();
				String activityType = event.getActType();
				String eventType = ((EventImpl) event).getEventType();
				
				StringBuffer sbColumns = new StringBuffer();
				StringBuffer sbValues = new StringBuffer();
				sbColumns.append("INSERT INTO ");
				sbColumns.append(tableName);
				sbColumns.append(" (");
				
				sbColumns.append("runId,");
				sbValues.append(runId + ",");
				sbColumns.append("time,");
				sbValues.append(time + ",");
				sbColumns.append("facilityId,");
				sbValues.append("'" + facilityId.toString() + "',");
				sbColumns.append("activityType,");
				sbValues.append("'" + activityType + "',");
				sbColumns.append("eventType");
				sbValues.append("'" + eventType + "'");
				
				sbColumns.append(") VALUES (");
				sbColumns.append(sbValues);
				sbColumns.append(")");
				dbConnectionTool.executeUpdate(sbColumns.toString());

				counter.incCounter();			
			} catch (SQLException e) {
				log.error("Error when trying to write to the DB.");
				log.error(e.getMessage());
			}
		}
	}
}
