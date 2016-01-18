package playground.balac.freefloating.qsimParkingModule;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;

public class FreeFloatingAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private final Scenario scenario;
	private final MatsimServices controler;
	private ParkingModuleWithFreeFloatingCarSharing parkingModule;

	public FreeFloatingAgentFactory(final Netsim simulation, final Scenario scenario, final MatsimServices controler,
			ParkingModuleWithFreeFloatingCarSharing parkingModule) {
		this.simulation = simulation;
		this.scenario = scenario;
		this.controler = controler;
		this.parkingModule = parkingModule;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		
		FreeFloatingParkingPersonDriverAgentImpl agent = new FreeFloatingParkingPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation, this.scenario, this.controler, this.parkingModule); 
		return agent;
	}
}
