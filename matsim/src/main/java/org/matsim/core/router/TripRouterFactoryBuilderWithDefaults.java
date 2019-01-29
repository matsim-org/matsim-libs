/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterModule;

import javax.inject.Provider;
import java.util.Arrays;

public class TripRouterFactoryBuilderWithDefaults {

	private Provider<TransitRouter> transitRouterFactory;
	
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
    private TravelTime carTravelTime;
    private TravelDisutility carTravelDisutility;

    public static Provider<TripRouter> createTripRouterProvider(
            final Scenario scenario,
            final LeastCostPathCalculatorFactory leastCostAlgoFactory,
            final Provider<TransitRouter> transitRouterFactory) {
        TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
        builder.setLeastCostPathCalculatorFactory(leastCostAlgoFactory);
        builder.setTransitRouterFactory(transitRouterFactory);
        return builder.build(scenario);
	}

    public void setTransitRouterFactory(Provider<TransitRouter> transitRouterFactory) {
		this.transitRouterFactory = transitRouterFactory;
	}

	public void setLeastCostPathCalculatorFactory(LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}
	
	public Provider<TripRouter> build(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                AbstractModule.override(
                        Arrays.asList(
                                new ScenarioByInstanceModule(scenario),
                                new TripRouterModule(),
                                new TravelDisutilityModule(),
                                new TravelTimeCalculatorModule(),
                                new EventsManagerModule()),
                        new AbstractModule() {
							@Override
                            public void install() {
                                if (leastCostPathCalculatorFactory != null) {
                                    bind(LeastCostPathCalculatorFactory.class).toInstance(leastCostPathCalculatorFactory);
                                }
                                if (transitRouterFactory != null && getConfig().transit().isUseTransit()) {
                                    bind(TransitRouter.class).toProvider(transitRouterFactory);
                                }
                                if (carTravelDisutility != null) {
                                    addTravelDisutilityFactoryBinding("car").toInstance(new TravelDisutilityFactory() {
                                        @Override
                                        public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
                                            return carTravelDisutility;
                                        }
                                    });
                                }
                                if (carTravelTime != null) {
                                    addTravelTimeBinding("car").toInstance(carTravelTime);
                                }
                            }
                        })).getProvider(TripRouter.class);
    }

    public static Provider<TripRouter> createDefaultTripRouterFactoryImpl(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                new TripRouterModule(),
                new TravelDisutilityModule(),
                new TravelTimeCalculatorModule(),
                new EventsManagerModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        install(new ScenarioByInstanceModule(scenario));
                    }
                })
                .getProvider(TripRouter.class);
    }

    public static Provider<TransitRouter> createDefaultTransitRouter(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                new TransitRouterModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        install(new ScenarioByInstanceModule(scenario));
                    }
                })
        .getProvider(TransitRouter.class);
	}

	public static LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                new ScenarioByInstanceModule(scenario),
                new EventsManagerModule(),
                new TravelDisutilityModule(),
                new TravelTimeCalculatorModule(),
                new LeastCostPathCalculatorModule())
        .getInstance(LeastCostPathCalculatorFactory.class);
    }

    public void setTravelTime(TravelTime travelTime) {
        this.carTravelTime = travelTime;
    }

    public void setTravelDisutility(TravelDisutility travelDisutility) {
        this.carTravelDisutility = travelDisutility;
    }

}
