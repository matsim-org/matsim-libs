package org.matsim.core.mobsim.queuesim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;

public class StaticFactoriesContainer {

	public static MobsimDriverAgent createQueuePersonAgent(Person p, QueueSimulation simulation) {
		return new DefaultAgentFactory( simulation ).createMobsimAgentFromPersonAndInsert( p ) ;
	}

}
