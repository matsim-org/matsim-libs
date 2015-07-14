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
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioElementsModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Provider;

public class TripRouterFactoryBuilderWithDefaults {

	private Provider<TransitRouter> transitRouterFactory;
	
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public void setTransitRouterFactory(Provider<TransitRouter> transitRouterFactory) {
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

		if (transitRouterFactory == null && config.transit().isUseTransit()) {
            transitRouterFactory = createDefaultTransitRouter(scenario);
        }
		
		return new DefaultTripRouterFactoryImpl(scenario, leastCostPathCalculatorFactory, transitRouterFactory);
	}

	public static Provider<TransitRouter> createDefaultTransitRouter(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                new TransitRouterModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        bind(TransitSchedule.class).toInstance(scenario.getTransitSchedule());
                        bind(Scenario.class).toInstance(scenario);
                    }
                })
        .getProvider(TransitRouter.class);
	}

	public static LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(final Scenario scenario) {
        return Injector.createInjector(scenario.getConfig(),
                new ScenarioElementsModule(),
                new TravelDisutilityModule(),
                new TravelTimeCalculatorModule(),
                new LeastCostPathCalculatorModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        bind(Scenario.class).toInstance(scenario);
                    }
                })
        .getInstance(LeastCostPathCalculatorFactory.class);
    }

}
