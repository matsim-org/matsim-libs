/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TransitRouterModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.pt.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import javax.inject.Inject;
import javax.inject.Provider;

public class TransitRouterModule extends AbstractModule {

    @Override
    public void install() {
        if (getConfig().scenario().isUseTransit()) {
            bindToProviderAsSingleton(TransitRouterFactory.class, TransitRouterFactoryProvider.class);
        } else {
            bindTo(TransitRouterFactory.class, DummyTransitRouterFactory.class);
        }
    }

    static class TransitRouterFactoryProvider implements Provider<TransitRouterFactory> {

        @Inject
        Scenario scenario;

        @Override
        public TransitRouterFactory get() {
            Config config = scenario.getConfig();
            return new TransitRouterImplFactory(
                    scenario.getTransitSchedule(),
                    new TransitRouterConfig(
                            config.planCalcScore(),
                            config.plansCalcRoute(),
                            config.transitRouter(),
                            config.vspExperimental()));
        }

    }

    static class DummyTransitRouterFactory implements TransitRouterFactory {
        @Override
        public TransitRouter createTransitRouter() {
            throw new RuntimeException("Transit not enabled.");
        }
    }

}
