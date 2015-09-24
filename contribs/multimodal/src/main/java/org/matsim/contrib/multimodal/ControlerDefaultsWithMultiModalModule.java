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

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.TransitTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ControlerDefaultsWithMultiModalModule extends AbstractModule {

    private static final Logger log = Logger.getLogger(ControlerDefaultsWithMultiModalModule.class);

    private final Map<String, Provider<TravelTime>> additionalTravelTimeFactories = new LinkedHashMap<>();

    private Map<Id<Link>, Double> linkSlopes;

    @Override
    public void install() {
        // use ControlerDefaults configuration, replacing the TripRouter with a multi-modal one
        install(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), new AbstractModule() {
            @Override
            public void install() {
                bind(TripRouterFactory.class).toProvider(MultiModalTripRouterFactoryProvider.class).in(Singleton.class);
            }
        }));

        PlansCalcRouteConfigGroup plansCalcRouteConfigGroup = getConfig().plansCalcRoute();
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());

        for (String mode : simulatedModes) {
            if (mode.equals(TransportMode.walk)) {
                Provider<TravelTime> factory = new WalkTravelTimeFactory(plansCalcRouteConfigGroup, linkSlopes);
                addTravelTimeBinding(mode).toProvider(factory);
            } else if (mode.equals(TransportMode.transit_walk)) {
                Provider<TravelTime> factory = new TransitWalkTravelTimeFactory(plansCalcRouteConfigGroup, linkSlopes);
                addTravelTimeBinding(mode).toProvider(factory);
            } else if (mode.equals(TransportMode.bike)) {
                Provider<TravelTime> factory = new BikeTravelTimeFactory(plansCalcRouteConfigGroup, linkSlopes);
                addTravelTimeBinding(mode).toProvider(factory);
            } else {
                Provider<TravelTime> factory = this.additionalTravelTimeFactories.get(mode);
                if (factory == null) {
                    log.warn("Mode " + mode + " is not supported! " +
                            "Use a constructor where you provide the travel time objects. " +
                            "Using a UnknownTravelTime calculator based on constant speed." +
                            "Agent specific attributes are not taken into account!");
                    factory = new UnknownTravelTimeFactory(mode, plansCalcRouteConfigGroup);
                } else {
                    log.info("Found additional travel time factory from type " + factory.getClass().toString() +
                            " for mode " + mode + ".");
                }
                addTravelTimeBinding(mode).toProvider(factory);
            }
        }
        addControlerListenerBinding().to(MultiModalControlerListener.class);
        bindMobsim().toProvider(MultimodalQSimFactory.class);
    }

    public void setLinkSlopes(Map<Id<Link>, Double> linkSlopes) {
        this.linkSlopes = linkSlopes;
    }

    private static class MultiModalTripRouterFactoryProvider implements Provider<TripRouterFactory> {

        private Scenario scenario;
        private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
        private Provider<TransitRouter> transitRouterFactory;
        private Map<String, TravelTime> multiModalTravelTimes;
        private TravelDisutilityFactory travelDisutilityFactory;

        @Inject
        MultiModalTripRouterFactoryProvider(Scenario scenario, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, Provider<TransitRouter> transitRouterFactory, Map<String, TravelTime> multiModalTravelTimes, Map<String, TravelDisutilityFactory> travelDisutilityFactory) {
            this.scenario = scenario;
            this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
            this.transitRouterFactory = transitRouterFactory;
            this.multiModalTravelTimes = multiModalTravelTimes;
            this.travelDisutilityFactory = travelDisutilityFactory.get(TransportMode.car);
        }

        @Override
        /*
		 * Use a ...
		 * - defaultDelegateFactory for the QNetsim modes
		 * - multiModalTripRouterFactory for the multi-modal modes
		 * - transitTripRouterFactory for transit trips
		 *
		 * Note that a FastDijkstraFactory is used for the multiModalTripRouterFactory
		 * since ...
		 * - only "fast" router implementations handle sub-networks correct
		 * - AStarLandmarks uses link speed information instead of agent speeds
		 */
        public TripRouterFactory get() {
            TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(scenario, leastCostPathCalculatorFactory);
            TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes, travelDisutilityFactory, defaultDelegateFactory, new FastDijkstraFactory());
            TripRouterFactory transitTripRouterFactory = new TransitTripRouterFactory(scenario, multiModalTripRouterFactory, transitRouterFactory);
            return transitTripRouterFactory;
        }

    }

    public void addAdditionalTravelTimeFactory(String mode, Provider<TravelTime> travelTimeFactory) {
        this.additionalTravelTimeFactories.put(mode, travelTimeFactory);
    }


}
