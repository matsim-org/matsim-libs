/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RoadPricingModule.java
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

package org.matsim.roadpricing;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;

public class ControlerDefaultsWithRoadPricingModule extends AbstractModule {

    private final RoadPricingScheme roadPricingScheme;

    public ControlerDefaultsWithRoadPricingModule() {
        this.roadPricingScheme = null;
    }

    public ControlerDefaultsWithRoadPricingModule(RoadPricingScheme roadPricingScheme) {
        this.roadPricingScheme = roadPricingScheme;
    }

    @Override
    public void install() {
        // This is not optimal yet. Modules should not need to have parameters.
        // But I am not quite sure yet how to best handle custom scenario elements. mz
        if (this.roadPricingScheme != null) {
            bind(RoadPricingScheme.class).toInstance(this.roadPricingScheme);
        } else {
            bind(RoadPricingScheme.class).toProvider(RoadPricingSchemeProvider.class).in(Singleton.class);
        }

        bind(PlansCalcRouteWithTollOrNot.class);
        addPlanStrategyBinding("ReRouteAreaToll").toProvider(ReRouteAreaToll.class);

        // use ControlerDefaults configuration, replacing the TravelDisutility with a toll-dependent one
        install(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), new AbstractModule() {
            @Override
            public void install() {
                bind(TravelDisutilityFactory.class).toProvider(TravelDisutilityIncludingTollFactoryProvider.class);
            }
        }));

        addControlerListenerBinding().to(RoadPricingControlerListener.class);

        // add the events handler to calculate the tolls paid by agents
        bind(CalcPaidToll.class).in(Singleton.class);
        addEventHandlerBinding().to(CalcPaidToll.class);

        bind(CalcAverageTolledTripLength.class).in(Singleton.class);
        addEventHandlerBinding().to(CalcAverageTolledTripLength.class);
    }

    private static class RoadPricingSchemeProvider implements Provider<RoadPricingScheme> {

        private final Config config;

        @Inject
        RoadPricingSchemeProvider(Config config) {
            this.config = config;
        }

        @Override
        public RoadPricingScheme get() {
            RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
            String tollLinksFile = rpConfig.getTollLinksFile();
            if ( tollLinksFile == null ) {
                throw new RuntimeException("Road pricing inserted but neither toll links file nor RoadPricingScheme given.  "
                        + "Such an execution path is not allowed.  If you want a base case without toll, "
                        + "construct a zero toll file and insert that. ") ;
            }
            RoadPricingSchemeImpl rpsImpl = new RoadPricingSchemeImpl() ;
            new RoadPricingReaderXMLv1(rpsImpl).parse(tollLinksFile);
            return rpsImpl;
        }
    }

    private static class TravelDisutilityIncludingTollFactoryProvider implements Provider<TravelDisutilityFactory> {

        private final Scenario scenario;
        private final RoadPricingScheme scheme;

        @Inject
        TravelDisutilityIncludingTollFactoryProvider(Scenario scenario, RoadPricingScheme scheme) {
            this.scenario = scenario;
            this.scheme = scheme;
        }

        @Override
        public TravelDisutilityFactory get() {
            RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
            final TravelDisutilityFactory originalTravelDisutilityFactory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
			RoadPricingTravelDisutilityFactory travelDisutilityFactory = new RoadPricingTravelDisutilityFactory(
                    originalTravelDisutilityFactory, scheme, scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney()
            );
            travelDisutilityFactory.setSigma(rpConfig.getRoutingRandomness());
            return travelDisutilityFactory;
        }

    }

}
