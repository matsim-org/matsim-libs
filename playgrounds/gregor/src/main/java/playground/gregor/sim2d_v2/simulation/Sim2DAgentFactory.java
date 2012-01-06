package playground.gregor.sim2d_v2.simulation;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.interfaces.Netsim;

public class Sim2DAgentFactory implements AgentFactory {


	protected final Netsim simulation;
	private final DefaultAgentFactory defaultAgentFactory;

	public Sim2DAgentFactory(final Netsim simulation) {
		this.simulation = simulation;
		this.defaultAgentFactory = new DefaultAgentFactory(simulation);
	}

	@Override
	public MobsimAgent createMobsimAgentFromPersonAndInsert(Person p) {
		MobsimDriverAgent pda = this.defaultAgentFactory.createMobsimAgentFromPersonAndInsert(p);
		return null;
	}

}
