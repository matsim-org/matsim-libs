package org.matsim.simwrapper.DbViewer;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DbEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler,
		PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, VehicleEntersTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(DbEventHandler.class);
	private final Map<String, AgentState> agentStates = new HashMap<>();
//	private final Map<String, LinkTraversal> linkTraversals = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleToPersonMap = new HashMap<>();
	private Connection conn;
	private PreparedStatement linkTraversalStmt;
	private int batchCount = 0;

	@Inject
	public DbEventHandler(@DbOutputPath String outputDirectory) throws SQLException {
		// open once, reuse forever
		this.conn = DriverManager.getConnection("jdbc:sqlite:" + outputDirectory + "/link_traversals.db");
		this.conn.setAutoCommit(false);

		// create table if it doesn't exist yet
		this.conn.createStatement().execute("""
        CREATE TABLE IF NOT EXISTS link_traversal (
            link_id    TEXT,
            agent_id   TEXT,
            hour       INTEGER,
            mode       TEXT
        )
    """);
		this.conn.commit();

		// prepare once, execute many times
		this.linkTraversalStmt = conn.prepareStatement(
			"INSERT INTO link_traversal VALUES (?,?,?,?)");

	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		log.info("VehicleEntersTraffic: vehicle={} person={}",
			event.getVehicleId(), event.getPersonId());
			vehicleToPersonMap.put(event.getVehicleId(), event.getPersonId());
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {

		Id<Person> personId = vehicleToPersonMap.get(event.getVehicleId());

		log.info("LinkEnterEvent: vehicle={} person={} link={}",
			event.getVehicleId(), personId, event.getLinkId());

		if (personId == null) {
			log.warn("personId null for vehicle {}", event.getVehicleId());
			return;
		}

		// write link_traversal to table
		try {
					linkTraversalStmt.setString(1, event.getLinkId().toString());
					linkTraversalStmt.setString(2, personId.toString());
					linkTraversalStmt.setInt(3, (int) (event.getTime() / 3600));
					linkTraversalStmt.setString(4, event.getVehicleId().toString());
					linkTraversalStmt.addBatch();

			// Batching heavily speeds up insertion - don't make every insertion a transaction (much slower than insert statement)
			// from sqlite.org: "Actually, SQLite will easily do 50,000 or more INSERT statements per second on an average desktop computer. But it will only do a few dozen transactions per second.
			// Transaction speed is limited by the rotational speed of your disk drive.
			// A transaction normally requires two complete rotations of the disk platter, which on a 7200RPM disk drive limits you to about 60 transactions per second."

			if (batchCount++ % 10_000 == 0) {
				linkTraversalStmt.executeBatch();
				conn.commit();
			}

				} catch (SQLException ex) {
				throw new RuntimeException(ex);
			}

//		String key = event.getLinkId() + "_" + personId + "_" + traversal.timeStamp;

//		linkTraversals.put(key, traversal);

//		AgentState state = agentStates.get(event.getVehicleId().toString());
//		if (state != null) state.appendLink(event.getLinkId().toString(), event.getTime());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		AgentState state = new AgentState();
		state.agentId = event.getPersonId().toString();
		state.mode = event.getLegMode();
		agentStates.put(state.agentId, state);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		System.out.println("person arrival event: " + event.getPersonId().toString());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		System.out.println("person " + event.getPersonId().toString() + " ended activity " + event.getEventType().toString());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		System.out.println("person " + event.getPersonId().toString() + " start activity " + event.getEventType().toString());
	}

	public Map<String, AgentState> getAgentStates() {
		return agentStates;
	}

	public void finish() throws SQLException {
		log.info("finish() called — flushing batch, batchCount={}", batchCount);
		linkTraversalStmt.executeBatch();
		conn.commit();
		conn.close();
	}

//	public Map<String, LinkTraversal> getLinkTraversals() {
//		return linkTraversals;
//	}
}
