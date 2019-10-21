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

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.pt.router.TransitRouterModule;


public class TripRouterModule extends AbstractModule {

    @Override
    public void install() {
	    // yy The code below will install _one_ LeastCostPathCalculator, which will be Dijkstra or Landmarks or something.  It will be the
	    // same Landmarks instance for all modes ... although one could do better by doing the preprocessing separately for the different modes.
	    // kai/mm, jan'17

        bind(TripRouter.class); // not thread-safe, not a singleton
        bind(MainModeIdentifier.class).to(MainModeIdentifierImpl.class);
        install(new LeastCostPathCalculatorModule());
        install(new TransitRouterModule());
        bind(SingleModeNetworksCache.class).asEagerSingleton();
        PlansCalcRouteConfigGroup routeConfigGroup = getConfig().plansCalcRoute();
        for (String mode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
            if (getConfig().transit().isUseTransit() && getConfig().transit().getTransitModes().contains(mode)) {
                // default config contains "pt" as teleported mode, but if we have simulated transit, this is supposed to override it
                // better solve this on the config level eventually.
                continue;
            }
            addRoutingModuleBinding(mode).toProvider(new FreespeedFactorRouting(getConfig().plansCalcRoute().getModeRoutingParams().get(mode)));
        }
        for (String mode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
            addRoutingModuleBinding(mode).toProvider(new BeelineTeleportationRouting(getConfig().plansCalcRoute().getModeRoutingParams().get(mode)));
        }
        
        boolean linkToLinkRouting = getConfig().controler().isLinkToLinkRoutingEnabled();
        if (linkToLinkRouting) {
            bind(NetworkTurnInfoBuilderI.class).to(NetworkTurnInfoBuilder.class) ;
        }
        for (String mode : routeConfigGroup.getNetworkModes()) {
            addRoutingModuleBinding(mode).toProvider(linkToLinkRouting ? //
                    new LinkToLinkRouting(mode) : new NetworkRoutingProvider(mode));
        }
        if (getConfig().transit().isUseTransit()) {
            for (String mode : getConfig().transit().getTransitModes()) {
                addRoutingModuleBinding(mode).toProvider(Transit.class);
            }
            addRoutingModuleBinding(TransportMode.transit_walk).to(Key.get(RoutingModule.class, Names.named(TransportMode.walk)));
        }
//        addRoutingModuleBinding( FallbackRoutingModule._fallback ).to( FallbackRoutingModule.class ) ;

        this.bind( FallbackRoutingModule.class ).to( FallbackRoutingModuleDefaultImpl.class ) ;
    }
}
