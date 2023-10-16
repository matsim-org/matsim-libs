/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.*;

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.timing.TimeInterpretation;

public class LinkToLinkRouting
        implements Provider<RoutingModule> {
    private final String mode;

    @Inject
    PopulationFactory populationFactory;

    @Inject
    SingleModeNetworksCache singleModeNetworksCache;

    @Inject
		RoutingConfigGroup routingConfigGroup;

    @Inject
    LeastCostPathCalculatorFactory leastCostPathCalcFactory;

    @Inject
    Scenario scenario;

    @Inject
    Map<String, TravelDisutilityFactory> travelDisutilities;

    @Inject
    Network network;

    @Inject
    LinkToLinkTravelTime travelTimes;

    @Inject
    NetworkTurnInfoBuilderI networkTurnInfoBuilder;

    @Inject
    TimeInterpretation timeInterpretation;

    @Inject
    @Named(TransportMode.walk)
    private RoutingModule walkRouter;

    @Inject
    private MultimodalLinkChooser multimodalLinkChooser;

    public LinkToLinkRouting(String mode) {
        this.mode = mode;
    }


    /**
     * Similar logic as in the {@link NetworkRoutingProvider}. regarding access egress behaviour.
     * @see NetworkRoutingProvider#get()
     */
    @Override
    public RoutingModule get() {

        // the network refers to the (transport)mode:
        Network filteredNetwork = null;
        Network invertedNetwork = null;

        // Ensure this is not performed concurrently by multiple threads!
        synchronized (this.singleModeNetworksCache.getSingleModeNetworksCache()) {
            filteredNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(mode);
            invertedNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(mode + "-inv");
            if (filteredNetwork == null) {
                TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
                Set<String> modes = new HashSet<>();
                modes.add(mode);
                filteredNetwork = NetworkUtils.createNetwork(scenario.getConfig().network());
                filter.filter(filteredNetwork, modes);

                invertedNetwork = new NetworkInverter(filteredNetwork, networkTurnInfoBuilder.createAllowedTurnInfos()).getInvertedNetwork();

                this.singleModeNetworksCache.getSingleModeNetworksCache().put(mode, filteredNetwork);
                this.singleModeNetworksCache.getSingleModeNetworksCache().put(mode + "-inv", invertedNetwork);
            }
        }

        InvertedLeastPathCalculator leastCostPathCalculator = InvertedLeastPathCalculator.create(
                leastCostPathCalcFactory, travelDisutilities.get(mode),
                filteredNetwork, invertedNetwork, travelTimes
        );

        // see NetworkRoutingProvider for some notes
        if (!routingConfigGroup.getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none)) {
            if (mode.equals(TransportMode.walk)) {
                return DefaultRoutingModules.createAccessEgressNetworkRouter(mode, leastCostPathCalculator, scenario,
                        filteredNetwork, invertedNetwork, null,null, timeInterpretation, multimodalLinkChooser);
            } else {
                return DefaultRoutingModules.createAccessEgressNetworkRouter(mode, leastCostPathCalculator, scenario,
                        filteredNetwork, invertedNetwork, walkRouter, walkRouter, timeInterpretation, multimodalLinkChooser);
            }

        } else {
            // pure inverted router
            return new LinkToLinkRoutingModule(mode, populationFactory, filteredNetwork, invertedNetwork, leastCostPathCalculator);
        }
    }
}
