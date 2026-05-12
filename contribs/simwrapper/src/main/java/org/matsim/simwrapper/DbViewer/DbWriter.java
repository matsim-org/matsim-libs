package org.matsim.simwrapper.DbViewer;

import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DbWriter {
	private final Map<String, AgentState> agentRecords;
	private final DB db;
	private final Scenario scenario;
	private final String outputDirectory;

	public DbWriter(DbEventHandler dbEventHandler, DB db, Scenario scenario, String outputDirectory) {
		this.agentRecords = dbEventHandler.getAgentStates();
		this.db = db;
		this.scenario = scenario;
		this.outputDirectory = outputDirectory;
	}

	public void writeAgentTable() throws SQLException {

		try (
			// create a database connection
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + this.outputDirectory);
			Statement statement = conn.createStatement();
		) {
			Population population = scenario.getPopulation();
			if (population.getPersons().isEmpty()) return;

			// Step 1 — discover keys from first person
			Person firstPerson = population.getPersons()
				.values().iterator().next();
			List<String> keys = new ArrayList<>(
				firstPerson.getAttributes().getAsMap().keySet()
			);

			// Step 2 — CREATE TABLE dynamically
			String cols = keys.stream()
				.map(k -> "\"" + k + "\" TEXT")
				.collect(Collectors.joining(", "));
			conn.createStatement().execute(
				"CREATE TABLE IF NOT EXISTS agent " +
					"(agent_id TEXT PRIMARY KEY, " + cols + ")"
			);

			// Step 3 — PreparedStatement with dynamic placeholders
			String placeholders = Collections.nCopies(keys.size(), "?")
				.stream().collect(Collectors.joining(", "));
			PreparedStatement stmt = conn.prepareStatement(
				"INSERT INTO agent VALUES (?, " + placeholders + ")"
			);

			// Step 4 — batch insert
			conn.setAutoCommit(false);
			for (Person person : population.getPersons().values()) {
				Map<String, Object> attrs = person.getAttributes().getAsMap();

				stmt.setString(1, person.getId().toString());
				for (int i = 0; i < keys.size(); i++) {
					Object val = attrs.get(keys.get(i));
					// setObject handles null and any type — SQLite stores as TEXT
					stmt.setObject(i + 2, val != null ? val.toString() : null);
				}
				stmt.addBatch();
			}
			stmt.executeBatch();
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			e.printStackTrace(System.err);
		}
	}

	public void write() {
		try {
			HTreeMap<String, String> table = db.hashMap("agents",
				Serializer.STRING, Serializer.STRING).createOrOpen();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				String agentId = person.getId().toString();

				// Don't hardcode attribute keys — read whatever is there dynamically
				Map<String, Object> attributes = new HashMap<>();
				for (String key : person.getAttributes().getAsMap().keySet()) {
					attributes.put(key, person.getAttributes().getAttribute(key));
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to write to MapDB: " + e.getMessage());
		}

	}
}
