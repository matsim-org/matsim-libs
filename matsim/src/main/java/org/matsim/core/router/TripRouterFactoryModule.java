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

import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterModule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TripRouterFactoryModule extends AbstractModule {
    @Override
    public void install() {
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
            addRoutingModuleBinding(mode).toProvider(new PseudoTransitRoutingModuleProvider(getConfig().plansCalcRoute().getModeRoutingParams().get(mode)));
        }
        for (String mode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
            addRoutingModuleBinding(mode).toProvider(new TeleportationRoutingModuleProvider(getConfig().plansCalcRoute().getModeRoutingParams().get(mode)));
        }
        for (String mode : routeConfigGroup.getNetworkModes()) {
            addRoutingModuleBinding(mode).toProvider(new NetworkRoutingModuleProvider(mode));
        }
        if (getConfig().transit().isUseTransit()) {
            for (String mode : getConfig().transit().getTransitModes()) {
                addRoutingModuleBinding(mode).toProvider(TransitRoutingModuleProvider.class);
            }
            addRoutingModuleBinding(TransportMode.transit_walk).to(Key.get(RoutingModule.class, Names.named(TransportMode.walk)));
        }
    }

    private static class TransitRoutingModuleProvider implements Provider<RoutingModule> {

        private final TransitRouter transitRouter;

        private final Scenario scenario;

        private final RoutingModule transitWalkRouter;

        @Inject
        TransitRoutingModuleProvider(TransitRouter transitRouter, Scenario scenario, @Named(TransportMode.transit_walk) RoutingModule transitWalkRouter) {
            this.transitRouter = transitRouter;
            this.scenario = scenario;
            this.transitWalkRouter = transitWalkRouter;
        }

        @Override
        public RoutingModule get() {
            return new TransitRouterWrapper(transitRouter,
                        scenario.getTransitSchedule(),
                        scenario.getNetwork(),
                        transitWalkRouter);
        }
    }

    public static class NetworkRoutingModuleProvider implements Provider<RoutingModule> {

        @Inject
        Map<String, TravelTime> travelTimes;

        @Inject
        Map<String, TravelDisutilityFactory> travelDisutilityFactory;

        @Inject
        SingleModeNetworksCache singleModeNetworksCache;

        @Inject
        Scenario scenario;

        @Inject
        LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

        public NetworkRoutingModuleProvider(String mode) {
            this.mode = mode;
        }

        private String mode;

        @Override
        public RoutingModule get() {
            Network filteredNetwork = null;

            // Ensure this is not performed concurrently by multiple threads!
            synchronized (this.singleModeNetworksCache.getSingleModeNetworksCache()) {
                filteredNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(mode);
                if (filteredNetwork == null) {
                    TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
                    Set<String> modes = new HashSet<>();
                    modes.add(mode);
                    filteredNetwork = NetworkUtils.createNetwork();
                    filter.filter(filteredNetwork, modes);
                    this.singleModeNetworksCache.getSingleModeNetworksCache().put(mode, filteredNetwork);
                }
            }

            LeastCostPathCalculator routeAlgo =
                    leastCostPathCalculatorFactory.createPathCalculator(
                            filteredNetwork,
                            travelDisutilityFactory.get(mode).createTravelDisutility(travelTimes.get(mode), scenario.getConfig().planCalcScore()),
                            travelTimes.get(mode));

            return DefaultRoutingModules.createNetworkRouter(mode, scenario.getPopulation().getFactory(),
                    filteredNetwork, routeAlgo);
        }
    }

    private static class PseudoTransitRoutingModuleProvider implements Provider<RoutingModule> {

        private final PlansCalcRouteConfigGroup.ModeRoutingParams params;

        public PseudoTransitRoutingModuleProvider(PlansCalcRouteConfigGroup.ModeRoutingParams params) {
            this.params = params;
        }

        @Inject
        private Scenario scenario;

        @Inject
        private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

        @Override
        public RoutingModule get() {
            FreespeedTravelTimeAndDisutility ptTimeCostCalc =
                    new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
            LeastCostPathCalculator routeAlgoPtFreeFlow =
                    leastCostPathCalculatorFactory.createPathCalculator(
                            scenario.getNetwork(),
                            ptTimeCostCalc,
                            ptTimeCostCalc);
            return DefaultRoutingModules.createPseudoTransitRouter(params.getMode(), scenario.getPopulation().getFactory(),
                    scenario.getNetwork(), routeAlgoPtFreeFlow, params);
        }
    }

    private static class TeleportationRoutingModuleProvider implements Provider<RoutingModule> {

        private final PlansCalcRouteConfigGroup.ModeRoutingParams params;

        public TeleportationRoutingModuleProvider(PlansCalcRouteConfigGroup.ModeRoutingParams params) {
            this.params = params;
        }

        @Inject
        private Scenario scenario;

        @Override
        public RoutingModule get() {
            return DefaultRoutingModules.createTeleportationRouter(params.getMode(), scenario.getPopulation().getFactory(), params);
        }
    }

}
