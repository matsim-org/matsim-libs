package playground.sergioo.passivePlanning2012.core.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class PRTripRouterFactory implements Provider<TripRouter> {

    private static Logger log = Logger.getLogger(PRTripRouterFactory.class);

    private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
    private final Provider<TransitRouter> transitRouterFactory;
    private final Scenario scenario;

    @Inject
    Map<String, TravelTime> travelTimes;

    @Inject
    Map<String, TravelDisutilityFactory> travelDisutilityFactories;

    @Inject
    public PRTripRouterFactory(Scenario scenario, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, com.google.inject.Provider<TransitRouter> transitRouterFactory) {
    	this.scenario = scenario;
    	this.transitRouterFactory = transitRouterFactory;
    	this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
    }
    
    @Override
    public TripRouter get() {
    	TripRouter tripRouter = new TripRouter();

        PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

        TravelTime travelTime = travelTimes.get("car");
        TravelDisutility travelDisutility = travelDisutilityFactories.get("car").createTravelDisutility(travelTimes.get("car"));

        LeastCostPathCalculator routeAlgo =
                leastCostPathCalculatorFactory.createPathCalculator(
                        scenario.getNetwork(),
                        travelDisutility,
                        travelTime);

        FreespeedTravelTimeAndDisutility ptTimeCostCalc =
                new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
        LeastCostPathCalculator routeAlgoPtFreeFlow =
                leastCostPathCalculatorFactory.createPathCalculator(
                        scenario.getNetwork(),
                        ptTimeCostCalc,
                        ptTimeCostCalc);

        final boolean networkIsMultimodal = NetworkUtils.isMultimodal(scenario.getNetwork());

        for (String mode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
            final RoutingModule routingModule = DefaultRoutingModules.createPseudoTransitRouter(mode, scenario.getPopulation().getFactory(), 
			        scenario.getNetwork(), routeAlgoPtFreeFlow, routeConfigGroup.getModeRoutingParams().get( mode ) );
			tripRouter.setRoutingModule( mode, routingModule ) ;
        }

        for (String mode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
            final RoutingModule routingModule = DefaultRoutingModules.createTeleportationRouter( mode, scenario.getPopulation().getFactory(), 
			        routeConfigGroup.getModeRoutingParams().get( mode ) );
			final RoutingModule result = tripRouter.setRoutingModule( mode, routingModule) ;

			if ( result != null ) {
                log.error( "inconsistent router configuration for mode "+mode );
                log.error( "One situation which triggers this warning: setting both speed and speedFactor for a mode (this used to be possible)." );
                throw new RuntimeException( "there was already a module set when trying to set teleporting module for mode "+mode+
                        ": "+result );
            }
        }

        for ( String mode : routeConfigGroup.getNetworkModes() ) {
            final RoutingModule routingModule = DefaultRoutingModules.createPureNetworkRouter(mode, scenario.getPopulation().getFactory(), 
			        scenario.getNetwork(), routeAlgo);
			final RoutingModule result = tripRouter.setRoutingModule( mode, routingModule);

			if ( result != null ) {
                log.error( "inconsistent router configuration for mode "+mode );
                throw new RuntimeException( "there was already a module set when trying to set network routing module for mode "+mode+
                        ": "+result );
            }
            
            // The default router will always route on the car network.  A user may, however, have prepared a network with dedicated bicycle
            // links and then expect the router to route on that.  The following test tries to catch that.  If someone improves on this,
            // the test can be removed.  kai, feb'15
            if ( networkIsMultimodal ) {
            	switch ( mode ) {
            	case TransportMode.car :
            	case TransportMode.ride :
            		break ;
            	default:
            		throw new RuntimeException("you have a multi-modal network and configured " + mode + " to be routed as a network mode.  "
            				+ "The present configuration will route this "
            				+ "mode on the car network.  This may be ok (e.g. with ``truck'' or ``motorbike''), or not (e.g. with ``bicycle''). "
            				+ "Throwing an exception anyways; please use a uni-modal network if you want to keep this configuration.") ;
            	}
            }
        }

        tripRouter.setRoutingModule("empty", new DummyRoutingModule());
        
        if ( scenario.getConfig().transit().isUseTransit() ) {
            TransitRouterWrapper routingModule = new TransitRouterWrapper(
                    transitRouterFactory.get(),
                    scenario.getTransitSchedule(),
                    scenario.getNetwork(), // use a walk router in case no PT path is found
                    DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, scenario.getPopulation().getFactory(),
					        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ) )) ;
            for (String mode : scenario.getConfig().transit().getTransitModes()) {
                // XXX one can't check for inconsistent setting here...
                // because the setting is inconsistent by default (defaults
                // set a teleportation setting for pt routing, which is overriden
                // here) (td, may 2013)
                tripRouter.setRoutingModule(mode, routingModule);
            }
        }
        return tripRouter;
    }

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return leastCostPathCalculatorFactory;
	}

}
