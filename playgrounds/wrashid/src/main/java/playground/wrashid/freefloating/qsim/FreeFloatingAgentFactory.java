package playground.wrashid.freefloating.qsim;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class FreeFloatingAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private ParkingModuleWithFreeFloatingCarSharing parkingModule;
	private TripRouter tripRouter;
	private LeastCostPathCalculator pathCalculator;
	public FreeFloatingAgentFactory(final Netsim simulation, final Scenario scenario, 
			ParkingModuleWithFreeFloatingCarSharing parkingModule, final Provider<TripRouter> tripRouterProvider, 
			LeastCostPathCalculator pathCalculator) {
		this.simulation = simulation;
		this.parkingModule = parkingModule;
		this.pathCalculator = pathCalculator;
		Provider<TripRouter> tripRouterFactory = tripRouterProvider;
		
		tripRouter = tripRouterFactory.get();	

	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		
		FreeFloatingParkingPersonDriverAgentImplNew agent = new FreeFloatingParkingPersonDriverAgentImplNew(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation,
				this.parkingModule,
				 this.tripRouter, this.pathCalculator); 
		return agent;
	}
}
