/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TripRouterFactoryModule.java
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

package org.matsim.contrib.signals.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.LeastCostPathCalculatorModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.pt.router.TransitRouterModule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;


public class InvertedNetworkRoutingModuleModule extends AbstractModule {

    private static Logger log = Logger.getLogger(InvertedNetworkRoutingModuleModule.class);

    @Override
    public void install() {
        install(new LeastCostPathCalculatorModule());
        install(new TransitRouterModule()); // yy why?  kai, jul'15
        if (getConfig().controler().isLinkToLinkRoutingEnabled()) {
            addRoutingModuleBinding(TransportMode.car).toProvider(new InvertedNetworkRoutingModuleProvider(TransportMode.car));
            log.warn("Link to link routing only affects car legs, which is correct if turning move costs only affect rerouting of car legs.");
        }
    }

    private static class InvertedNetworkRoutingModuleProvider implements Provider<RoutingModule> {

        String mode;

        @Inject
        Scenario scenario;

        @Inject
        LeastCostPathCalculatorFactory leastCostPathCalcFactory;

        @Inject
        Map<String, TravelDisutilityFactory> travelDisutilities;

        @Inject
        LinkToLinkTravelTime travelTimes;

        public InvertedNetworkRoutingModuleProvider(String mode) {
            this.mode = mode;
        }

        @Override
        public RoutingModule get() {
            return new InvertedNetworkRoutingModule( mode, scenario.getPopulation().getFactory(), scenario, leastCostPathCalcFactory, travelDisutilities.get(TransportMode.car), travelTimes) ;
        }
    }

}
