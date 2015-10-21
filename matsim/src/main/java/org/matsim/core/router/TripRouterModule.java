/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TripRouterModule.java
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

package org.matsim.core.router;

import org.matsim.core.controler.AbstractModule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class TripRouterModule extends AbstractModule {

    @Override
    public void install() {
        install(new TripRouterFactoryModule());
        bind(MainModeIdentifier.class).to(MainModeIdentifierImpl.class);
        bind(TripRouter.class).toProvider(RealTripRouterProvider.class);
    }

    private static class RealTripRouterProvider implements Provider<TripRouter> {

        final Map<String, Provider<RoutingModule>> routingModules;
        private MainModeIdentifier mainModeIdentifier;

        @Inject
        RealTripRouterProvider(Map<String, Provider<RoutingModule>> routingModules, MainModeIdentifier mainModeIdentifier) {
            this.routingModules = routingModules;
            this.mainModeIdentifier = mainModeIdentifier;
        }

        @Override
        public TripRouter get() {
            TripRouter tripRouter = new TripRouter();
            for (Map.Entry<String, Provider<RoutingModule>> entry : routingModules.entrySet()) {
                tripRouter.setRoutingModule(entry.getKey(), entry.getValue().get());
            }
            tripRouter.setMainModeIdentifier(mainModeIdentifier);
            return tripRouter;
        }

    }

}
