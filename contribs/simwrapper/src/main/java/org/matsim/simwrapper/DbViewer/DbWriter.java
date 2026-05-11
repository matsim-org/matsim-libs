package org.matsim.simwrapper.DbViewer;

import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.Map;

public class DbWriter {
	private final Map<String, AgentState> agentRecords;
	private final DB db;

	public DbWriter(DbEventHandler dbEventHandler, DB db) {
		this.agentRecords = dbEventHandler.getAgentStates();
		this.db = db;
	}

	public void write() {
		try {
			HTreeMap<String, String> table = db.hashMap("agents",
					Serializer.STRING, Serializer.STRING).createOrOpen();
			for (AgentState state : agentRecords.values()) {
				table.put(state.agentId, String.join("|", state.linkSequence));
			}
		} catch (Exception e) {
			System.err.println("Failed to write to MapDB: " + e.getMessage());
		}
	}
}
