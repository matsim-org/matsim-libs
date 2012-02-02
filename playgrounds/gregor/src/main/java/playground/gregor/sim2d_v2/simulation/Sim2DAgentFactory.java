package playground.gregor.sim2d_v2.simulation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.interfaces.Netsim;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalAgentRepresentation;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.LinkSwitcher;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.MentalLinkSwitcher;

public class Sim2DAgentFactory implements AgentFactory {


	protected final Netsim simulation;
	private final DefaultAgentFactory defaultAgentFactory;
	private final Scenario sc;
	private LinkSwitcher mlsw;
	

	public Sim2DAgentFactory(final Netsim simulation, Scenario sc) {
		this.simulation = simulation;
		this.defaultAgentFactory = new DefaultAgentFactory(simulation);
		this.sc = sc;
		Sim2DConfigGroup s2d = (Sim2DConfigGroup) sc.getConfig().getModule("sim2d");
		
		if (s2d.isEnableMentalLinkSwitch()){
			this.mlsw = new MentalLinkSwitcher(sc);
		} else {
			this.mlsw = new LinkSwitcher() {
				@Override
				public void checkForMentalLinkSwitch(Id curr, Id next, Agent2D agent) {
					// nothing to do here
				}
			};
		}
	}

	
	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		MobsimDriverAgent pda = this.defaultAgentFactory.createMobsimAgentFromPerson(p);
		PhysicalAgentRepresentation par = new PhysicalAgentRepresentation();
		Agent2D agent = new Agent2D(pda, this.sc, this.mlsw,par);
		return agent;
	}

}
