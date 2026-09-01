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
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
	final long rowGroupSize = 32L * 1024 * 1024;
	final int pageSize = 512 * 1024;

	ParquetWriter<Group> writerLinkTraversals;
	ParquetWriter<Group> sortedWriterLinkTraversals;
//	SimpleGroupFactory factoryLinkTraversals;
	ParquetWriter<Group> writerLegSequences;
	ParquetWriter<Group> sortedWriterLegSequences;
//	SimpleGroupFactory factoryLegSequences;
	Configuration conf = new Configuration();
	FileSystem fs = FileSystem.get(new Configuration());

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

	SimpleGroupFactory factoryLinkTraversals = new SimpleGroupFactory(schemaLinkTraversals);
	SimpleGroupFactory factoryLegSequences = new SimpleGroupFactory(schemaLegSequences);

	String outputDirectory;
	Path pathLinkTraversals;
	Path pathLegSequences;

	private record DictionaryCalibration(
		long rowGroupSizeRows,
		long dictionarySizeLimit,
		long stringDictionaryPageSizeLimitBytes
	) {}

	private DictionaryCalibration calibrateDictionaryParams(Connection duckConn, String rawPath, String schema) throws SQLException {
		long totalRows;
		try (var stmt = duckConn.createStatement();
		     var rs = stmt.executeQuery("SELECT COUNT(*) AS n FROM read_parquet('" + rawPath + "')")) {
			rs.next();
			totalRows = rs.getLong("n");
		}

		// Worst-case per-group NDV driver: agent_id / leg_id aren't correlated with the
		// link_id/hour sort order, so their per-row-group cardinality approaches
		// min(rows_per_group, global NDV). link_id benefits from sort locality so we
		// don't need to calibrate against it.
		record ColStats(long ndv, double avgLen) {}
		Map<String, ColStats> stats = new HashMap<>();
		List<String> colList = List.of();
		if (schema.equals("links")) {
			colList = List.of("link_id", "agent_id", "leg_id", "mode");
		} else if (schema.equals("legs")) {
			colList = List.of("leg_id", "trip_id", "agent_id");
		}
		for (String col : colList) {
			try (var stmt = duckConn.createStatement();
			     var rs = stmt.executeQuery(
					 "SELECT CAST(approx_count_distinct(" + col + ") AS BIGINT) AS ndv, " +
						 "AVG(LENGTH(" + col + ")) AS avg_len " +
						 "FROM read_parquet('" + rawPath + "')")) {
				rs.next();
				stats.put(col, new ColStats(rs.getLong("ndv"), rs.getDouble("avg_len")));
			}
		}

		// Pick a target row group row count. This keeps row groups from ballooning
		// regardless of dataset size/compressibility - default DuckDB tips suggest
		// 250k-500k rows for analytics tables, so we use that as an upper bound and
		// never exceed it even if the dataset is small enough that fewer groups would do.
		long targetRowGroupRows = Math.min(500_000L, Math.max(50_000L, totalRows / 20));

		// Worst-case NDV within a single row group of that size, for the highest-cardinality
		// uncorrelated column.
		long worstCaseNdvPerGroup = stats.values().stream()
			.mapToLong(s -> Math.min(targetRowGroupRows, s.ndv()))
			.max().orElse(targetRowGroupRows);

		// Add 50% headroom so we don't sit right at the edge (values won't be perfectly
		// evenly distributed across row groups in practice).
		long dictionarySizeLimit = (long) (worstCaseNdvPerGroup * 1.5);

		double worstCaseAvgLen = stats.values().stream()
			.mapToDouble(ColStats::avgLen)
			.max().orElse(20.0);
		long stringDictPageBytes = (long) (dictionarySizeLimit * worstCaseAvgLen * 1.5);
		// Floor at the DuckDB default (1MB) so we never regress below stock behavior.
		stringDictPageBytes = Math.max(stringDictPageBytes, 1_000_000L);

		log.info("Calibrated writer params for " + schema + ": rowGroupRows={}, dictionarySizeLimit={}, " +
				"stringDictPageBytes={} (from totalRows={}, worstCaseNdvPerGroup={})",
			targetRowGroupRows, dictionarySizeLimit, stringDictPageBytes,
			totalRows, worstCaseNdvPerGroup);

		return new DictionaryCalibration(targetRowGroupRows, dictionarySizeLimit, stringDictPageBytes);
	}

	public DbEventHandler(String outputDirectory) throws IOException {
		this.outputDirectory = outputDirectory;
		pathLinkTraversals = new Path(outputDirectory + "/link-traversals.parquet");
		pathLegSequences = new Path(outputDirectory + "/leg-sequences.parquet");

		if (fs.exists(pathLinkTraversals)) fs.delete(pathLinkTraversals, true);

		/* INITIAL WRITERS WHILE READING EVENTS */

		writerLinkTraversals = ExampleParquetWriter
			.builder(HadoopOutputFile.fromPath(pathLinkTraversals, conf))
			.withType(schemaLinkTraversals)
			.withCompressionCodec(CompressionCodecName.ZSTD)
			.withDictionaryEncoding(true)
			.withDictionaryEncoding("link_id", true) // Explicitly for link_id
			.withDictionaryEncoding("agent_id", true)
			.withDictionaryEncoding("leg_id", true)
			.withDictionaryEncoding("hour", true)
			.withDictionaryEncoding("mode", true)   // Explicitly for mode
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

		if (fs.exists(pathLegSequences)) fs.delete(pathLegSequences, true);

		writerLegSequences = ExampleParquetWriter
			.builder(HadoopOutputFile.fromPath(pathLegSequences, conf))
			.withType(schemaLegSequences)
			.withCompressionCodec(CompressionCodecName.ZSTD)
			.withDictionaryEncoding("leg_id", true) // Explicitly for link_id
			.withDictionaryEncoding("trip_id", true)
			.withDictionaryEncoding("agent_id", true)
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

		String linkTraversalsPath = this.pathLinkTraversals.toString();
		String linkTraversalsSortedPath = this.outputDirectory + "/link-traversals-sorted.parquet";
		String legSequencesPath = this.pathLegSequences.toString();
		String legSequencesSortedPath = this.outputDirectory + "/leg-sequences-sorted.parquet";

		String linkTraversalsCsvPath =  this.outputDirectory + "/link-traversals-sorted.csv.zst";
		String legSequencesCsvPath =  this.outputDirectory + "/leg-sequences-sorted.csv.zst";

		String linkTraversalsCsvPathUncompressed =  this.outputDirectory + "/link-traversals-sorted.csv";
		String tempPath = linkTraversalsSortedPath + ".tmp";
		String tempPathLegs = legSequencesSortedPath + ".tmp";

		if (fs.exists(new Path(tempPath))) fs.delete(new Path(tempPath), true);
		if (fs.exists(new Path(tempPathLegs))) fs.delete(new Path(tempPathLegs), true);

		duckConn.createStatement().execute(
			"SET preserve_insertion_order=false"
		);

//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') " +
//				"ORDER BY link_id, hour) " +
//				"TO '" + linkTraversalsSortedPath + "' " +
//				"(FORMAT PARQUET, STRING_DICTIONARY_PAGE_SIZE_LIMIT 10000000, CODEC 'ZSTD', ROW_GROUP_SIZE_BYTES " + rowGroupSize + ")"
//		);

		DictionaryCalibration calLinks = calibrateDictionaryParams(duckConn, linkTraversalsPath, "links");

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsSortedPath + "' " +
				"(FORMAT PARQUET, " +
				"ROW_GROUP_SIZE " + calLinks.rowGroupSizeRows() + ", " +
				"ROW_GROUP_SIZE_BYTES " + rowGroupSize + ", " +
				"STRING_DICTIONARY_PAGE_SIZE_LIMIT " + calLinks.stringDictionaryPageSizeLimitBytes() + ", " +
				"DICTIONARY_SIZE_LIMIT " + calLinks.dictionarySizeLimit() + ", " +
				"CODEC 'ZSTD') "
		);

		DictionaryCalibration calLegs = calibrateDictionaryParams(duckConn, legSequencesPath, "legs");

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesSortedPath + "' " +
				"(FORMAT PARQUET, " +
				"ROW_GROUP_SIZE " + calLegs.rowGroupSizeRows() + ", " +
				"ROW_GROUP_SIZE_BYTES " + rowGroupSize + ", " +
				"STRING_DICTIONARY_PAGE_SIZE_LIMIT " + calLegs.stringDictionaryPageSizeLimitBytes() + ", " +
				"DICTIONARY_SIZE_LIMIT " + calLegs.dictionarySizeLimit() + ", " +
				"CODEC 'ZSTD') "
		);

//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + legSequencesPath + "') " +
//				"ORDER BY leg_id) " +
//				"TO '" + legSequencesSortedPath + "' " +
//				"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE_BYTES " + rowGroupSize + ")"
//		);


		// 2. Calibrate NDV from that file's real per-row-group stats
//		long calibratedNdv;
//		try (var stmt = duckConn.createStatement();
//		     var rs = stmt.executeQuery(
//				 "SELECT CAST(approx_count_distinct(link_id) AS BIGINT) AS ndv " +
//					 "FROM read_parquet('" + linkTraversalsSortedPath + "')")) {
//			rs.next();
//			calibratedNdv = rs.getLong("ndv");
//		}
//
//		long calibratedNdvLegs;
//		try (var stmt = duckConn.createStatement();
//		     var rs = stmt.executeQuery(
//				 "SELECT CAST(approx_count_distinct(leg_id) AS BIGINT) AS ndv " +
//					 "FROM read_parquet('" + legSequencesSortedPath + "')")) {
//			rs.next();
//			calibratedNdvLegs = rs.getLong("ndv");
//		}

//		sortedWriterLinkTraversals = ExampleParquetWriter
//			.builder(HadoopOutputFile.fromPath(new Path(tempPath), conf))
//			.withType(schemaLinkTraversals)
//			.withCompressionCodec(CompressionCodecName.ZSTD)
//			.withDictionaryEncoding(true) // Enable for all columns
////			.withDictionaryEncoding("link_id", true) // Explicitly for link_id
////			.withDictionaryEncoding("agent_id", true)
////			.withDictionaryEncoding("leg_id", true)
////			.withDictionaryEncoding("hour", true)
////			.withDictionaryEncoding("mode", true)   // Explicitly for mode
//			.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
//			// Need to explain these row and page sizes
//			.withRowGroupSize(rowGroupSize)
//			.withPageSize(pageSize)
////			.withBloomFilterEnabled(false)
////			.withBloomFilterEnabled("link_id", true)
////			.withBloomFilterNDV("link_id", calibratedNdv)
//			.build();
//
//		sortedWriterLegSequences = ExampleParquetWriter
//			.builder(HadoopOutputFile.fromPath(new Path(tempPathLegs), conf))
//			.withType(schemaLegSequences)
//			.withCompressionCodec(CompressionCodecName.ZSTD)
//			.withDictionaryEncoding(true) // Enable for all columns
//			.withDictionaryEncoding("leg_id", true) // Explicitly for link_id
//			.withDictionaryEncoding("trip_id", true)
//			.withDictionaryEncoding("agent_id", true)
//			.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
//			// Need to explain these row and page sizes
//			.withRowGroupSize(rowGroupSize)
//			.withPageSize(pageSize)
////			.withBloomFilterEnabled(false)
////			.withBloomFilterEnabled("leg_id", true)
////			.withBloomFilterNDV("leg_id", calibratedNdvLegs)
//			.build();



//		 3. Rewrite the SAME path via Java, using the calibrated NDV — this becomes the final file
//		    (read from linkTraversalsSortedPath itself, since it's already sorted — no need to re-sort)
//		Path pathLinkTraversalsSorted = new Path(linkTraversalsSortedPath);
//		try (var stmt = duckConn.createStatement();
//		     var rs = stmt.executeQuery(
//				 "SELECT link_id, agent_id, leg_id, hour, mode FROM read_parquet('" + linkTraversalsSortedPath + "')")) {
//			while (rs.next()) {
//				sortedWriterLinkTraversals.write(factoryLinkTraversals.newGroup()
//					.append("link_id", rs.getString("link_id"))
//					.append("agent_id", rs.getString("agent_id"))
//					.append("leg_id", rs.getString("leg_id"))
//					.append("hour", rs.getInt("hour"))
//					.append("mode", rs.getString("mode")));
//			}
//			// fully drain rs into memory or a temp list first, THEN delete+rewrite the path —
//			// can't read and overwrite the same file at once
//
//		}

//		Path pathLegSequencesSortedPath = new Path(legSequencesSortedPath);
//		try (var stmt = duckConn.createStatement();
//		     var rs = stmt.executeQuery(
//				 "SELECT leg_id, trip_id, agent_id, leg_sequence FROM read_parquet('" + legSequencesSortedPath + "')")) {
//			while (rs.next()) {
//				sortedWriterLegSequences.write(factoryLegSequences.newGroup()
//					.append("leg_id", rs.getString("leg_id"))
//					.append("trip_id", rs.getString("trip_id"))
//					.append("agent_id", rs.getString("agent_id"))
//					.append("leg_sequence", rs.getString("leg_sequence")));
//			}
//			// fully drain rs into memory or a temp list first, THEN delete+rewrite the path —
//			// can't read and overwrite the same file at once
//
//		}


		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsCsvPath + "' (FORMAT CSV, HEADER, DELIMITER ',', COMPRESSION zstd)"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesSortedPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesCsvPath + "' (FORMAT CSV, HEADER, DELIMITER ',', COMPRESSION zstd)"
		);

// 		this deletion already happens in AgentTabe.java, which runs before this method.
//		java.nio.file.Path dbPath = java.nio.file.Path.of(outputDirectory + "/sla.sqlite3");
// 		Files.deleteIfExists(dbPath);

		duckConn.createStatement().execute(
			"ATTACH '" + outputDirectory + "/sla.sqlite3' AS sqlite_db (TYPE SQLITE)"
		);

		duckConn.createStatement().execute(
			"CREATE TABLE IF NOT EXISTS sqlite_db.link_traversals AS " +
				"SELECT * FROM read_parquet('" + outputDirectory + "/link-traversals-sorted.parquet')"
		);
		duckConn.createStatement().execute(
			"CREATE TABLE IF NOT EXISTS sqlite_db.leg_sequences AS " +
				"SELECT * FROM read_parquet('" + outputDirectory + "/leg-sequences-sorted.parquet')"
		);

//		fs.delete(new Path(linkTraversalsSortedPath), false);
//		fs.rename(new Path(tempPath), new Path(linkTraversalsSortedPath));
//		fs.delete(new Path(legSequencesSortedPath), false);
//		fs.rename(new Path(tempPathLegs), new Path(legSequencesSortedPath));
//
//
//		log.info("Calibrated NDV determined to be: {}", calibratedNdv);
//		log.info("Calibrated NDV for legs determined to be: {}", calibratedNdvLegs);
//
//
//		sortedWriterLinkTraversals.close();
//		sortedWriterLegSequences.close();
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
