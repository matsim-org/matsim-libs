package org.matsim.contrib.carsharing.runExample;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.router.FreeFloatingRoutingModule;
import org.matsim.contrib.carsharing.router.OneWayCarsharingRoutingModule;
import org.matsim.contrib.carsharing.router.TwoWayCarsharingRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;

public class CarsharingUtils {
	public static Config addConfigModules(Config config) {
		
		OneWayCarsharingConfigGroup configGroup = new OneWayCarsharingConfigGroup();
    	config.addModule(configGroup);
    	
    	FreeFloatingConfigGroup configGroupff = new FreeFloatingConfigGroup();
    	config.addModule(configGroupff);
    	
    	TwoWayCarsharingConfigGroup configGrouptw = new TwoWayCarsharingConfigGroup();
    	config.addModule(configGrouptw);
    	
    	CarsharingConfigGroup configGroupAll = new CarsharingConfigGroup();
    	config.addModule(configGroupAll);
    	
    	return config;
		
	}
	public static TripRouterFactory createTripRouterFactory(final Scenario scenario) {
		
		return new TripRouterFactory() {
            @Override
            public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
                // this factory initializes a TripRouter with default modules,
                // taking into account what is asked for in the config

                // This allows us to just add our module and go.
                final TripRouterFactory delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario);

                final TripRouter router = delegate.instantiateAndConfigureTripRouter(routingContext);

                // add our module to the instance
                router.setRoutingModule(
                    "twowaycarsharing",
                    new TwoWayCarsharingRoutingModule());

                router.setRoutingModule(
                        "freefloating",
                        new FreeFloatingRoutingModule());

                router.setRoutingModule(
                        "onewaycarsharing",
                        new OneWayCarsharingRoutingModule());

                // we still need to provide a way to identify our trips
                // as being twowaycarsharing trips.
                // This is for instance used at re-routing.
                final MainModeIdentifier defaultModeIdentifier =
                    router.getMainModeIdentifier();
                router.setMainModeIdentifier(
                        new MainModeIdentifier() {
                            @Override
                            public String identifyMainMode(
                                    final List<PlanElement> tripElements) {
                                for ( PlanElement pe : tripElements ) {
                                    if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twowaycarsharing" ) ) {
                                        return "twowaycarsharing";
                                    }
                                    else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
                                        return "onewaycarsharing";
                                    }
                                    else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
                                        return "freefloating";
                                    }
                                }
                                // if the trip doesn't contain a carsharing leg,
                                // fall back to the default identification method.
                                return defaultModeIdentifier.identifyMainMode( tripElements );
                            }
                        });

                return router;
            }

		
		};

	}
}
