/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MultiModalModule.java
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

package org.matsim.contrib.multimodal;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.util.BikeTravelTimeFactory;
import org.matsim.contrib.multimodal.router.util.UnknownTravelTimeFactory;
import org.matsim.contrib.multimodal.router.util.WalkTravelTimeFactory;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import jakarta.inject.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MultiModalModule extends AbstractModule {

    private static final Logger log = LogManager.getLogger(MultiModalModule.class);

    private final Map<String, Provider<TravelTime>> additionalTravelTimeFactories = new LinkedHashMap<>();

    private Map<Id<Link>, Double> linkSlopes;

    @Override
    public void install() {
        // yyyy with some thinking and testing, the following could be collapsed into nearly nothing:
        // * Factories for network-based routing can now be provided by config, so no need to provide them here.
        // * The multi-modal contrib now essentially is (I think) a "parallel" network for each mode.  In consequence, the corresponding network-based router
        // would just operate on that sub-network.
        // kai, dec'19

        RoutingConfigGroup routingConfigGroup = getConfig().routing();
        ScoringConfigGroup cnScoringGroup = getConfig().scoring();
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
        for (String mode : simulatedModes) {
            switch( mode ){
                case TransportMode.walk:{
                    Provider<TravelTime> factory = new WalkTravelTimeFactory(routingConfigGroup, linkSlopes );
                    addTravelTimeBinding( mode ).toProvider( factory );
                    addTravelDisutilityFactoryBinding( mode ).toInstance( new RandomizingTimeDistanceTravelDisutilityFactory( mode, getConfig() ) );
                    addRoutingModuleBinding( mode ).toProvider( new NetworkRoutingProvider( mode ) );
                    break;
                }
                case TransportMode.transit_walk:
//                Provider<TravelTime> factory = new TransitWalkTravelTimeFactory(plansCalcRouteConfigGroup, linkSlopes);
//                addTravelTimeBinding(mode).toProvider(factory);
//                addTravelDisutilityFactoryBinding(mode).toInstance( new RandomizingTimeDistanceTravelDisutility.Builder( mode ) );
                    addRoutingModuleBinding( mode ).to( Key.get( RoutingModule.class, Names.named( TransportMode.walk ) ) );
                    break;
                case TransportMode.bike:{
                    Provider<TravelTime> factory = new BikeTravelTimeFactory(routingConfigGroup, linkSlopes );
                    addTravelTimeBinding( mode ).toProvider( factory );
                    addTravelDisutilityFactoryBinding( mode ).toInstance( new RandomizingTimeDistanceTravelDisutilityFactory( mode, getConfig() ) );
                    addRoutingModuleBinding( mode ).toProvider( new NetworkRoutingProvider( mode ) );
                    break;
                }
                default:{
                    Provider<TravelTime> factory = additionalTravelTimeFactories.get( mode );
                    if( factory == null ){
                        log.warn( "Mode " + mode + " is not supported! " +
                                                  "Use a constructor where you provide the travel time objects. " +
                                                  "Using an UnknownTravelTime calculator based on constant speed." +
                                                  "Agent specific attributes are not taken into account!" );
                        factory = new UnknownTravelTimeFactory( mode, routingConfigGroup);
                    } else{
                        log.info( "Found additional travel time factory from type " + factory.getClass().toString() +
                                                  " for mode " + mode + "." );
                    }
                    addTravelTimeBinding( mode ).toProvider( factory );
                    addTravelDisutilityFactoryBinding( mode ).toInstance( new RandomizingTimeDistanceTravelDisutilityFactory( mode, getConfig() ) );
                    addRoutingModuleBinding( mode ).toProvider( new NetworkRoutingProvider( mode ) );
                    break;
                }
            }
        }
        addControllerListenerBinding().to(MultiModalControlerListener.class);
        bindMobsim().toProvider(MultimodalQSimFactory.class);
    }

    public void setLinkSlopes(Map<Id<Link>, Double> linkSlopes) {
        this.linkSlopes = linkSlopes;
    }

    public void addAdditionalTravelTimeFactory(String mode, Provider<TravelTime> travelTimeFactory) {
        this.additionalTravelTimeFactories.put(mode, travelTimeFactory);
    }


}
