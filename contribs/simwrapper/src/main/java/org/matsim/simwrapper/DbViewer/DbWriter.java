package org.matsim.simwrapper.DbViewer;

import org.mapdb.DB;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public class DbWriter {

	private final AgentState agentState;
	private DbEventListener DbEventListener;

	private final IdMap<Person, Plan> agentRecords = new IdMap<>(Person.class);


	public DbWriter(DbEventHandler dbEventHandler, DB db, AgentState agentState) {
		this.agentState = agentState;
	}
}
