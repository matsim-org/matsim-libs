package org.matsim.contrib.carsharing.relocation.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class RelocationAgentFactory {
	private Scenario scenario;

	public RelocationAgentFactory(Scenario scenario) {
		this.scenario = scenario;
	}

	public RelocationAgent createRelocationAgent(Id<Person> id, String companyId, Id<Link> relocationAgentBaseLinkId) {
		RelocationAgent agent = new RelocationAgent(id, companyId, relocationAgentBaseLinkId, this.scenario);

		return agent;
	}

}
