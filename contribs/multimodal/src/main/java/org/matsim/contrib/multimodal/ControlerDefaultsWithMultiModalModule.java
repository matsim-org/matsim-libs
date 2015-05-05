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
import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.TransitTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.LinkSlopesReader;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ControlerDefaultsWithMultiModalModule extends AbstractModule {

    private final Map<String, Provider<TravelTime>> additionalTravelTimeFactories = new LinkedHashMap<>();

    @Override
    public void install() {
        // use ControlerDefaults configuration, replacing the TripRouter with a multi-modal one
        install(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), new AbstractModule() {
            @Override
            public void install() {
                bind(TripRouterFactory.class).toProvider(MultiModalTripRouterFactoryProvider.class).in(Singleton.class);
            }
        }));
        addControlerListenerBinding().to(MultiModalControlerListener.class);
        bind(new TypeLiteral<Map<String, TravelTime>>() {
        }).toProvider(MultiModalTravelTimeLoader.class).in(Singleton.class);
        bind(new TypeLiteral<Map<String, Provider<TravelTime>>>() {
        }).toInstance(additionalTravelTimeFactories);
        bindMobsim().toProvider(MultimodalQSimFactory.class);
    }

    private static class MultiModalTravelTimeLoader implements Provider<Map<String, TravelTime>> {

        private final Scenario scenario;
        private final Map<String, Provider<TravelTime>> additionalTravelTimeFactories;

        @Inject
        MultiModalTravelTimeLoader(Scenario scenario, Map<String, Provider<TravelTime>> additionalTravelTimeFactories) {
            this.scenario = scenario;
            this.additionalTravelTimeFactories = additionalTravelTimeFactories;
        }

        @Override
        public Map<String, TravelTime> get() {
            MultiModalConfigGroup multiModalConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), MultiModalConfigGroup.GROUP_NAME, MultiModalConfigGroup.class);
            Map<Id<Link>, Double> linkSlopes = new LinkSlopesReader().getLinkSlopes(multiModalConfigGroup, scenario.getNetwork());
            MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig(), linkSlopes, additionalTravelTimeFactories);
            return multiModalTravelTimeFactory.createTravelTimes();
        }
    }

    private static class MultiModalTripRouterFactoryProvider implements Provider<TripRouterFactory> {

        private Scenario scenario;
        private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
        private Provider<TransitRouter> transitRouterFactory;
        private Map<String, TravelTime> multiModalTravelTimes;
        private TravelDisutilityFactory travelDisutilityFactory;

        @Inject
        MultiModalTripRouterFactoryProvider(Scenario scenario, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, Provider<TransitRouter> transitRouterFactory, Map<String, TravelTime> multiModalTravelTimes, TravelDisutilityFactory travelDisutilityFactory) {
            this.scenario = scenario;
            this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
            this.transitRouterFactory = transitRouterFactory;
            this.multiModalTravelTimes = multiModalTravelTimes;
            this.travelDisutilityFactory = travelDisutilityFactory;
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
