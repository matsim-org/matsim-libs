package playground.balac.freefloating.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.misc.PopulationUtils;

public class FreeFloatingAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private final Scenario scenario;
	private final Controler controler;
	private FreeFloatingVehiclesLocation ffvehiclesLocation;

	public FreeFloatingAgentFactory(final Netsim simulation, final Scenario scenario, final Controler controler, FreeFloatingVehiclesLocation ffvehiclesLocation) {
		this.simulation = simulation;
		this.scenario = scenario;
		this.controler = controler;
		this.ffvehiclesLocation = ffvehiclesLocation;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		FreeFloatingPersonDriverAgentImpl agent = new FreeFloatingPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation, this.scenario, this.controler, this.ffvehiclesLocation); 
		return agent;
	}
}
