package org.matsim.simwrapper.SelectLinkAnalysis;

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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.*;

public class DbEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler,
	PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, VehicleEntersTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(DbEventHandler.class);
	private final Map<String, AgentState> agentStates = new HashMap<>();
	private final Map<Id<Vehicle>, Map<Id<Person>, String>> vehicleToPersonMap = new HashMap<>();
	private final Map<Id<Person>, String> modeToPersonMap = new HashMap<>();

	// 64MB - change 64 (long type) to something like 32 for 32MB for example.
	// page size remaining the same for this test
	final long rowGroupSize = 64L * 1024 * 1024;
	final int pageSize = 512 * 1024;

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
			// Need to explain these row and page sizes
			.withRowGroupSize(rowGroupSize)
			.withPageSize(pageSize)
//			originally thought I had to set a bloom filter here, but then the DuckDB copy operation later
//			in this script which sorts this parquet file by hour and link_id removed the bloom filter.
//			This is not a problem since DuckDB states: "As of the last feature release (1.2.0), DuckDB
//			supports both reading and writing of Parquet Bloom filters. This happens completely transparently
//			to users, no additional action or configuration is required."


//			.withBloomFilterEnabled(true)
//			.withBloomFilterEnabled("linkId", true)
//			// best to set this to distinct num links, if possible.
//			// Helps parquet reader with Bloom filter probability when it knows the amount of distinct values
//			// EXAMPLE: running the following command in terminal (after entering duckdb environment by typing
//			// duckdb) gives you distinct count:
//			//		SELECT approx_count_distinct(l) AS ndv
//			//		FROM (
//			//			SELECT unnest(linkId) AS l
//			//			FROM read_avro('network.avro')
//			//		);
//			.withBloomFilterNDV("linkId", 305298)

			.build();

		Path pathLegSequences = new Path(outputDirectory + "/leg-sequences.parquet");
		if (fs.exists(pathLegSequences)) fs.delete(pathLegSequences, true);

		writerLegSequences = ExampleParquetWriter
			.builder(HadoopOutputFile.fromPath(pathLegSequences, conf))
			.withType(schemaLegSequences)
			.withCompressionCodec(CompressionCodecName.ZSTD)
			.withDictionaryEncoding(true)
			.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
			// just took recommended page and row group sizes: https://parquet.apache.org/docs/file-format/configurations/
			.withRowGroupSize(rowGroupSize)
			.withPageSize(pageSize)
//			.withBloomFilterEnabled(true)
//			.withBloomFilterEnabled("leg_id", true)
			// best to set this to distinct num legs, if possible. For this, you need to let this
			// program run once to generate legs file first.
			// Helps parquet reader with Bloom filter probability when it knows the amount of distinct values
			// EXAMPLE: running the following command in terminal (after entering duckdb environment by typing
			// duckdb) gives you distinct count:
			//		SELECT approx_count_distinct(l) AS ndv
			//		FROM (
			//			SELECT leg_id AS l
			//			FROM read_parquet('leg-sequences-sorted.parquet')
//			.withBloomFilterNDV("leg_id", 364447)
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

		try (var stmt = duckConn.createStatement();
		     var rs = stmt.executeQuery("SELECT version()")) {

			if (rs.next()) {
				log.info("DuckDB JDBC version: {}", rs.getString(1));
			}
		}

		String linkTraversalsPath = this.outputDirectory + "/link-traversals.parquet";
		String linkTraversalsSortedPath = this.outputDirectory + "/link-traversals-sorted.parquet";
		String legSequencesPath = this.outputDirectory + "/leg-sequences.parquet";
		String legSequencesSortedPath = this.outputDirectory + "/leg-sequences-sorted.parquet";

		String linkTraversalsCsvPath =  this.outputDirectory + "/link-traversals-sorted.csv.zst";
		String legSequencesCsvPath =  this.outputDirectory + "/leg-sequences-sorted.csv.zst";

		String linkTraversalsCsvPathUncompressed =  this.outputDirectory + "/link-traversals-sorted.csv";

		duckConn.createStatement().execute(
			"SET preserve_insertion_order=false"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsSortedPath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE_BYTES " + rowGroupSize + ")"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesSortedPath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE_BYTES " + rowGroupSize + ")"
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
