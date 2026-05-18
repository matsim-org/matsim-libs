package org.matsim.simwrapper.DbViewer;

import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DbEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler,
	PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, VehicleEntersTrafficEventHandler {
	private final static Logger log = LogManager.getLogger(DbEventHandler.class);
	private final Map<String, AgentState> agentStates = new HashMap<>();
	private final Map<Integer, ArrayList<Id<Link>>> legSequence = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleToPersonMap = new HashMap<>();
	ParquetWriter<Group> writerLinkTraversals;
	SimpleGroupFactory factoryLinkTraversals;
	ParquetWriter<Group> writerLegSequences;
	SimpleGroupFactory factoryLegSequences;
	private Connection conn;
//	private Connection connLegSequences;
	private PreparedStatement linkTraversalStmt;
	private PreparedStatement legSequenceStmt;
	private int linkBatchCount = 0;
	private int legBatchCount = 0;
	private int currentLegId = 0;
	@Inject
	public DbEventHandler(@DbOutputPath String outputDirectory) throws SQLException {
		// open once, reuse forever
		this.conn = DriverManager.getConnection("jdbc:sqlite:" + outputDirectory + "/sla.db");
		this.conn.setAutoCommit(false);

		// create table if it doesn't exist yet
		this.conn.createStatement().execute("""
			    CREATE TABLE IF NOT EXISTS link_traversals (
			        link_id    TEXT,
			        agent_id   TEXT,
			        leg_id      TEXT,
			        hour       INTEGER,
			        mode       TEXT
			    )
			""");
		this.conn.commit();

		// create table if it doesn't exist yet
		this.conn.createStatement().execute("""
			    CREATE TABLE IF NOT EXISTS leg_sequences (
			        leg_id   TEXT,
			        trip_id     TEXT,
			        agent_id    TEXT,
			        leg_sequence  TEXT
			    )
			""");
		this.conn.commit();

		// prepare once, execute many times
		this.linkTraversalStmt = conn.prepareStatement(
			"INSERT INTO link_traversals VALUES (?,?,?,?,?)");

		// prepare once, execute many times
		this.legSequenceStmt = conn.prepareStatement(
			"INSERT INTO leg_sequences VALUES (?,?,?,?)");


		///////////////////////////////////////////////////
		///////// Parquet - link traversals //////////////
		///////////////////////////////////////////////////

		String schemaString = """
        message link_traversals {
          required binary link_id (UTF8);
		  required binary agent_id (UTF8);
		  required binary leg_id (UTF8);
		  required int32 hour;
		  required binary mode (UTF8);
        }
        """;

		String schemaStringLegs = """
        message link_traversals {
          required binary leg_id (UTF8);
          required binary trip_id (UTF8);
		  required binary agent_id (UTF8);
		  required binary leg_sequence (UTF8);
        }
        """;

		MessageType schemaLinkTraversals = MessageTypeParser.parseMessageType(schemaString);
		MessageType schemaLegSequences = MessageTypeParser.parseMessageType(schemaStringLegs);

		this.factoryLinkTraversals = new SimpleGroupFactory(schemaLinkTraversals);
		this.factoryLegSequences = new SimpleGroupFactory(schemaLegSequences);

		Configuration conf = new Configuration();
		Path pathLinkTraversals = new Path(outputDirectory + "/link-traversals.parquet");
		try {
			writerLinkTraversals = ExampleParquetWriter
				.builder(HadoopOutputFile.fromPath(pathLinkTraversals, conf))
				.withType(schemaLinkTraversals)
				.withConf(conf)
				.withCompressionCodec(CompressionCodecName.ZSTD)
				.withDictionaryEncoding(true)
				.build();
		} catch (IOException e) {
			log.error("Failed to open parquet file", e);
		}

		Path pathLegSequences = new Path(outputDirectory + "/leg-sequences.parquet");
		try {
			writerLegSequences = ExampleParquetWriter
				.builder(HadoopOutputFile.fromPath(pathLegSequences, conf))
				.withType(schemaLegSequences)
				.withConf(conf)
				.withCompressionCodec(CompressionCodecName.ZSTD)
				.withDictionaryEncoding(true)
				.build();
		} catch (IOException e) {
			log.error("Failed to open parquet file", e);
		}


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
		if (personId == null) {
			log.warn("personId null for vehicle {}", event.getVehicleId());
			return;
		}
		AgentState state  = agentStates.get(personId.toString());

		log.info("LinkEnterEvent: vehicle={} person={} link={}",
			event.getVehicleId(), personId, event.getLinkId());

		state.legSequence.add(event.getLinkId().toString());

		// write link_traversal to table
		try {
			linkTraversalStmt.setString(1, event.getLinkId().toString());
			linkTraversalStmt.setString(2, personId.toString());
			linkTraversalStmt.setString(3, state.getLegId());
			linkTraversalStmt.setInt(4, (int) (event.getTime() / 3600));
			linkTraversalStmt.setString(5, event.getVehicleId().toString());
			linkTraversalStmt.addBatch();

			// Batching heavily speeds up insertion - don't make every insertion a transaction (much slower than insert statement)
			// from sqlite.org: "Actually, SQLite will easily do 50,000 or more INSERT statements per second on an average desktop computer. But it will only do a few dozen transactions per second.
			// Transaction speed is limited by the rotational speed of your disk drive.
			// A transaction normally requires two complete rotations of the disk platter, which on a 7200RPM disk drive limits you to about 60 transactions per second."

			if (linkBatchCount++ % 10_000 == 0) {
				linkTraversalStmt.executeBatch();
				conn.commit();
			}

		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}

		try {
			this.writerLinkTraversals.write(this.factoryLinkTraversals.newGroup()
				.append("link_id", event.getLinkId().toString())
				.append("agent_id", personId.toString())
				.append("leg_id", state.getLegId())
				.append("hour", (int) (event.getTime() / 3600))
				.append("mode", event.getVehicleId().toString()));
//				log.info("");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		System.out.println("person departure event: " + event.getPersonId().toString());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		System.out.println("person arrival event: " + event.getPersonId().toString());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		System.out.println("person " + event.getPersonId().toString() + " ended activity " + event.getEventType().toString());
		if (!agentStates.containsKey(event.getPersonId().toString())) {
			AgentState state = new AgentState();
			state.agentId = event.getPersonId().toString();
			state.legIndex = 1;
			state.tripIndex = 1;
			state.legSequence = new ArrayList<>();
			agentStates.put(state.agentId, state);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		AgentState state = agentStates.get(event.getPersonId().toString());

			// in handleEvent(ActivityStartEvent) — leg complete
			String legId = state.getLegId();
			String tripId = state.getTripId();
			String links = String.join("|", state.legSequence);

			log.info("Writing leg sequence: legId={} links={}", legId, links);
			// write leg_sequence to table
			try {
				legSequenceStmt.setString(1, legId);
				legSequenceStmt.setString(2, tripId);
				legSequenceStmt.setString(3, state.agentId);
				legSequenceStmt.setObject(4, links);
				legSequenceStmt.addBatch();
				// Batching heavily speeds up insertion - don't make every insertion a transaction (much slower than insert statement)
				// from sqlite.org: "Actually, SQLite will easily do 50,000 or more INSERT statements per second on an average desktop computer. But it will only do a few dozen transactions per second.
				// Transaction speed is limited by the rotational speed of your disk drive.
				// A transaction normally requires two complete rotations of the disk platter, which on a 7200RPM disk drive limits you to about 60 transactions per second."

				if (legBatchCount++ % 5_000 == 0) {
					legSequenceStmt.executeBatch();
					conn.commit();
				}

			} catch (SQLException ex) {
				throw new RuntimeException(ex);
			}

		try {
			this.writerLegSequences.write(this.factoryLegSequences.newGroup()
				.append("leg_id", legId)
				.append("trip_id", tripId)
				.append("agent_id", state.agentId)
				.append("leg_sequence", links));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		state.legIndex++;
		if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
			state.tripIndex++;
		}
		state.legSequence.clear();

		System.out.println("person " + event.getPersonId().toString() + " start activity " + event.getEventType().toString());
	}

	public Map<String, AgentState> getAgentStates() {
		return agentStates;
	}

	public void finish() throws SQLException {
		log.info("finish() called — flushing link batch, batchCount={}", linkBatchCount);
		log.info("finish() called — flushing leg batch, batchCount={}", legBatchCount);
		linkTraversalStmt.executeBatch();
		legSequenceStmt.executeBatch();
		conn.commit();
		conn.close();
			try {
				writerLinkTraversals.close();
			}  catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		try {
			writerLegSequences.close();
		}  catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
