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

import java.util.Map;

import javax.inject.*;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;


public class InvertedNetworkRoutingGuiceModule
    extends AbstractModule
{
    private final String mode;

    public InvertedNetworkRoutingGuiceModule()
    {
        this(TransportMode.car);
    }
    
    public InvertedNetworkRoutingGuiceModule(String mode)
    {
        this.mode = mode;
    }
    

    @Override
    public void install()
    {
        bind(NetworkTurnInfoBuilder.class);
        addRoutingModuleBinding(mode)
                .toProvider(new InvertedNetworkRoutingModuleProvider(mode));
    }


    public static class InvertedNetworkRoutingModuleProvider
        implements Provider<RoutingModule>
    {
        private final String mode;

        @Inject
        PopulationFactory populationFactory;

        @Inject
        LeastCostPathCalculatorFactory leastCostPathCalcFactory;

        @Inject
        Map<String, TravelDisutilityFactory> travelDisutilities;

        @Inject
        Network network;

        @Inject
        LinkToLinkTravelTime travelTimes;

        @Inject
        NetworkTurnInfoBuilder networkTurnInfoBuilder;


        public InvertedNetworkRoutingModuleProvider(String mode)
        {
            this.mode = mode;
        }


        @Override
        public RoutingModule get()
        {
            return new InvertedNetworkRoutingModule(mode, populationFactory, network,
                    leastCostPathCalcFactory, travelDisutilities.get(mode),
                    travelTimes, networkTurnInfoBuilder);
        }
    }

}
