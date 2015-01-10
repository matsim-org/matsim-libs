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

package org.matsim.core.router;

import com.google.inject.util.Providers;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterModule;

public class TripRouterFactoryModule extends AbstractModule {
    @Override
    public void install() {
        include(new LeastCostPathCalculatorModule());
        if (getConfig().scenario().isUseTransit()) {
            include(new TransitRouterModule());
        } else {
            bindToProvider(TransitRouterFactory.class, Providers.<TransitRouterFactory>of(null));
        }
        if (getConfig().controler().isLinkToLinkRoutingEnabled()) {
            bindAsSingleton(TripRouterFactory.class, LinkToLinkTripRouterFactory.class);
        } else {
            bindAsSingleton(TripRouterFactory.class, DefaultTripRouterFactoryImpl.class);
        }
    }
}
