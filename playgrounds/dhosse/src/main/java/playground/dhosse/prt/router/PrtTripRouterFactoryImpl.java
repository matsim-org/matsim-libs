package playground.dhosse.prt.router;

import java.util.Collections;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculatorImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PseudoTransitLegRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.dhosse.prt.passenger.PrtRequestCreator;

public class PrtTripRouterFactoryImpl implements TripRouterFactory {
	
	private final MatsimVrpContext context;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	
	public PrtTripRouterFactoryImpl(final MatsimVrpContext context, final TravelTime ttime, final TravelDisutility tdis){

		this.context = context;
		this.travelTime = ttime;
		this.travelDisutility = tdis;
		
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
		
		Network network = this.context.getScenario().getNetwork();
		LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory = createDefaultLeastCostPathCalculatorFactory(this.context.getScenario());
		PopulationFactory populationFactory = this.context.getScenario().getPopulation().getFactory();
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl)populationFactory).getModeRouteFactory();
		
		TripRouter tripRouter = new TripRouter();

		PlansCalcRouteConfigGroup routeConfigGroup = this.context.getScenario().getConfig().plansCalcRoute();
		
		LeastCostPathCalculator routeAlgo =
				leastCostPathAlgorithmFactory.createPathCalculator(
	                    network,
	                    routingContext.getTravelDisutility(),
	                    routingContext.getTravelTime());
		
		FreespeedTravelTimeAndDisutility ptTimeCostCalc =
				new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
			LeastCostPathCalculator routeAlgoPtFreeFlow =
				leastCostPathAlgorithmFactory.createPathCalculator(
	                    network,
	                    ptTimeCostCalc,
	                    ptTimeCostCalc);
			
			if ( NetworkUtils.isMultimodal(network) ) {
				// note: LinkImpl has a default allowed mode of "car" so that all links
				// of a monomodal network are actually restricted to car, making the check
				// of multimodality unecessary from a behavioral point of view.
				// However, checking the mode restriction for each link is expensive,
				// so it is not worth doing it if it is not necessary. (td, oct. 2012)
				if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
					((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(
						Collections.singleton( TransportMode.car ));
					((IntermodalLeastCostPathCalculator) routeAlgoPtFreeFlow).setModeRestriction(
						Collections.singleton( TransportMode.car ));
				}
				else {
					// this is impossible to reach when using the algorithms of org.matsim.*
					// (all implement IntermodalLeastCostPathCalculator)
				}
			}
		
		for(String mainMode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()){
			
			tripRouter.setRoutingModule(mainMode,
					new LegRouterWrapper(mainMode, populationFactory,
							new PseudoTransitLegRouter(
									network, routeAlgoPtFreeFlow,
									routeConfigGroup.getTeleportedModeFreespeedFactors().get(mainMode),
	                                routeConfigGroup.getModeRoutingParams().get( mainMode ).getBeelineDistanceFactor(),
//									routeConfigGroup.getBeelineDistanceFactor(),
									modeRouteFactory)));
			
		}
		
		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
                            populationFactory,
						new TeleportationLegRouter(
                                modeRouteFactory,
							routeConfigGroup.getTeleportedModeSpeeds().get( mainMode ),
                            routeConfigGroup.getModeRoutingParams().get( mainMode ).getBeelineDistanceFactor() ))) ;
//							routeConfigGroup.getBeelineDistanceFactor())));
		}

		for ( String mainMode : routeConfigGroup.getNetworkModes() ) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
                            populationFactory,
						new NetworkLegRouter(
                                network,
							routeAlgo,
                                modeRouteFactory)));
		}
		
		Network prtNetwork = NetworkUtils.createNetwork();
		for(Link link : network.getLinks().values()){
			if(link.getAllowedModes().contains(TransportMode.car)){
				Node fromNode = link.getFromNode();
				Node toNode = link.getToNode();
				if(!prtNetwork.getNodes().containsKey(fromNode.getId())){
					((NetworkImpl)prtNetwork).createAndAddNode(fromNode.getId(), fromNode.getCoord());
				}
				if(!prtNetwork.getNodes().containsKey(toNode.getId())){
					((NetworkImpl)prtNetwork).createAndAddNode(toNode.getId(), toNode.getCoord());
				}
				prtNetwork.addLink(link);
			}
		}
		
		tripRouter.setRoutingModule(PrtRequestCreator.MODE, new PrtRouterWrapper(PrtRequestCreator.MODE, network,
				populationFactory, new VrpPathCalculatorImpl(routeAlgo, this.travelTime, this.travelDisutility),
				new LegRouterWrapper(TransportMode.transit_walk, populationFactory,
						new NetworkLegRouter(network, routeAlgoPtFreeFlow, modeRouteFactory))));
		
		return tripRouter;
		
	}

	private LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(
			Scenario scenario) {
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
