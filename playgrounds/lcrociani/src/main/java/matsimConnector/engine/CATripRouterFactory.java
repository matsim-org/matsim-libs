package matsimConnector.engine;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

public class CATripRouterFactory implements  Provider<RoutingModule>{
	private Scenario scenario;
	private Map<String, TravelTime> travelTimes;
	private Map<String, TravelDisutilityFactory> travelDisutilities;

	@Inject
	CATripRouterFactory(Scenario scenario, Map<String, TravelTime> travelTimes, Map<String, TravelDisutilityFactory> travelDisutilities) {
		this.scenario = scenario;
		this.travelTimes = travelTimes;
		this.travelDisutilities = travelDisutilities;
	}

	@Override
	public RoutingModule get() {
		return DefaultRoutingModules.createNetworkRouter(Constants.CAR_LINK_MODE, scenario.getPopulation()
				.getFactory(), scenario.getNetwork(), createRoutingAlgo());
	}

	private LeastCostPathCalculator createRoutingAlgo() {
		return new AStarLandmarksFactory(
				scenario.getNetwork(),
				new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore()),
				scenario.getConfig().global().getNumberOfThreads()).createPathCalculator(scenario.getNetwork(),
				travelDisutilities.get("car").createTravelDisutility(travelTimes.get("car"), scenario.getConfig().planCalcScore()),
				travelTimes.get("car"));
	}
}
