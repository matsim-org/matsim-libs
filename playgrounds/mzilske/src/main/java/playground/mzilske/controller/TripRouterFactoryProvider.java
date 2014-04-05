/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TripRouterFactoryProvider.java
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

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.LinkToLinkTripRouterFactory;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.pt.router.TransitRouterFactory;

import javax.annotation.Nullable;
import javax.inject.Provider;

public class TripRouterFactoryProvider implements Provider<TripRouterFactory> {

    @Inject Config config;
    @Inject Scenario scenario;
    @Inject LinkToLinkTravelTime linkToLinkTravelTimes;
    @Inject LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
    @Inject TravelDisutilityFactory travelDisutilityFactory;
    @Inject @Nullable TransitRouterFactory transitRouterFactory;

    @Override
    public TripRouterFactory get() {
        DefaultTripRouterFactoryImpl defaultTripRouterFactory = new DefaultTripRouterFactoryImpl(scenario, leastCostPathCalculatorFactory, transitRouterFactory);
        if (config.controler().isLinkToLinkRoutingEnabled()) {
            return new LinkToLinkTripRouterFactory(
                    scenario,
                    leastCostPathCalculatorFactory,
                    travelDisutilityFactory,
                    linkToLinkTravelTimes,
                    scenario.getPopulation().getFactory(),
                    defaultTripRouterFactory);
        } else {
            return defaultTripRouterFactory;
        }
    }

}
