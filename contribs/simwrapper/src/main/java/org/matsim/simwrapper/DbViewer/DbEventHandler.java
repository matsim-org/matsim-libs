package org.matsim.simwrapper.DbViewer;

import com.google.common.collect.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.column.ParquetProperties;
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
import org.matsim.application.options.CsvOptions;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.*;

public class DbEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler,
	PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, VehicleEntersTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(DbEventHandler.class);
	private final Map<String, AgentState> agentStates = new HashMap<>();
	private final Map<Id<Vehicle>, Map<Id<Person>, String>> vehicleToPersonMap = new HashMap<>();
	private final Map<Id<Person>, String> modeToPersonMap = new HashMap<>();

	ParquetWriter<Group> writerLinkTraversals;
	SimpleGroupFactory factoryLinkTraversals;
	ParquetWriter<Group> writerLegSequences;
	SimpleGroupFactory factoryLegSequences;

	String outputDirectory;

	public DbEventHandler(String outputDirectory) throws IOException {
		this.outputDirectory = outputDirectory;

		FileSystem fs = FileSystem.get(new Configuration());
		Configuration conf = new Configuration();

		String schemaStringTraversals = """
            message link-traversals {
              required binary link_id (UTF8);
              required binary agent_id (UTF8);
              required binary leg_id (UTF8);
              required int32 hour;
              required binary mode (UTF8);
            }
            """;

		String schemaStringLegs = """
            message leg-sequences {
              required binary leg_id (UTF8);
              required binary trip_id (UTF8);
              required binary agent_id (UTF8);
              required binary leg_sequence (UTF8);
            }
            """;

		MessageType schemaLinkTraversals = MessageTypeParser.parseMessageType(schemaStringTraversals);
		MessageType schemaLegSequences = MessageTypeParser.parseMessageType(schemaStringLegs);

		this.factoryLinkTraversals = new SimpleGroupFactory(schemaLinkTraversals);
		this.factoryLegSequences = new SimpleGroupFactory(schemaLegSequences);

		Path pathLinkTraversals = new Path(outputDirectory + "/link-traversals.parquet");
		if (fs.exists(pathLinkTraversals)) fs.delete(pathLinkTraversals, true);

		writerLinkTraversals = ExampleParquetWriter
			.builder(HadoopOutputFile.fromPath(pathLinkTraversals, conf))
			.withType(schemaLinkTraversals)
			.withCompressionCodec(CompressionCodecName.ZSTD)
			.withDictionaryEncoding(true)
			.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
			.withRowGroupSize(32L * 1024 * 1024) // why these numbers?
			.withPageSize(512 * 1024) // why these numbers?
			.build();

		Path pathLegSequences = new Path(outputDirectory + "/leg-sequences.parquet");
		if (fs.exists(pathLegSequences)) fs.delete(pathLegSequences, true);

		writerLegSequences = ExampleParquetWriter
			.builder(HadoopOutputFile.fromPath(pathLegSequences, conf))
			.withType(schemaLegSequences)
			.withCompressionCodec(CompressionCodecName.ZSTD)
			.withDictionaryEncoding(true)
			.withRowGroupSize(32L * 1024 * 1024)
			.withPageSize(512 * 1024)
			.build();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicleToPersonMap.put(event.getVehicleId(), Map.of(event.getPersonId(), event.getNetworkMode()));
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getVehicleId().toString().contains("pt")) return;

		Map<Id<Person>, String> personIdAndMode = vehicleToPersonMap.get(event.getVehicleId());
		if (personIdAndMode.keySet() == null ||  personIdAndMode.values() == null  || personIdAndMode.keySet().size() != 1 || personIdAndMode.values().toArray().length != 1) return;

		Id<Person> personId = personIdAndMode.keySet().iterator().next();
		String mode = personIdAndMode.values().toArray()[0].toString();

		AgentState state = agentStates.get(personId.toString());
		if (state == null) return;

		state.legSequence.add(event.getLinkId().toString());

		try {
			writerLinkTraversals.write(factoryLinkTraversals.newGroup()
				.append("link_id", event.getLinkId().toString())
				.append("agent_id", personId.toString())
				.append("leg_id", state.getLegId())
				.append("hour", (int) (event.getTime() / 3600))
				.append("mode", mode));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
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
		if (state == null || state.legSequence.isEmpty()) return;

		String legId = state.getLegId();
		String tripId = state.getTripId();
		String links = String.join("|", state.legSequence);

		try {
			writerLegSequences.write(factoryLegSequences.newGroup()
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
	}

	@Override public void handleEvent(PersonDepartureEvent event) {}
	@Override public void handleEvent(PersonArrivalEvent event) {}

	public void createComparisonDatasets() throws SQLException, IOException {
		Connection duckConn = DriverManager.getConnection("jdbc:duckdb:");

		String linkTraversalsPath = this.outputDirectory + "/link-traversals.parquet";
		String linkTraversalsSortedPath = this.outputDirectory + "/link-traversals-sorted.parquet";
		String legSequencesPath = this.outputDirectory + "/leg-sequences.parquet";
		String legSequencesSortedPath = this.outputDirectory + "/leg-sequences-sorted.parquet";

		String linkTraversalsCsvPath =  this.outputDirectory + "/link-traversals-sorted.csv.zst";
		String legSequencesCsvPath =  this.outputDirectory + "/leg-sequences-sorted.csv.zst";

		String linkTraversalsCsvPathUncompressed =  this.outputDirectory + "/link-traversals-sorted.csv";

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsSortedPath + "' (FORMAT PARQUET, CODEC 'ZSTD')"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesSortedPath + "' (FORMAT PARQUET, CODEC 'ZSTD')"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsCsvPath + "' (FORMAT CSV, COMPRESSION zstd)"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesSortedPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesCsvPath + "' (FORMAT CSV, COMPRESSION zstd)"
		);

//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
//				"ORDER BY link_id, hour) " +
//				"TO '" + linkTraversalsCsvPathUncompressed + "' (FORMAT CSV)"
//		);

		java.nio.file.Path dbPath = java.nio.file.Path.of(outputDirectory + "/sla.db");
		java.nio.file.Files.deleteIfExists(dbPath);

		duckConn.createStatement().execute(
			"ATTACH '" + outputDirectory + "/sla.db' AS sqlite_db (TYPE SQLITE)"
		);
		duckConn.createStatement().execute(
			"CREATE TABLE IF NOT EXISTS sqlite_db.link_traversals AS " +
				"SELECT * FROM read_parquet('" + outputDirectory + "/link-traversals-sorted.parquet')"
		);
		duckConn.createStatement().execute(
			"CREATE TABLE IF NOT EXISTS sqlite_db.leg_sequences AS " +
				"SELECT * FROM read_parquet('" + outputDirectory + "/leg-sequences-sorted.parquet')"
		);
//
//		duckConn.createStatement().execute("INSTALL avro");
//		duckConn.createStatement().execute("LOAD avro");
//
//		String linkTraversalsAvroPath = this.outputDirectory + "/link-traversals-sorted.avro";
//		String legSequencesAvroPath = this.outputDirectory + "/leg-sequences-sorted.avro";
//
//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
//				"ORDER BY link_id, hour) " +
//				"TO '" + linkTraversalsAvroPath + "' (FORMAT AVRO)"
//		);
//
//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + legSequencesSortedPath + "') " +
//				"ORDER BY leg_id) " +
//				"TO '" + legSequencesAvroPath + "' (FORMAT AVRO)"
//		);

	}



	public void finish() throws IOException {

		writerLinkTraversals.close();
		writerLegSequences.close();

		try {
			createComparisonDatasets();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}

	public Map<String, AgentState> getAgentStates() {
		return agentStates;
	}
}

//public class DbEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler,
//	PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, VehicleEntersTrafficEventHandler {
//	private final static Logger log = LogManager.getLogger(DbEventHandler.class);
//	private final Map<String, AgentState> agentStates = new HashMap<>();
////	private final Map<Integer, ArrayList<Id<Link>>> legSequence = new HashMap<>();
//	private final Map<Id<Vehicle>, Id<Person>> vehicleToPersonMap = new HashMap<>();
//	private final List<LinkLinkPair> linkLinkPairBuffer = new ArrayList<>();
//	record LinkLinkPair(String linkId, String coLinkId, int hour) {}
//	ParquetWriter<Group> writerLinkTraversals;
//	SimpleGroupFactory factoryLinkTraversals;
//	ParquetWriter<Group> writerLegSequences;
//	SimpleGroupFactory factoryLegSequences;
//	ParquetWriter<Group> writerLinkLinkPairs;
//	SimpleGroupFactory factoryLinkLinkPairs;
//	ParquetWriter<Group> writerLinkIndex;
//	SimpleGroupFactory factoryLinkIndex;
//	private Connection conn;
//	private final List<LinkTraversalRecord> linkTraversalBuffer = new ArrayList<>();
//	record LinkTraversalRecord(String linkId, String agentId, String legId, int hour, String mode) {}
//
//	private final List<LegSequenceRecord> legSequenceBuffer = new ArrayList<>();
//	record LegSequenceRecord(String legId, String agentId, String tripId, String legSequence) {
//		public String legSequence() {
//			return legSequence;
//		}
//	}
//
//	List<String> header = Lists.newArrayList("link_id", "trip_id", "agent_id", "leg_sequence");
//
//	List<String> linksHeader = Lists.newArrayList("link_id", "hour", "co_link_id", "count");
//
//	private PreparedStatement linkIndexStmt;
//	private PreparedStatement linkTraversalStmt;
//	private PreparedStatement legSequenceStmt;
//	private int linkBatchCount = 0;
//	private int legBatchCount = 0;
//	private int currentLegId = 0;
//	String outputDirectory;
//
//	public DbEventHandler(String outputDirectory) throws SQLException, IOException {
//		this.outputDirectory = outputDirectory;
//
//		this.conn = DriverManager.getConnection("jdbc:sqlite:" + this.outputDirectory + "/sla.db");
//		this.conn.setAutoCommit(false);
//
//		// create table if it doesn't exist yet
//
//		// create table if it doesn't exist yet
//		this.conn.createStatement().execute("""
//			    CREATE TABLE IF NOT EXISTS link_traversals (
//			        link_id    TEXT,
//			        agent_id   TEXT,
//			        leg_id      TEXT,
//			        hour       INTEGER,
//			        mode       TEXT
//			    )
//			""");
//		this.conn.commit();
//
//		this.conn.createStatement().execute("""
//			    CREATE TABLE IF NOT EXISTS link_index (
//			        link_id    TEXT,
//			        hour  INTEGER NOT NULL,
//			        co_link_id      TEXT,
//			        count INTEGER NOT NULL
//			    )
//			""");
//
//		this.conn.commit();
//
//		// create table if it doesn't exist yet
//		this.conn.createStatement().execute("""
//			    CREATE TABLE IF NOT EXISTS leg_sequences (
//			        leg_id   TEXT,
//			        trip_id     TEXT,
//			        agent_id    TEXT,
//			        leg_sequence  TEXT
//			    )
//			""");
//		this.conn.commit();
//
//		this.linkTraversalStmt = conn.prepareStatement(
//			"INSERT INTO link_traversals VALUES (?,?,?,?,?)");
//		this.linkIndexStmt = conn.prepareStatement(
//			"INSERT INTO link_index VALUES (?,?,?,?)");
//		this.legSequenceStmt = conn.prepareStatement(
//			"INSERT INTO leg_sequences VALUES (?,?,?,?)");
//
//
//		///////////////////////////////////////////////////
//		///////// Parquet - link traversals //////////////
//		///////////////////////////////////////////////////
//
//		String schemaStringTraversals = """
//        message link-traversals {
//          required binary link_id (UTF8);
//		  required binary agent_id (UTF8);
//		  required binary leg_id (UTF8);
//		  required int32 hour;
//		  required binary mode (UTF8);
//        }
//        """;
//
//		String schemaStringLegs = """
//        message leg-sequences {
//          required binary leg_id (UTF8);
//          required binary trip_id (UTF8);
//		  required binary agent_id (UTF8);
//		  required binary leg_sequence (UTF8);
//        }
//        """;
//
//		MessageType schemaLinkTraversals = MessageTypeParser.parseMessageType(schemaStringTraversals);
//		MessageType schemaLegSequences = MessageTypeParser.parseMessageType(schemaStringLegs);
//
//		this.factoryLinkTraversals = new SimpleGroupFactory(schemaLinkTraversals);
//		this.factoryLegSequences = new SimpleGroupFactory(schemaLegSequences);
//
//
//		FileSystem fs = FileSystem.get(new Configuration());
//		Configuration conf = new Configuration();
//		Path pathLinkTraversals = new Path(this.outputDirectory + "/link-traversals.parquet");
//		if (fs.exists(pathLinkTraversals)) {
//			fs.delete(pathLinkTraversals, true);
//		}
//
//		try {
//			writerLinkTraversals = ExampleParquetWriter
//				.builder(HadoopOutputFile.fromPath(pathLinkTraversals, conf))
//				.withType(schemaLinkTraversals)
//				.withConf(conf)
//				.withCompressionCodec(CompressionCodecName.ZSTD)
//				.withDictionaryEncoding(true)
//				.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0) // enables column index
//				.withRowGroupSize(32L * 1024 * 1024)  // 32MB
//				.withPageSize(512 * 1024) // - finer-grained skipping based off of page size
//				.build();
//		} catch (IOException e) {
//			log.error("Failed to open parquet file", e);
//		}
//
//		Path pathLegSequences = new Path(this.outputDirectory + "/leg-sequences.parquet");
//
//		if (fs.exists(pathLegSequences)) {
//			fs.delete(pathLegSequences, true);
//		}
//
//		try {
//			writerLegSequences = ExampleParquetWriter
//				.builder(HadoopOutputFile.fromPath(pathLegSequences, conf))
//				.withType(schemaLegSequences)
//				.withConf(conf)
//				.withCompressionCodec(CompressionCodecName.ZSTD)
//				.withDictionaryEncoding(true) // setting this to false creates a massive file like Sqlite
//				.build();
//		} catch (IOException e) {
//			log.error("Failed to open parquet file", e);
//		}
//
//		// Add to constructor
//		String schemaStringLinkLinkPairs = """
//    message link-link-pairs {
//      required binary link_id (UTF8);
//      required binary co_link_id (UTF8);
//      required int32 hour;
//    }
//    """;
//		MessageType schemaLinkLinkPairs = MessageTypeParser.parseMessageType(schemaStringLinkLinkPairs);
//		this.factoryLinkLinkPairs = new SimpleGroupFactory(schemaLinkLinkPairs);
//
//
//		Path pathLinkLinkPairs = new Path(this.outputDirectory + "/link-link-pairs.parquet");
//		if (fs.exists(pathLinkLinkPairs)) {
//			fs.delete(pathLinkLinkPairs, true);
//		}
//
//		try {
//			writerLinkLinkPairs = ExampleParquetWriter
//				.builder(HadoopOutputFile.fromPath(pathLinkLinkPairs, conf))
//				.withType(schemaLinkLinkPairs)
//				.withConf(conf)
//				.withCompressionCodec(CompressionCodecName.ZSTD)
//				.withDictionaryEncoding(true)
//				.withRowGroupSize(32L * 1024 * 1024)
//				.withPageSize(512 * 1024)
//				.build();
//		} catch (IOException e) {
//			log.error("Failed to open link-link-pairs parquet file", e);
//		}
//
//		String schemaStringLinkIndex = """
//    message link-link-pairs {
//      required binary link_id (UTF8);
//      required binary co_link_id (UTF8);
//      required int32 hour;
//    }
//    """;
//		MessageType schemaLinkIndex = MessageTypeParser.parseMessageType(schemaStringLinkIndex);
//		this.factoryLinkIndex = new SimpleGroupFactory(schemaLinkIndex);
//
//
//		Path pathLinkIndex = new Path(this.outputDirectory + "/link-index.parquet");
//		if (fs.exists(pathLinkIndex)) {
//			fs.delete(pathLinkIndex, true);
//		}
//
//		try {
//			writerLinkIndex = ExampleParquetWriter
//				.builder(HadoopOutputFile.fromPath(pathLinkIndex, conf))
//				.withType(schemaLinkIndex)
//				.withConf(conf)
//				.withCompressionCodec(CompressionCodecName.ZSTD)
//				.withDictionaryEncoding(true)
//				.withRowGroupSize(32L * 1024 * 1024)
//				.withPageSize(512 * 1024)
//				.build();
//		} catch (IOException e) {
//			log.error("Failed to open link-index parquet file", e);
//		}
//
//
//	}
//
//
//	@Override
//	public void handleEvent(VehicleEntersTrafficEvent event) {
//		log.info("VehicleEntersTraffic: vehicle={} person={}",
//			event.getVehicleId(), event.getPersonId());
//		vehicleToPersonMap.put(event.getVehicleId(), event.getPersonId());
//	}
//
//
//	@Override
//	public void handleEvent(LinkEnterEvent event) {
//
//		if (event.getVehicleId().toString().contains("pt"))
//		{
//			log.warn("currently not tracking pt lines for SLA, ignoring {}", event.getVehicleId());
//			return;
//		}
//
//		Id<Person> personId = vehicleToPersonMap.get(event.getVehicleId());
//		if (personId == null) {
//			log.warn("personId null for vehicle {}", event.getVehicleId());
//			return;
//		}
//		AgentState state  = agentStates.get(personId.toString());
//
//		log.info("LinkEnterEvent: vehicle={} person={} link={}",
//			event.getVehicleId(), personId, event.getLinkId());
//
//		state.legSequence.add(event.getLinkId().toString());
//
//		// write link_traversal to table
//		try {
//			linkTraversalStmt.setString(1, event.getLinkId().toString());
//			linkTraversalStmt.setString(2, personId.toString());
//			linkTraversalStmt.setString(3, state.getLegId());
//			linkTraversalStmt.setInt(4, (int) (event.getTime() / 3600));
//			linkTraversalStmt.setString(5, event.getVehicleId().toString());
//			linkTraversalStmt.addBatch();
////
////			// Batching heavily speeds up insertion - don't make every insertion a transaction (much slower than insert statement)
////			// from sqlite.org: "Actually, SQLite will easily do 50,000 or more INSERT statements per second on an average desktop computer. But it will only do a few dozen transactions per second.
////			// Transaction speed is limited by the rotational speed of your disk drive.
////			// A transaction normally requires two complete rotations of the disk platter, which on a 7200RPM disk drive limits you to about 60 transactions per second."
////
//			if (linkBatchCount++ % 10_000 == 0) {
//				linkTraversalStmt.executeBatch();
//				conn.commit();
//			}
////
//		} catch (SQLException ex) {
//			throw new RuntimeException(ex);
//		}
//
//			// -- BEST APPROACH (PARQUET DOESN'T NEED TO HAVE A SORTED BUFFER AHEAD OF TIME. STREAMING IS MOST MEMORY EFFICIENT)
//		try{
//			this.writerLinkTraversals.write(this.factoryLinkTraversals.newGroup()
//				.append("link_id", event.getLinkId().toString())
//				.append("agent_id", personId.toString())
//				.append("leg_id", state.getLegId())
//				.append("hour", (int) (event.getTime() / 3600))
//				.append("mode", event.getVehicleId().toString()));
//		} catch (IOException ex) {
//			throw new RuntimeException(ex);
//		}
//
////				log.info("");
//			// NEW APPROACH
////			linkTraversalBuffer.add(new LinkTraversalRecord(
////				event.getLinkId().toString(),
////				personId.toString(),
////				state.getLegId(),
////				(int)(event.getTime() / 3600),
////				event.getVehicleId().toString()
////			));
//
//	}
//
//	@Override
//	public void handleEvent(PersonDepartureEvent event) {
//		System.out.println("person departure event: " + event.getPersonId().toString());
//	}
//
//	@Override
//	public void handleEvent(PersonArrivalEvent event) {
//		System.out.println("person arrival event: " + event.getPersonId().toString());
//	}
//
//	@Override
//	public void handleEvent(ActivityEndEvent event) {
//		System.out.println("person " + event.getPersonId().toString() + " ended activity " + event.getEventType().toString());
//		if (!agentStates.containsKey(event.getPersonId().toString())) {
//			AgentState state = new AgentState();
//			state.agentId = event.getPersonId().toString();
//			state.legIndex = 1;
//			state.tripIndex = 1;
//			state.legSequence = new ArrayList<>();
//			agentStates.put(state.agentId, state);
//		}
//	}
//
//	@Override
//	public void handleEvent(ActivityStartEvent event) {
//		AgentState state = agentStates.get(event.getPersonId().toString());
//		if (state == null || state.legSequence.isEmpty()) return;
//
//			// in handleEvent(ActivityStartEvent) — leg complete
//			String legId = state.getLegId();
//			String tripId = state.getTripId();
//			List<String> links = state.legSequence;
//
//			log.info("Writing leg sequence: legId={} links={}", legId, links);
//			// write leg_sequence to table
//			try {
//				legSequenceStmt.setString(1, legId);
//				legSequenceStmt.setString(2, tripId);
//				legSequenceStmt.setString(3, state.agentId);
//				legSequenceStmt.setObject(4, links);
//				legSequenceStmt.addBatch();
//				// Batching heavily speeds up insertion - don't make every insertion a transaction (much slower than insert statement)
//				// from sqlite.org: "Actually, SQLite will easily do 50,000 or more INSERT statements per second on an average desktop computer. But it will only do a few dozen transactions per second.
//				// Transaction speed is limited by the rotational speed of your disk drive.
//				// A transaction normally requires two complete rotations of the disk platter, which on a 7200RPM disk drive limits you to about 60 transactions per second."
//
//				if (legBatchCount++ % 5_000 == 0) {
//					legSequenceStmt.executeBatch();
//					conn.commit();
//				}
//
//			} catch (SQLException ex) {
//				throw new RuntimeException(ex);
//			}
//
//		try {
//			this.writerLegSequences.write(this.factoryLegSequences.newGroup()
//				.append("leg_id", legId)
//				.append("trip_id", tripId)
//				.append("agent_id", state.agentId)
//				.append("leg_sequence", String.join("|", links)));
//		} catch (IOException ex) {
//			throw new RuntimeException(ex);
//		}
//
//		// --- NEW: Expand leg_sequence and store link-link pairs ---
//		for (int i = 0; i < links.size(); i++) {
//			String linkId = links.get(i);
//			int hour = (int) (event.getTime() / 3600); // Or use the hour from link_traversals
//			// Store all pairs (linkId, coLinkId) where coLinkId is every other link in the leg
//			for (String coLinkId : links) {
//				if (!linkId.equals(coLinkId)) { // Optional: Exclude self-pairs
//					linkLinkPairBuffer.add(new LinkLinkPair(linkId, coLinkId, hour));
//				}
//			}
//		}
//
//		// Flush buffer to Parquet periodically
//		if (linkLinkPairBuffer.size() >= 100_000) {
//			flushLinkLinkPairs();
//		}
//
////		legSequenceBuffer.add(new LegSequenceRecord(
////			legId,
////			state.agentId,
////			tripId,
////			links
////		));
//
//		state.legIndex++;
//		if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
//			state.tripIndex++;
//		}
//		state.legSequence.clear();
//
//		System.out.println("person " + event.getPersonId().toString() + " start activity " + event.getEventType().toString());
//	}
//
//	public Map<String, AgentState> getAgentStates() {
//		return agentStates;
//	}
//
//
//	public void buildLinkIndex() throws IOException, SQLException {
//
////		Connection duckConn = DriverManager.getConnection("jdbc:duckdb:");
////		duckConn.createStatement().execute("SET memory_limit='64GB'");
////		duckConn.createStatement().execute(
////			"ATTACH '" + outputDirectory + "/sla.db' AS sla (TYPE sqlite, READ_ONLY true)"
////		);
//
////		var rs = duckConn.createStatement().executeQuery(
////			"WITH expanded AS (" +
////				"    SELECT" +
////				"        lt.link_id," +
////				"        lt.hour," +
////				"        unnest(string_split(ls.leg_sequence, '|')) AS co_link_id" +
////				"    FROM read_parquet('" + outputDirectory + "/link-traversals.parquet') lt" +
////				"    JOIN read_parquet('" + outputDirectory + "/leg-sequences.parquet') ls" +
////				"        ON lt.leg_id = ls.leg_id" +
////				")" +
////				"SELECT link_id, hour, co_link_id, COUNT(*) AS count " +
////				"FROM expanded " +
////				"GROUP BY link_id, hour, co_link_id " +
////				"ORDER BY link_id, hour"
////		);
//
//		try (Connection duckConn = DriverManager.getConnection("jdbc:duckdb:")) {
//			duckConn.createStatement().execute(
//				"COPY (" +
//					"SELECT link_id, hour, co_link_id, COUNT(*) AS count " +
//					"FROM read_parquet('" + outputDirectory + "/link-link-pairs.parquet') " +
//					"GROUP BY link_id, hour, co_link_id " +
//					"ORDER BY link_id, hour" +
//					") TO '" + outputDirectory + "/link-index.parquet'"
//			);
//		} catch (SQLException ex) {
//			throw new RuntimeException(ex);
//		}
//			String schemaString = """
//				message link-index {
//				  required binary link_id (UTF8);
//				  required int32 hour;
//				  required binary co_link_id (UTF8);
//				  required int32 count;
//				}
//				""";
////			MessageType schema = MessageTypeParser.parseMessageType(schemaString);
////			SimpleGroupFactory factory = new SimpleGroupFactory(schema);
////			Path path = new Path(outputDirectory + "/link-index.parquet");
////			FileSystem fs = FileSystem.get(new Configuration());
////			if (fs.exists(path)) fs.delete(path, true);
////
////			java.nio.file.Path pathLinks = java.nio.file.Path.of(outputDirectory + "/link-index.csv");
//
////			try (
////				CSVPrinter csvWriter = new CSVPrinter(Files.newBufferedWriter(pathLinks),
////					CSVFormat.DEFAULT.withHeader("link_id", "hour", "co_link_id", "count"));
////				ParquetWriter<Group> writer = ExampleParquetWriter
////					.builder(HadoopOutputFile.fromPath(path, new Configuration()))
////					.withType(schema)
////					.withCompressionCodec(CompressionCodecName.ZSTD)
////					.withDictionaryEncoding(true)
////					.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
////					.withRowGroupSize(32L * 1024 * 1024)
////					.withPageSize(512 * 1024)
////					.build()
////			) {
////			while (rs.next()) {
////				String linkId = rs.getString("link_id");
////				int hour = rs.getInt("hour");
////				String coLinkId = rs.getString("co_link_id");
////				int count = rs.getInt("count");
//
////				// parquet
////				writer.write(factory.newGroup()
////					.append("link_id", linkId)
////					.append("hour", hour)
////					.append("co_link_id", coLinkId)
////					.append("count", count));
////
////				// csv
////				csvWriter.printRecord(linkId, hour, coLinkId, count);
////
////				// sqlite
////				linkIndexStmt.setString(1, linkId);
////				linkIndexStmt.setInt(2, hour);
////				linkIndexStmt.setString(3, coLinkId);
////				linkIndexStmt.setInt(4, count);
////				linkIndexStmt.addBatch();
////				if (linkBatchCount++ % 10_000 == 0) {
////					linkIndexStmt.executeBatch();
////					conn.commit();
////				}
////			}
////		}
////		duckConn.close();
////			}
//	}
//
//
//	public void finish() throws SQLException, IOException {
//
//		linkIndexStmt.executeBatch();
//		legSequenceStmt.executeBatch();
//		conn.commit();
//
//		writerLinkTraversals.close();
//		writerLegSequences.close();
//		writerLinkLinkPairs.close();
//
//		buildLinkIndex();
//
//		linkIndexStmt.executeBatch();
//		conn.commit();
//
//		conn.createStatement().execute("DROP INDEX IF EXISTS idx_link");
//		conn.createStatement().execute("DROP INDEX IF EXISTS idx_leg");
//		conn.createStatement().execute("CREATE INDEX idx_link ON link_index(link_id, hour)");
//		conn.createStatement().execute("CREATE INDEX idx_leg ON leg_sequences(agent_id, trip_id)");
//		conn.commit();
//		conn.close();
//	}
//
//	private void flushLinkLinkPairs() {
//		try {
//			for (LinkLinkPair pair : linkLinkPairBuffer) {
//				writerLinkLinkPairs.write(factoryLinkLinkPairs.newGroup()
//					.append("link_id", pair.linkId())
//					.append("co_link_id", pair.coLinkId())
//					.append("hour", pair.hour()));
//			}
//			linkLinkPairBuffer.clear();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//
//}
