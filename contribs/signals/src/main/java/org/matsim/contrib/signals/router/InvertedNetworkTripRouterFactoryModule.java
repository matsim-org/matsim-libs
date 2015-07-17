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

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.LeastCostPathCalculatorModule;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.pt.router.TransitRouterModule;

public class InvertedNetworkTripRouterFactoryModule extends AbstractModule {
    @Override
    public void install() {
        install(new LeastCostPathCalculatorModule());
        install(new TransitRouterModule()); // yy why?  kai, jul'15
        if (getConfig().controler().isLinkToLinkRoutingEnabled()) {
            bind(TripRouterFactory.class).to(LinkToLinkTripRouterFactory.class).in(Singleton.class);
        }
    }
}
