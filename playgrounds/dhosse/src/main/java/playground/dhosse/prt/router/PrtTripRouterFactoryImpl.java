package playground.dhosse.prt.router;

import java.util.Collections;

import javax.inject.Provider;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.*;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.passenger.PrtRequestCreator;

public class PrtTripRouterFactoryImpl implements Provider<TripRouter> {
	
    private VrpData vrpData;
    private Scenario scenario;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final PrtData data;
	
	public PrtTripRouterFactoryImpl(VrpData vrpData, Scenario scenario, final TravelTime ttime, final TravelDisutility tdis, PrtData data){

        this.vrpData = vrpData;
        this.scenario = scenario;
		this.travelTime = ttime;
		this.travelDisutility = tdis;
		this.data = data;
		
	}

	@Override
	public TripRouter get() {
		
		Network network = this.scenario.getNetwork();
		LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory = createDefaultLeastCostPathCalculatorFactory(this.scenario);
		PopulationFactory populationFactory = this.scenario.getPopulation().getFactory();
		RouteFactoryImpl modeRouteFactory = ((PopulationFactoryImpl)populationFactory).getRouteFactory();
		
		TripRouter tripRouter = new TripRouter();

		PlansCalcRouteConfigGroup routeConfigGroup = this.scenario.getConfig().plansCalcRoute();

		System.out.println(routeConfigGroup.getModeRoutingParams().get(TransportMode.walk));
		
		LeastCostPathCalculator routeAlgo =
				leastCostPathAlgorithmFactory.createPathCalculator(
	                    network,
						travelDisutility,
						travelTime);
		
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
				if (routeAlgo instanceof Dijkstra) {
					((Dijkstra) routeAlgo).setModeRestriction(
						Collections.singleton( TransportMode.car ));
					((Dijkstra) routeAlgoPtFreeFlow).setModeRestriction(
						Collections.singleton( TransportMode.car ));
				}
				else {
					// this is impossible to reach when using the algorithms of org.matsim.*
					// (all implement IntermodalLeastCostPathCalculator)
					throw new RuntimeException();
				}
			}
			
		for(String mainMode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()){
			
			tripRouter.setRoutingModule( mainMode, 
					DefaultRoutingModules.createPseudoTransitRouter(mainMode, populationFactory, 
					network, routeAlgoPtFreeFlow, routeConfigGroup.getModeRoutingParams().get(mainMode) ) ) ;
			
		}
		
		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {

			RoutingModule module = DefaultRoutingModules.createTeleportationRouter(mainMode, populationFactory, routeConfigGroup.getModeRoutingParams().get(mainMode) ) ;
			tripRouter.setRoutingModule( mainMode, module) ;
			
		}

		for ( String mainMode : routeConfigGroup.getNetworkModes() ) {

			RoutingModule module = DefaultRoutingModules.createPureNetworkRouter(mainMode, populationFactory, network, routeAlgoPtFreeFlow) ;
			tripRouter.setRoutingModule(mainMode, module) ;
			
		}
		
		tripRouter.setRoutingModule(
				PrtRequestCreator.MODE, new PrtRouterWrapper(PrtRequestCreator.MODE, network,
				populationFactory, this.data,
				DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, populationFactory, routeConfigGroup.getModeRoutingParams().get(TransportMode.walk) ) ) 
			);
		
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
