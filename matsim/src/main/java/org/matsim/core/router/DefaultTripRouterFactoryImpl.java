package org.matsim.core.router;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.pt.router.TransitRouter;

public class DefaultTripRouterFactoryImpl implements TripRouterFactory {

    private static Logger log = Logger.getLogger(DefaultTripRouterFactoryImpl.class);
    
    public static TripRouterFactory createRichTripRouterFactoryImpl(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                new TripRouterFactoryModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        bind(Scenario.class).toInstance(scenario);
                    }
                })
                .getInstance(TripRouterFactory.class);
	}

    // Use a cache for the single mode networks. Otherwise, a new network is created for each TripRouterFactory instance!
    private final Map<String, Network> singleModeNetworksCache = new ConcurrentHashMap<>();

	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
    private final Provider<TransitRouter> transitRouterFactory;
    private final Scenario scenario;

    @Inject
    public DefaultTripRouterFactoryImpl(Scenario scenario, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, Provider<TransitRouter> transitRouterFactory) {
    	this.scenario = scenario;
    	this.transitRouterFactory = transitRouterFactory;
    	this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
    }
    
    @Override
    public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {

        TripRouter tripRouter = new TripRouter();

        PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

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
        	
        	Network filteredNetwork = null;
        	
        	// Ensure this is not performed concurrently by multiple threads!
        	synchronized (this.singleModeNetworksCache) {
        		filteredNetwork = this.singleModeNetworksCache.get(mode);
        		if (filteredNetwork == null) {
        			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
        			Set<String> modes = new HashSet<>();
        			modes.add(mode);
        			filteredNetwork = NetworkUtils.createNetwork();
        			filter.filter(filteredNetwork, modes);
        			this.singleModeNetworksCache.put(mode, filteredNetwork);
        		}
			}

            LeastCostPathCalculator routeAlgo =
            leastCostPathCalculatorFactory.createPathCalculator(
                    filteredNetwork,
                    routingContext.getTravelDisutility(),
                    routingContext.getTravelTime());

            final RoutingModule routingModule = DefaultRoutingModules.createNetworkRouter(mode, scenario.getPopulation().getFactory(),
                    filteredNetwork, routeAlgo);
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

	public Provider<TransitRouter> getTransitRouterFactory() {
		return transitRouterFactory;
	}
}
