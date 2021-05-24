package org.matsim.contrib.carsharing.relocation.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

public class RelocationAgentFactory {
	private Scenario scenario;
	private Network network;
	public RelocationAgentFactory(Scenario scenario, Network network) {
		this.scenario = scenario;
		this.network = network;
	}

	public RelocationAgent createRelocationAgent(Id<Person> id, String companyId, Id<Link> relocationAgentBaseLinkId) {
		RelocationAgent agent = new RelocationAgent(id, companyId, relocationAgentBaseLinkId, this.scenario, this.network);

		return agent;
	}

}
