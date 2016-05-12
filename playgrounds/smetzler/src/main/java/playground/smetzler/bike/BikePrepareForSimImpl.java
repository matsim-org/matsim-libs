package playground.smetzler.bike;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.XY2Links;

import javax.inject.Inject;
import javax.inject.Provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class BikePrepareForSimImpl implements PrepareForSim {

	private static Logger log = Logger.getLogger(PrepareForSim.class);

	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;

	@Inject
	BikePrepareForSimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network, Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider) {
		this.globalConfigGroup = globalConfigGroup;
		this.scenario = scenario;
		this.network = network;
		this.population = population;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
	}


	@Override
	public void run() {
		if (scenario instanceof MutableScenario) {
			((MutableScenario)scenario).setLocked();
			// see comment in ScenarioImpl. kai, sep'14
		}

		{
			Network carNetwork = NetworkUtils.createNetwork();
			Network bikeNetwork = NetworkUtils.createNetwork();
			
			Set<String> car = new HashSet<>();
			car.add("car");
			
			Set<String> bike = new HashSet<>();
			bike.add("bike");
			
			new TransportModeNetworkFilter(network).filter(carNetwork, car);
			new TransportModeNetworkFilter(network).filter(bikeNetwork, bike);
	
			XY2Links xy2LinksCar = new XY2Links(carNetwork, null);
			XY2Links xy2LinksBike = new XY2Links(bikeNetwork, null);
	

			for (Person person : population.getPersons().values()) {
				// TODO check if bike user or car user. DONE?!
//				String bla = person.getSelectedPlan().getPlanElements().get(1).toString();
				Leg firstLeg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
				String mode = firstLeg.getMode();
				// VERY important: This works *only* if *all* persons have only trips which are all with
				// the same mode (for the whole day)
//				System.out.println(bla);

				if (mode.equals("car")){
					xy2LinksCar.run(person);
				} else {
					xy2LinksBike.run(person);
				}
			}
		}
		
		PlanRouter planRouter = new PlanRouter(tripRouterProvider.get());

		for (Person person : population.getPersons().values()) {
			planRouter.run(person);
		}

		
		
		if (population instanceof PopulationImpl) {
			((PopulationImpl) population).setLocked();
			
		}

	}
}
