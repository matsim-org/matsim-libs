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
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;

public class TripRouterModule extends AbstractModule {

    @Override
    public void install() {
        install(new TripRouterFactoryModule());
        bind(TripRouter.class).toProvider(RealTripRouterProvider.class);
    }

    private static class RealTripRouterProvider implements Provider<TripRouter> {

        final TripRouterFactory tripRouterFactory;
        final Provider<TravelDisutility> travelDisutility;
        final Provider<TravelTime> travelTime;

        @Inject
        RealTripRouterProvider(TripRouterFactory tripRouterFactory, Provider<TravelDisutility> travelDisutility, Provider<TravelTime> travelTime) {
            this.travelDisutility = travelDisutility;
            this.tripRouterFactory = tripRouterFactory;
            this.travelTime = travelTime;
        }

        @Override
        public TripRouter get() {
            return tripRouterFactory.instantiateAndConfigureTripRouter(new RoutingContext() {

                @Override
                public TravelDisutility getTravelDisutility() {
                    return travelDisutility.get();
                }

                @Override
                public TravelTime getTravelTime() {
                    return travelTime.get();
                }

            });
        }

    }

}
