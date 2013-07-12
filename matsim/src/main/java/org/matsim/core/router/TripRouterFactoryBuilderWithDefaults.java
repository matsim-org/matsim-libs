package org.matsim.core.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImplFactory;

public class TripRouterFactoryBuilderWithDefaults {

	private TransitRouterFactory transitRouterFactory;
	
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public void setTransitRouterFactory(TransitRouterFactory transitRouterFactory) {
		this.transitRouterFactory = transitRouterFactory;
	}

	public void setLeastCostPathCalculatorFactory(LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}
	
	public DefaultTripRouterFactoryImpl build(Scenario scenario) {
		Config config = scenario.getConfig();
		
		if (leastCostPathCalculatorFactory == null) {
			leastCostPathCalculatorFactory = createDefaultLeastCostPathCalculatorFactory(scenario);
		}

		if (transitRouterFactory == null && config.scenario().isUseTransit()) {
            transitRouterFactory = createDefaultTransitRouter(scenario);
        }
		
		return new DefaultTripRouterFactoryImpl(scenario, leastCostPathCalculatorFactory, transitRouterFactory);
	}

	public TransitRouterImplFactory createDefaultTransitRouter(Scenario scenario) {
		Config config = scenario.getConfig();
		return new TransitRouterImplFactory(
		        scenario.getTransitSchedule(),
		        new TransitRouterConfig(
		                config.planCalcScore(),
		                config.plansCalcRoute(),
		                config.transitRouter(),
		                config.vspExperimental()));
	}

	public LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(Scenario scenario) {
		Config config = scenario.getConfig();
		if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
            return new DijkstraFactory();
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
            return new AStarLandmarksFactory(
                    scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()), config.global().getNumberOfThreads());
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
            return new FastDijkstraFactory();
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
            return new FastAStarLandmarksFactory(
                    scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
        } else {
            throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
        }
	}

}
