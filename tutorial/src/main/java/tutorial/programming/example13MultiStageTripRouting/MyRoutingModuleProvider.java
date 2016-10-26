package tutorial.programming.example13MultiStageTripRouting;

import com.google.inject.Provider;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;

public class MyRoutingModuleProvider implements Provider<RoutingModule> {

	private Provider<RoutingModule> tripRouterProvider;
	private PopulationFactory populationFactory;
	private ActivityFacility teleport;

	public MyRoutingModuleProvider(Provider<RoutingModule> tripRouterProvider, PopulationFactory populationFactory, ActivityFacility teleport) {
		this.tripRouterProvider = tripRouterProvider;
		this.populationFactory = populationFactory;
		this.teleport = teleport;
	}

	@Override
	public RoutingModule get() {
		return new MyRoutingModule(tripRouterProvider, populationFactory, teleport);
	}
}
