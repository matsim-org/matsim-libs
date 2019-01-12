/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package vwExamples.utils.parking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import parking.ZonalLinkParkingInfo;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class CustomParkingRouterRoutingModuleProvider implements Provider<RoutingModule> {

    private static final Logger log = Logger.getLogger(CustomParkingRouterRoutingModuleProvider.class);

    private final String mode;
    private final String routingMode;

    @Inject
    Map<String, TravelTime> travelTimes;

    @Inject
    Map<String, TravelDisutilityFactory> travelDisutilityFactories;

    @Inject
    SingleModeNetworksCache singleModeNetworksCache;

    @Inject
    PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

    @Inject
    Network network;

    @Inject
    PopulationFactory populationFactory;

    @Inject
    LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

    @Inject
    ZonalLinkParkingInfo zoneToLinks;

    CustomParkingRouterRoutingModuleProvider(String mode) {
        this(mode, mode);
    }

    CustomParkingRouterRoutingModuleProvider(String mode, String routingMode) {
//		log.setLevel(Level.DEBUG);
        this.mode = mode;
        this.routingMode = routingMode;
    }

    @Override
    public RoutingModule get() {
        // the network refers to the (transport)mode:
        Network filteredNetwork = null;

        // Ensure this is not performed concurrently by multiple threads!
        synchronized (this.singleModeNetworksCache.getSingleModeNetworksCache()) {
            filteredNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(mode);
            if (filteredNetwork == null) {
                TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
                Set<String> modes = new HashSet<>();
                modes.add(mode);
                filteredNetwork = NetworkUtils.createNetwork();
                filter.filter(filteredNetwork, modes);
                this.singleModeNetworksCache.getSingleModeNetworksCache().put(mode, filteredNetwork);
            }
        }

        // the travel time & disutility refer to the routing mode:
        TravelDisutilityFactory travelDisutilityFactory = this.travelDisutilityFactories.get(routingMode);
        if (travelDisutilityFactory == null) {
            throw new RuntimeException("No TravelDisutilityFactory bound for mode " + routingMode + ".");
        }
        TravelTime travelTime = travelTimes.get(routingMode);
        if (travelTime == null) {
            throw new RuntimeException("No TravelTime bound for mode " + routingMode + ".");
        }
        LeastCostPathCalculator routeAlgo =
                leastCostPathCalculatorFactory.createPathCalculator(
                        filteredNetwork,
                        travelDisutilityFactory.createTravelDisutility(travelTime),
                        travelTime);

        // the following again refers to the (transport)mode, since it will determine the mode of the leg on the network:
        if (plansCalcRouteConfigGroup.isInsertingAccessEgressWalk()) {
            return new CustomParkingRouterNetworkRoutingModule(mode, populationFactory, filteredNetwork, routeAlgo,
                    plansCalcRouteConfigGroup, this.zoneToLinks);
        } else {
            return DefaultRoutingModules.createPureNetworkRouter(mode, populationFactory, filteredNetwork, routeAlgo);
        }
    }
}
