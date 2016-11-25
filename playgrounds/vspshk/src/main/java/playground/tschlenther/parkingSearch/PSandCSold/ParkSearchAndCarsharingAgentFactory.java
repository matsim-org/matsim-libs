package playground.tschlenther.parkingSearch.PSandCSold;
/*package playground.tschlenther.parkingSearch;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.CarSharingVehiclesNew;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class ParkSearchAndCarsharingAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private CarSharingVehiclesNew carSharingVehicles;

	private TripRouter tripRouter;
	private LeastCostPathCalculator pathCalculator;
	
	public ParkSearchAndCarsharingAgentFactory(final Netsim simulation, final Scenario scenario, final Provider<TripRouter> tripRouterProvider, 
			CarSharingVehiclesNew carSharingVehicles, LeastCostPathCalculator pathCalculator) {
		this.simulation = simulation;
		this.carSharingVehicles = carSharingVehicles;
		this.pathCalculator = pathCalculator;
				
		Provider<TripRouter> tripRouterFactory = tripRouterProvider;
		
		tripRouter = tripRouterFactory.get();		
		
	}

	@Override 
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		
		
		//if we want to simulate all agents then we have a normal AllCSModesPersonDriverAgentImpl agent 
		//if we want to simulate only carsharing members we would have PersonDriverAgentOnlyMembersImpl agent here

		MobsimDriverAgent agent ;
//		agent = new CarsharingPersonDriverAgentImplOLD(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()),
//				this.simulation, this.scenario, this.carSharingVehicles, this.tripRouter); 
		
		
		// randomly search until destination link is passed for the 5th time
//		agent = new ParkSearchAndCarsharingPersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation,
//				this.carSharingVehicles, this.tripRouter, pathCalculator);
		
		// considering availibility (randomly drwed number) and inserting walking leg if destinationLink of car_trip is not destination link of trip
		agent = new PSAndCSPersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation,
				this.carSharingVehicles, this.tripRouter, pathCalculator);
		
		return agent;
	}
}
*/