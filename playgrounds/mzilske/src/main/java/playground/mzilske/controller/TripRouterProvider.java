/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TripRouterProvider.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package playground.mzilske.controller;

import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;

class TripRouterProvider implements Provider<TripRouter> {

    @Inject TravelTime linkTravelTimes;
    @Inject TravelDisutility travelDisutility;
    @Inject TripRouterFactory tripRouterFactory;

    @Override
    public TripRouter get() {
        return tripRouterFactory.instantiateAndConfigureTripRouter(new RoutingContext() {

            @Override
            public TravelDisutility getTravelDisutility() {
                return travelDisutility;
            }

            @Override
            public TravelTime getTravelTime() {
                return linkTravelTimes;
            }

        });
    }

}
