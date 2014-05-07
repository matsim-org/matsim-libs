package playground.balac.onewaycarsharingredisgned.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;


public class OneWayCarsharingRDAgentFactory  implements AgentFactory{
	private final Netsim simulation;
	private final Scenario scenario;
	private final Controler controler;
	private OneWayCarsharingRDVehicleLocation ffvehiclesLocation;

	public OneWayCarsharingRDAgentFactory(final Netsim simulation, final Scenario scenario, final Controler controler, OneWayCarsharingRDVehicleLocation ffvehiclesLocation) {
		this.simulation = simulation;
		this.scenario = scenario;
		this.controler = controler;
		this.ffvehiclesLocation = ffvehiclesLocation;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		OneWayCSRDPersonDriverAgentImpl agent = new OneWayCSRDPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation, this.scenario, this.controler, this.ffvehiclesLocation); 
		return agent;
	}
}
