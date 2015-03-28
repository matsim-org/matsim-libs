package playground.balac.allcsmodestest.qsim;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;

import playground.balac.freefloating.qsim.FreeFloatingVehiclesLocation;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingVehicleLocation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;

public class AllCSModesAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private final Scenario scenario;
	private final Controler controler;
	private FreeFloatingVehiclesLocation ffvehiclesLocation;
	private OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocation;
	private TwoWayCSVehicleLocation twvehiclesLocation;
	private TripRouter tripRouter;
	public AllCSModesAgentFactory(final Netsim simulation, final Scenario scenario, final Controler controler, FreeFloatingVehiclesLocation ffvehiclesLocation, OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocation, TwoWayCSVehicleLocation twvehiclesLocation) {
		this.simulation = simulation;
		this.scenario = scenario;
		this.controler = controler;
		this.ffvehiclesLocation = ffvehiclesLocation;
		this.owvehiclesLocation = owvehiclesLocation;
		this.twvehiclesLocation = twvehiclesLocation;
		
		Provider<TripRouter> tripRouterFactory = controler.getTripRouterProvider();
		
		tripRouter = tripRouterFactory.get();
		
		
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		
		
		//if we want to simulate all agents then we have a normal AllCSModesPersonDriverAgentImpl agent 
		//if we want to simulate only carsharing members we would have PersonDriverAgentOnlyMembersImpl agent here
		
		PersonDriverAgentOnlyMembersImpl agent = new PersonDriverAgentOnlyMembersImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation, this.scenario, this.controler, this.ffvehiclesLocation, this.owvehiclesLocation, this.twvehiclesLocation, this.tripRouter); 
		return agent;
	}
}
