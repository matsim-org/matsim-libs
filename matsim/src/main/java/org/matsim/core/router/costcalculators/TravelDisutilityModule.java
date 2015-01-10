/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TravelDisutilityModule.java
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

package org.matsim.core.router.costcalculators;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;

public class TravelDisutilityModule extends AbstractModule {

    @Override
    public void install() {
        bindTo(TravelDisutilityFactory.class, TravelTimeAndDistanceBasedTravelDisutilityFactory.class);
        bindToProvider(TravelDisutility.class, TravelDisutilityProvider.class);
    }

    private static class TravelDisutilityProvider implements Provider<TravelDisutility> {

        final TravelDisutilityFactory travelDisutilityFactory;
        final Config config;
        final TravelTime travelTime;

        @Inject
        TravelDisutilityProvider(TravelDisutilityFactory travelDisutilityFactory, Config config, TravelTime travelTime) {
            this.travelDisutilityFactory = travelDisutilityFactory;
            this.config = config;
            this.travelTime = travelTime;
        }

        @Override
        public TravelDisutility get() {
            return travelDisutilityFactory.createTravelDisutility(travelTime, config.planCalcScore());
        }

    }

}
