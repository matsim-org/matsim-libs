package playground.balac.twowaycarsharingredisigned.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;


public class TwoWayCSAgentFactory  implements AgentFactory{
	private final Netsim simulation;
	private final Scenario scenario;
	private final MatsimServices controler;
	private TwoWayCSVehicleLocation ffvehiclesLocation;

	public TwoWayCSAgentFactory(final Netsim simulation, final Scenario scenario, final MatsimServices controler, TwoWayCSVehicleLocation ffvehiclesLocation) {
		this.simulation = simulation;
		this.scenario = scenario;
		this.controler = controler;
		this.ffvehiclesLocation = ffvehiclesLocation;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		TwoWayCSPersonDriverAgentImpl agent = new TwoWayCSPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation, this.scenario, this.controler, this.ffvehiclesLocation); 
		return agent;
	}
}
