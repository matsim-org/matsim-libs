package org.matsim.pt.withinday;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Inject;

public class ScriptedTransitAgentFactory implements AgentFactory {

	private Netsim netsim;
	
	@Inject
	public ScriptedTransitAgentFactory(Netsim netsim) {
		this.netsim = netsim;
	}
	
	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		return ScriptedTransitAgent.createTransitAgent(p, netsim);
	}

}
