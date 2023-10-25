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

import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.pt.config.TransitConfigGroup.TransitRoutingAlgorithmType;
import org.matsim.pt.router.TransitRouterModule;


public class TripRouterModule extends AbstractModule {

    @Override
    public void install() {
	    // yy The code below will install _one_ LeastCostPathCalculator, which will be Dijkstra or Landmarks or something.  It will be the
	    // same Landmarks instance for all modes ... although one could do better by doing the preprocessing separately for the different modes.
	    // kai/mm, jan'17

        bind(TripRouter.class); // not thread-safe, not a singleton
        bind(MainModeIdentifier.class).to(MainModeIdentifierImpl.class);
        bind(AnalysisMainModeIdentifier.class).to(DefaultAnalysisMainModeIdentifier.class);

        bind(MultimodalLinkChooser.class).to(MultimodalLinkChooserDefaultImpl.class);

        install(new LeastCostPathCalculatorModule());
        install(new TransitRouterModule());
        bind(SingleModeNetworksCache.class).asEagerSingleton();
        RoutingConfigGroup routeConfigGroup = getConfig().routing();
        for (String mode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
            if (getConfig().transit().isUseTransit() && getConfig().transit().getTransitModes().contains(mode)) {
                // default config contains "pt" as teleported mode, but if we have simulated transit, this is supposed to override it
                // better solve this on the config level eventually.
                continue;
            }
            addRoutingModuleBinding(mode).toProvider(new FreespeedFactorRouting(getConfig().routing().getModeRoutingParams().get(mode)));
        }
        for (String mode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
            addRoutingModuleBinding(mode).toProvider(new BeelineTeleportationRouting(getConfig().routing().getModeRoutingParams().get(mode)));
        }

        boolean linkToLinkRouting = getConfig().controller().isLinkToLinkRoutingEnabled();
        if (linkToLinkRouting) {
            bind(NetworkTurnInfoBuilderI.class).to(NetworkTurnInfoBuilder.class) ;
        }
        for (String mode : routeConfigGroup.getNetworkModes()) {
            addRoutingModuleBinding(mode).toProvider(linkToLinkRouting ? //
                    new LinkToLinkRouting(mode) : new NetworkRoutingProvider(mode));
        }
        if (getConfig().transit().isUseTransit()) {
            if (getConfig().transit().getRoutingAlgorithmType() != TransitRoutingAlgorithmType.SwissRailRaptor) {
                // the SwissRailRaptorModule adds the routingModuleBinding itself
                for (String mode : getConfig().transit().getTransitModes()) {
                    addRoutingModuleBinding(mode).toProvider(Transit.class);
                }
            }
        }

        this.bind( FallbackRoutingModule.class ).to( FallbackRoutingModuleDefaultImpl.class ) ;
    }
}
