package playground.pieter.distributed.plans.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PseudoTransitLegRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.pt.router.TransitRouterFactory;
import playground.pieter.distributed.plans.PopulationFactoryForPlanGenomes;

import javax.inject.Inject;
import java.util.Collections;

public class DefaultTripRouterFactoryForPlanGenomes implements TripRouterFactory {

    private static Logger log = Logger.getLogger(DefaultTripRouterFactoryForPlanGenomes.class);

    public static TripRouterFactory createRichTripRouterFactoryImpl(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                new TripRouterFactoryModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        bindToInstance(Scenario.class, scenario);
                    }
                })
                .getInstance(TripRouterFactory.class);
	}

	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
    private final TransitRouterFactory transitRouterFactory;
    private final Scenario scenario;

    @Inject
    public DefaultTripRouterFactoryForPlanGenomes(Scenario scenario, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, TransitRouterFactory transitRouterFactory) {
    	this.scenario = scenario;
    	this.transitRouterFactory = transitRouterFactory;
    	this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
    }
    
    @Override
    public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {

        TripRouter tripRouter = new TripRouter();

        PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

        LeastCostPathCalculator routeAlgo =
                leastCostPathCalculatorFactory.createPathCalculator(
                        scenario.getNetwork(),
                        routingContext.getTravelDisutility(),
                        routingContext.getTravelTime());

        FreespeedTravelTimeAndDisutility ptTimeCostCalc =
                new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
        LeastCostPathCalculator routeAlgoPtFreeFlow =
                leastCostPathCalculatorFactory.createPathCalculator(
                        scenario.getNetwork(),
                        ptTimeCostCalc,
                        ptTimeCostCalc);

        if ( NetworkUtils.isMultimodal(scenario.getNetwork()) ) {
            // note: LinkImpl has a default allowed mode of "car" so that all links
            // of a monomodal network are actually restricted to car, making the check
            // of multimodality unecessary from a behavioral point of view.
            // However, checking the mode restriction for each link is expensive,
            // so it is not worth doing it if it is not necessary. (td, oct. 2012)
            if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
                ((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(
                        Collections.singleton(TransportMode.car));
                ((IntermodalLeastCostPathCalculator) routeAlgoPtFreeFlow).setModeRestriction(
                        Collections.singleton( TransportMode.car ));
            }
            else {
                // this is impossible to reach when using the algorithms of org.matsim.*
                // (all implement IntermodalLeastCostPathCalculator)
                log.warn( "network is multimodal but least cost path algorithm is not an instance of IntermodalLeastCostPathCalculator!" );
            }
        }

        for (String mainMode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
            tripRouter.setRoutingModule(
                    mainMode,
                    new LegRouterWrapper(
                            mainMode,
                            scenario.getPopulation().getFactory(),
                            new PseudoTransitLegRouter(
                                    scenario.getNetwork(),
                                    routeAlgoPtFreeFlow,
                                    routeConfigGroup.getTeleportedModeFreespeedFactors().get( mainMode ),
                                    routeConfigGroup.getModeRoutingParams().get( mainMode ).getBeelineDistanceFactor(),
                                    ((PopulationFactoryForPlanGenomes) scenario.getPopulation().getFactory()).getModeRouteFactory())));
        }

        for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
            final RoutingModule old =
                    tripRouter.setRoutingModule(
                            mainMode,
                            new LegRouterWrapper(
                                    mainMode,
                                    scenario.getPopulation().getFactory(),
                                    new TeleportationLegRouter(
                                            ((PopulationFactoryForPlanGenomes) scenario.getPopulation().getFactory()).getModeRouteFactory(),
                                            routeConfigGroup.getTeleportedModeSpeeds().get( mainMode ),
                                            routeConfigGroup.getModeRoutingParams().get( mainMode ).getBeelineDistanceFactor())));
            if ( old != null ) {
                log.error( "inconsistent router configuration for mode "+mainMode );
                log.error( "One situation which triggers this warning: setting both speed and speedFactor for a mode (this used to be possible)." );
                throw new RuntimeException( "there was already a module set when trying to set teleporting module for mode "+mainMode+
                        ": "+old );
            }
        }

        for ( String mainMode : routeConfigGroup.getNetworkModes() ) {
            final RoutingModule old =
                    tripRouter.setRoutingModule(
                            mainMode,
                            new LegRouterWrapper(
                                    mainMode,
                                    scenario.getPopulation().getFactory(),
                                    new NetworkLegRouter(
                                            scenario.getNetwork(),
                                            routeAlgo,
                                            ((PopulationFactoryForPlanGenomes) scenario.getPopulation().getFactory()).getModeRouteFactory())));
            if ( old != null ) {
                log.error( "inconsistent router configuration for mode "+mainMode );
                throw new RuntimeException( "there was already a module set when trying to set network routing module for mode "+mainMode+
                        ": "+old );
            }
        }

        if ( scenario.getConfig().scenario().isUseTransit() ) {
            TransitRouterWrapper routingModule = new TransitRouterWrapper(
                    transitRouterFactory.createTransitRouter(),
                    scenario.getTransitSchedule(),
                    scenario.getNetwork(), // use a walk router in case no PT path is found
                    new LegRouterWrapper(
                            TransportMode.transit_walk,
                            scenario.getPopulation().getFactory(),
                            new TeleportationLegRouter(
                                    ((PopulationFactoryForPlanGenomes) scenario.getPopulation().getFactory()).getModeRouteFactory(),
                                    routeConfigGroup.getTeleportedModeSpeeds().get( TransportMode.walk ),
                                    routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor())));
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

	public TransitRouterFactory getTransitRouterFactory() {
		return transitRouterFactory;
	}
}
