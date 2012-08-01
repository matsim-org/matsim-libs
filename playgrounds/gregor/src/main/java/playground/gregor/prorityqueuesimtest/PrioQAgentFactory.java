package playground.gregor.prorityqueuesimtest;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.gregor.sim2d_v3.simulation.floor.Agent2D;


public class PrioQAgentFactory implements AgentFactory {


	protected final Netsim simulation;
	private final DefaultAgentFactory defaultAgentFactory;
	

	public PrioQAgentFactory(final Netsim simulation, Scenario sc) {
		this.simulation = simulation;
		this.defaultAgentFactory = new DefaultAgentFactory(simulation);
	}

	
	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		MobsimDriverAgent pda = this.defaultAgentFactory.createMobsimAgentFromPerson(p);
		Agent2D agent = new Agent2D(pda, null, null, null, null);
		return (MobsimAgent) agent;
	}

}
