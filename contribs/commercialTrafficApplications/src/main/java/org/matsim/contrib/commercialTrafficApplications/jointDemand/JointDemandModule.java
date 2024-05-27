/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;/*
 * created by jbischoff, 03.05.2019
 */

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCostsFactory;
import org.matsim.freight.carriers.jsprit.VRPTransportCostsFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.util.TravelTime;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.Map;

public class JointDemandModule extends AbstractModule {

    @Override
    public void install() {

        if(MultiModeDrtConfigGroup.get(getConfig()) != null){
            installDRT();
        }

        JointDemandConfigGroup ctcg = JointDemandConfigGroup.get(getConfig());

        bind(CommercialJobScoreCalculator.class).toInstance(new DefaultCommercialServiceScore(ctcg.getMaxJobScore(), ctcg.getMinJobScore(), ctcg.getZeroUtilityDelay()));
        bind(Carriers.class).toProvider(new CarrierProvider());

        bind(ScoreCommercialJobs.class).in(Singleton.class);
        bind(TourLengthAnalyzer.class).in(Singleton.class);
        bind(CommercialJobGenerator.class).to(DefaultCommercialJobGenerator.class).in(Singleton.class);
        addControlerListenerBinding().to(CommercialJobGenerator.class);
        addControlerListenerBinding().to(CommercialTrafficAnalysisListener.class);
        bind(VRPTransportCostsFactory.class).to(NetworkBasedTransportCostsFactory.class).in(Singleton.class);

        //bind strategy that enables to choose between operators
        addPlanStrategyBinding(ChangeCommercialJobOperator.SELECTOR_NAME).toProvider(new Provider<PlanStrategy>() {
            @Inject
            Config config;
            @Inject
            Carriers carriers;
            @Override
            public PlanStrategy get() {
                final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
                builder.addStrategyModule(new ChangeCommercialJobOperator(config.global(), carriers));
                return builder.build();
            }
        });
    }

    private void installDRT(){
        install(new MultiModeDrtModule());
        install(new DvrpModule());
    }


    private class CarrierProvider implements com.google.inject.Provider<Carriers> {
        @com.google.inject.Inject
        Scenario scenario;

        private CarrierProvider() {
        }

        public Carriers get() {
            return CarriersUtils.getCarriers(this.scenario);
        }
    }

    @Provides
    @Singleton
    private NetworkBasedTransportCostsFactory provideNetworkBasedTransportCostsFactory(Scenario scenario,
                                                                                       Carriers carriers, Map<String, TravelTime> travelTimes, Config config) {

        return new NetworkBasedTransportCostsFactory(scenario,
                carriers, travelTimes, config);
    }

}
