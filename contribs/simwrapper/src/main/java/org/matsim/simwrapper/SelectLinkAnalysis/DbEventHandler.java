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
import java.sql.SQLException;

import java.util.*;

public class DbEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler,
	PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, VehicleEntersTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(DbEventHandler.class);
	private final Map<String, AgentState> agentStates = new HashMap<>();
	private final Map<Id<Vehicle>, Map<Id<Person>, String>> vehicleToPersonMap = new HashMap<>();
	private final Map<Id<Person>, String> modeToPersonMap = new HashMap<>();

	// DuckDB needs a number of rows - doesn't work correctly when bytes are specified instead.
	// default duckdb size - 122880 row group size

	int rowGroupSize;

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
	// to save space, don't need these for current implementation
	//              required binary trip_id (UTF8);
	//				required binary agent_id (UTF8);
	String schemaStringLegs = """
            message leg-sequences {
              required binary leg_id (UTF8);
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

	public DbEventHandler(String outputDirectory, Integer rowGroupSize) throws IOException {
		this.rowGroupSize = rowGroupSize;
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
			.build();

		if (fs.exists(pathLegSequences)) fs.delete(pathLegSequences, true);

		writerLegSequences = ExampleParquetWriter
			.builder(HadoopOutputFile.fromPath(pathLegSequences, conf))
			.withType(schemaLegSequences)
			.withCompressionCodec(CompressionCodecName.ZSTD)
			.withDictionaryEncoding("leg_id", true) // Explicitly for link_id
//			.withDictionaryEncoding("trip_id", true)
//			.withDictionaryEncoding("agent_id", true)
			.withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
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
//		String tripId = state.getTripId();
		String links = String.join("|", state.legSequence);

		try {
			writerLegSequences.write(factoryLegSequences.newGroup()
				.append("leg_id", legId)
//				.append("trip_id", tripId)
//				.append("agent_id", state.agentId)
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

	public void createComparisonDatasets(Integer rowGroupSize) throws SQLException, IOException {
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
		String partitionDirParquet = this.outputDirectory + "/hour-partition-parquet";
		String partitionDirCsv = this.outputDirectory + "/hour-partition-csv";
		String linkTraversalsCsvPathUncompressed =  this.outputDirectory + "/link-traversals-sorted.csv";
		String legSequencesCsvPathUncompressed =  this.outputDirectory + "/leg-sequences-sorted.csv";

		String rowGroupClause = (rowGroupSize != null) ? ", ROW_GROUP_SIZE " + rowGroupSize : "";
		System.out.println(rowGroupClause);

		duckConn.createStatement().execute(
			"SET preserve_insertion_order=false"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsSortedPath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD'" + rowGroupClause + ")"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesSortedPath + "' " +
				"(FORMAT PARQUET, CODEC 'ZSTD'" + rowGroupClause + ")"
		);

		for (int hour = 0; hour < 35; hour++) {
			String hourDir = partitionDirParquet + "/hour-" + hour;
			String hourCsvDir = partitionDirCsv  + "/hour-" + hour;

			fs.mkdirs(new Path(hourCsvDir));
			fs.mkdirs(new Path(hourDir));

			duckConn.createStatement().execute(
				"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') lt " +
					"  WHERE lt.hour = " + hour +
					"ORDER BY link_id, hour) " + " " +
					"TO '" + hourDir + "/link-traversals-sorted-" + hour + ".parquet" + "'" +
					"(FORMAT PARQUET, CODEC 'ZSTD'" + rowGroupClause + ")"
			);

			duckConn.createStatement().execute(
				"COPY (" +
//					"SELECT  lt.leg_id, ls.leg_sequence " +
					"SELECT DISTINCT lt.leg_id, ls.leg_sequence " +
					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
					"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
					"  ON lt.leg_id = ls.leg_id " +
					"  WHERE lt.hour = " + hour +
					"ORDER BY leg_id) " + " " +
					"TO '" + hourDir + "/leg-sequences-sorted-" + hour + ".parquet" + "'" +
					"(FORMAT PARQUET, CODEC 'ZSTD'" + rowGroupClause + ")"
			);

			duckConn.createStatement().execute(
				"COPY (SELECT * FROM read_parquet('" + linkTraversalsPath + "') lt " +
					"  WHERE lt.hour = " + hour +
					"ORDER BY link_id, hour) " + " " +
					"TO '" + hourCsvDir + "/link-traversals-sorted-" + hour + ".csv'" +
					"(FORMAT CSV, HEADER, DELIMITER ',')"
			);

			duckConn.createStatement().execute(
				"COPY (" +
//					"SELECT  lt.leg_id, ls.leg_sequence " +
					"SELECT DISTINCT lt.leg_id, ls.leg_sequence " +
					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
					"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
					"  ON lt.leg_id = ls.leg_id " +
					"  WHERE lt.hour = " + hour + " " +
					"ORDER BY leg_id) " +
					"TO '" + hourCsvDir + "/leg-sequences-sorted-" + hour + ".csv'" +
					"(FORMAT CSV, HEADER, DELIMITER ',')"
			);

		}


		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
				"ORDER BY link_id, hour) " +
				"TO '" + linkTraversalsCsvPathUncompressed + "' (FORMAT CSV, HEADER, DELIMITER ',')"
		);

		duckConn.createStatement().execute(
			"COPY (SELECT * FROM read_parquet('" + legSequencesSortedPath + "') " +
				"ORDER BY leg_id) " +
				"TO '" + legSequencesCsvPathUncompressed + "' (FORMAT CSV, HEADER, DELIMITER ',')"
		);

		java.nio.file.Path dbPath = java.nio.file.Path.of(outputDirectory + "/sla.sqlite3");
 		Files.deleteIfExists(dbPath);

		duckConn.createStatement().execute("INSTALL sqlite;");
		duckConn.createStatement().execute("LOAD sqlite;");

		duckConn.createStatement().execute(
			"ATTACH '" + outputDirectory + "/sla.sqlite3' AS sqlite_db (TYPE SQLITE)"
		);

		duckConn.createStatement().execute(
			"CREATE TABLE IF NOT EXISTS sqlite_db.link_traversals AS " +
				"SELECT * FROM read_parquet('" + linkTraversalsSortedPath + "') " +
				"ORDER BY (link_id, hour) "
		);
		duckConn.createStatement().execute(
			"CREATE TABLE IF NOT EXISTS sqlite_db.leg_sequences AS " +
				"SELECT ls.leg_id, ls.leg_sequence FROM read_parquet('" + legSequencesSortedPath + "') ls " +
				"ORDER BY (leg_id) "
		);

		duckConn.createStatement().execute(
			"CREATE INDEX IF NOT EXISTS idx_t ON sqlite_db.traversals(link_id, hour, leg_id)"
		);
		duckConn.createStatement().execute(
			"CREATE INDEX IF NOT EXISTS idx_s ON sqlite_db.sequences(leg_id)"
		);

		duckConn.createStatement().execute("DETACH sqlite_db");

		for (int hour = 0; hour < 35; hour++) {
			String hourDir = this.outputDirectory + "/hour-partition-sqlite/hour-" + hour;
			fs.mkdirs(new Path(hourDir));

			String sqlitePathHour = hourDir + "/sla-" + hour + ".sqlite3";

			java.nio.file.Path dbPathHour = java.nio.file.Path.of(sqlitePathHour);
			java.nio.file.Files.deleteIfExists(dbPathHour);

			duckConn.createStatement().execute(
				"ATTACH '" + dbPathHour + "' AS sqlite_db_hour (TYPE SQLITE)"
			);

			duckConn.createStatement().execute(
				"CREATE TABLE sqlite_db_hour.traversals AS " +
					"SELECT * " +
					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
					" WHERE lt.hour = " + hour + " " +
					"ORDER BY (link_id, hour) "
			);

			duckConn.createStatement().execute(
				"CREATE TABLE sqlite_db_hour.sequences AS " +
					"SELECT DISTINCT lt.leg_id, ls.leg_sequence " +
					"FROM read_parquet('" + linkTraversalsSortedPath + "') lt " +
					"JOIN read_parquet('" + legSequencesSortedPath + "') ls " +
					"  ON lt.leg_id = ls.leg_id " +
					" WHERE lt.hour = " + hour + " " +
					"ORDER BY (leg_id) "
			);

			duckConn.createStatement().execute(
				"CREATE INDEX IF NOT EXISTS idx_t ON sqlite_db_hour.traversals(link_id, hour, leg_id)"
			);
			duckConn.createStatement().execute(
				"CREATE INDEX IF NOT EXISTS idx_s ON sqlite_db_hour.sequences(leg_id)"
			);


			duckConn.createStatement().execute("DETACH sqlite_db_hour");
		}
	}

	public void finish() throws IOException {

		writerLinkTraversals.close();
		writerLegSequences.close();

		try {
			createComparisonDatasets(this.rowGroupSize);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}
}
