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

	// DuckDB needs a number of rows - doesn't work correctly when bytes are specified instead.
	// sizes for tests:
	// 4096
	// 30720
	// default duckdb size - 122880
	// 491520
	// 983040

	final int rowGroupSize = 30_720;
	final int pageSize = 512 * 1024;

	ParquetWriter<Group> writerLinkTraversals;
	ParquetWriter<Group> writerLegSequences;
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
			.withPageSize(pageSize)
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
			.withPageSize(pageSize)
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

		String linkTraversalCountsDir = this.outputDirectory + "/link-traversals-counts";

		String linkTraversalsCsvPath =  this.outputDirectory + "/link-traversals-sorted.csv.zst";
		String legSequencesCsvPath =  this.outputDirectory + "/leg-sequences-sorted.csv.zst";

		String linkTraversalsCsvPathUncompressed =  this.outputDirectory + "/link-traversals-sorted.csv";
		String legSequencesCsvPathUncompressed =  this.outputDirectory + "/leg-seqeunces-sorted.csv";

		duckConn.createStatement().execute(
			"SET preserve_insertion_order=false"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsSortedPath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE " + rowGroupSize + ")"
		);

		// 2. leg-sequences: DuckDB sort first, into a temp path used purely for calibration + as read source
//		String legSequencesDuckSortedPath =  this.outputDirectory + "/leg-sequences-sorted-tmp.parquet";
		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesSortedPath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE " + rowGroupSize + ")"
		);

		// last optmization test - this one wins:

		for (int hour = 0; hour < 35; hour++) {
			String hourDir = linkTraversalCountsDir + "/hour-" + hour;
			String hourCsvDir = linkTraversalCountsDir + "-csv" + "/hour-" + hour;

			fs.mkdirs(new Path(hourCsvDir));
			fs.mkdirs(new Path(hourDir));

			duckConn.createStatement().execute(
				"COPY (" +
					"SELECT lt.link_id, lt.hour, lt.leg_id, lt.agent_id, ls.leg_sequence " +
					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
					"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
					"  ON lt.leg_id = ls.leg_id " +
					"  WHERE lt.hour = " + hour +
					"ORDER BY lt.link_id, lt.hour" +
					") TO '" + hourDir + "/link-traversals-with-sequence-" + hour + ".parquet" + "'" +
					"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE " + rowGroupSize + ")"
			);

			duckConn.createStatement().execute(
				"COPY (" +
					"SELECT lt.link_id, lt.hour, lt.leg_id, lt.agent_id, ls.leg_sequence " +
					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
					"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
					"  ON lt.leg_id = ls.leg_id " +
					"WHERE lt.hour = " + hour + " " +
					"ORDER BY lt.link_id, lt.hour" +
					") TO '" + hourCsvDir + "/link-traversals-with-sequence-" + hour + ".csv.zst'" +
					"(FORMAT CSV, HEADER, DELIMITER ',', COMPRESSION zstd)"
			);

		}


		//
//				duckConn.createStatement().execute(
//					"COPY (" +
//						"WITH sequences AS (" +
//						"  SELECT lt.link_id, lt.hour, " +
//						"         UNNEST(string_split(ls.leg_sequence, '|')) AS co_link_id " +
//						"  FROM read_parquet('" + linkTraversalsPath + "') lt " +
//						"  JOIN read_parquet('" + legSequencesPath + "') ls " +
//						"    ON lt.leg_id = ls.leg_id " +
//						"  WHERE lt.hour = " + hour +
//						") " +
//						"SELECT link_id, hour, co_link_id, COUNT(*) AS traversal_count " +
//						"FROM sequences " +
//						"GROUP BY link_id, hour, co_link_id " +
//						"ORDER BY link_id" +
//						") TO '" + hourDir + "/data_0.parquet' " +
//						"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE " + rowGroupSize + ")"
//				);
//			}

/*		String linkTraversalsWithSequencePath = this.outputDirectory + "/link-traversals-with-sequence.parquet";

		duckConn.createStatement().execute(
			"COPY (" +
				"SELECT lt.link_id, lt.hour, lt.leg_id, ls.leg_sequence " +
				"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
				"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
				"  ON lt.leg_id = ls.leg_id " +
				"ORDER BY lt.link_id, lt.hour" +
				") TO '" + linkTraversalsWithSequencePath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE " + rowGroupSize + ")"
		);*/

/*		String linkCoTraversalCountsPath = this.outputDirectory + "/link-cotraversal-counts.parquet";

		duckConn.createStatement().execute(
			"COPY (" +
				"WITH sequences AS (" +
				"  SELECT lt.link_id, lt.hour, " +
				"         UNNEST(string_split(ls.leg_sequence, '|')) AS co_link_id " +
				"  FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
				"  JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
				"    ON lt.leg_id = ls.leg_id" +
				") " +
				"SELECT link_id, hour, co_link_id, COUNT(*) AS traversal_count " +
				"FROM sequences " +
				"GROUP BY link_id, hour, co_link_id " +
				"ORDER BY link_id, hour" +
				") TO '" + linkCoTraversalCountsPath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD', ROW_GROUP_SIZE " + rowGroupSize + ")"
		);*/
/*//		// 3. calibrate NDV from the DuckDB-written file's real per-row-group stats
		long calibratedLegNdv;
		try (var stmt = duckConn.createStatement();
		     var rs = stmt.executeQuery(
				 "SELECT CAST(quantile_cont(stats_distinct_count, 0.95) AS BIGINT) AS p95 " +
					 "FROM parquet_metadata('" + legSequencesDuckSortedPath + "') WHERE path_in_schema = 'leg_id'")) {
			rs.next();
			calibratedLegNdv = rs.getLong("p95");
		}
		log.info("Calibrated leg_id NDV per row group: {}", calibratedLegNdv);

		if (calibratedLegNdv < 2) {
			try (var stmt = duckConn.createStatement();
			     var rs = stmt.executeQuery(
					 "SELECT count(DISTINCT leg_id) AS p95 " +
						 "FROM read_parquet('" + legSequencesDuckSortedPath + "')")) {
				rs.next();
				calibratedLegNdv = rs.getLong("p95");
			}
			log.info("New Calibrated leg_id NDV per row group: {}", calibratedLegNdv);
		}

		// 4. rewrite via Java, forcing dictionary off + bloom filter on for leg_id
		Path pathLegSequencesSorted = new Path(legSequencesSortedPath);
		if (fs.exists(pathLegSequencesSorted)) fs.delete(pathLegSequencesSorted, true);

		ParquetWriter<Group> sortedWriterLegSequences = ExampleParquetWriter
			.builder(HadoopOutputFile.fromPath(pathLegSequencesSorted, conf))
			.withType(schemaLegSequences)
			.withCompressionCodec(CompressionCodecName.ZSTD)
			.withDictionaryEncoding(true)
//			.withDictionaryEncoding("leg_id", false)
			.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
			.withRowGroupRowCountLimit(rowGroupSize)
//			.withRowGroupSize(rowGroupSize)
			.withPageSize(pageSize)
			.withBloomFilterEnabled(true)
			.withBloomFilterEnabled("leg_id", true)
			.withBloomFilterNDV("leg_id", calibratedLegNdv)
			.build();

		try (var stmt = duckConn.createStatement();
		     var rs = stmt.executeQuery(
				 "SELECT leg_id, trip_id, agent_id, leg_sequence FROM read_parquet('" + legSequencesDuckSortedPath + "')")) {
			while (rs.next()) {
				sortedWriterLegSequences.write(factoryLegSequences.newGroup()
					.append("leg_id", rs.getString("leg_id"))
					.append("trip_id", rs.getString("trip_id"))
					.append("agent_id", rs.getString("agent_id"))
					.append("leg_sequence", rs.getString("leg_sequence")));
			}
		}
		sortedWriterLegSequences.close();

		// 5. clean up the temp calibration file
		fs.delete(new Path(legSequencesDuckSortedPath), false);*/

//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
//				"ORDER BY link_id, hour) " +
//				"TO '" + linkTraversalsCsvPath + "' (FORMAT CSV, HEADER, DELIMITER ',', COMPRESSION zstd)"
//		);
//
//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + legSequencesSortedPath + "') " +
//				"ORDER BY leg_id) " +
//				"TO '" + legSequencesCsvPath + "' (FORMAT CSV, HEADER, DELIMITER ',', COMPRESSION zstd)"
//		);

//		for (int hour = 0; hour < 35; hour++) {
//			String hourDir = this.outputDirectory + "/link-cotraversal-counts-csv/hour-" + hour;
//			fs.mkdirs(new Path(hourDir));
//
//			duckConn.createStatement().execute(
//				"COPY (" +
//					"SELECT lt.link_id, lt.hour, lt.leg_id, ls.leg_sequence " +
//					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
//					"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
//					"  ON lt.leg_id = ls.leg_id " +
//					"WHERE lt.hour = " + hour + " " +
//					"ORDER BY lt.link_id, lt.hour" +
//					") TO '" + hourDir + "/link-traversals-with-sequence-uncompressed-" + hour + ".csv' " +
//					"(FORMAT CSV, HEADER, DELIMITER ',')"
//			);
//		}

//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
//				"ORDER BY link_id, hour) " +
//				"TO '" + linkTraversalsCsvPathUncompressed + "' (FORMAT CSV, HEADER, DELIMITER ',')"
//		);
//
//		duckConn.createStatement().execute(
//			"COPY (SELECT * FROM read_parquet('" + legSequencesSortedPath + "') " +
//				"ORDER BY leg_id) " +
//				"TO '" + legSequencesCsvPathUncompressed + "' (FORMAT CSV, HEADER, DELIMITER ',')"
//		);

// 		this deletion already happens in AgentTabe.java, which runs before this method.
//		java.nio.file.Path dbPath = java.nio.file.Path.of(outputDirectory + "/sla.sqlite3");
// 		Files.deleteIfExists(dbPath);

//		duckConn.createStatement().execute(
//			"ATTACH '" + outputDirectory + "/sla.sqlite3' AS sqlite_db (TYPE SQLITE)"
//		);
//
//		duckConn.createStatement().execute(
//			"CREATE TABLE IF NOT EXISTS sqlite_db.link_traversals AS " +
//				"SELECT * FROM read_parquet('" + outputDirectory + "/link-traversals-sorted.parquet')"
//		);
//		duckConn.createStatement().execute(
//			"CREATE TABLE IF NOT EXISTS sqlite_db.leg_sequences AS " +
//				"SELECT * FROM read_parquet('" + outputDirectory + "/leg-sequences-sorted.parquet')"
//		);

//		for (int hour = 0; hour < 35; hour++) {
//			String hourDir = this.outputDirectory + "/link-cotraversal-counts-sqlite/hour-" + hour;
//			fs.mkdirs(new Path(hourDir));
//			String sqlitePath = hourDir + "/cotraversals-" + hour + ".sqlite3";
//
//			java.nio.file.Path dbPath = java.nio.file.Path.of(sqlitePath);
//			java.nio.file.Files.deleteIfExists(dbPath);
//
//			duckConn.createStatement().execute(
//				"ATTACH '" + sqlitePath + "' AS sqlite_db (TYPE SQLITE)"
//			);
//
//			duckConn.createStatement().execute(
//				"CREATE TABLE sqlite_db.cotraversals AS " +
//					"SELECT lt.link_id, lt.hour, lt.leg_id, lt.agent_id, ls.leg_sequence " +
//					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
//					"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
//					"  ON lt.leg_id = ls.leg_id " +
//					"WHERE lt.hour = " + hour
//			);
//
//			duckConn.createStatement().execute("DETACH sqlite_db");
//		}
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
}
