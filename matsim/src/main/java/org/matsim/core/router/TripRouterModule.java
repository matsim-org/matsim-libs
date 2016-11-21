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
import com.google.inject.Provides;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouterModule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class TripRouterModule extends AbstractModule {

    @Override
    public void install() {
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
        for (String mode : routeConfigGroup.getNetworkModes()) {
            addRoutingModuleBinding(mode).toProvider(new NetworkRouting(mode));
        }
        if (getConfig().transit().isUseTransit()) {
            for (String mode : getConfig().transit().getTransitModes()) {
                addRoutingModuleBinding(mode).toProvider(Transit.class);
            }
            addRoutingModuleBinding(TransportMode.transit_walk).to(Key.get(RoutingModule.class, Names.named(TransportMode.walk)));
        }

    }

}
