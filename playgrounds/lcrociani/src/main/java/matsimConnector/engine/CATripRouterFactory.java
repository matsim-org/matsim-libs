package matsimConnector.engine;

import java.util.ArrayList;

import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

import pedCA.output.Log;

public class CATripRouterFactory implements TripRouterFactory{
	private Scenario scenario;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public CATripRouterFactory(Scenario sc) {
		this.scenario = sc;
		initLeastCostPathCalculatorFactory();
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
		
		LeastCostPathCalculator routeAlgo = leastCostPathCalculatorFactory
				.createPathCalculator(scenario.getNetwork(),
						routingContext.getTravelDisutility(),
						routingContext.getTravelTime());

		TripRouter tr = new TripRouter();

		ArrayList <String> modes = new ArrayList<String>();
		modes.add(Constants.CA_LINK_MODE);
		modes.add(Constants.CAR_LINK_MODE);
		modes.add(Constants.WALK_LINK_MODE);
		
		for (String mode : modes)
			tr.setRoutingModule(
					mode,
					LegRouterWrapper.createLegRouterWrapper(mode, scenario.getPopulation()
							.getFactory(), new NetworkLegRouter(scenario
					.getNetwork(), routeAlgo,
					((PopulationFactoryImpl) scenario.getPopulation()
							.getFactory()).getModeRouteFactory())));
		return tr;
	}

	private void initLeastCostPathCalculatorFactory() {
		Config config = scenario.getConfig();
		if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
			Log.debug("CATripRouter: new DijkstraFactory()");
			leastCostPathCalculatorFactory = new DijkstraFactory();
		} else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)){
			Log.debug("CATripRouter: new AStarLandmarksFactory()");
			leastCostPathCalculatorFactory = new AStarLandmarksFactory(scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()), config.global().getNumberOfThreads());
		} else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
			Log.debug("CATripRouter: new FastDijkstraFactory()");
			leastCostPathCalculatorFactory = new FastDijkstraFactory();
		} else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
			Log.debug("CATripRouter: new FastAStarLandmarksFactory()");
			leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
		} else {
			throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
		}
	}
}
