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

package commercialtraffic.commercialJob;/*
 * created by jbischoff, 03.05.2019
 */

import com.google.inject.Singleton;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import javax.inject.Inject;
import javax.inject.Provider;

public class CommercialTrafficModule extends AbstractModule {

    @Override
    public void install() {

        if(MultiModeDrtConfigGroup.get(getConfig()) != null){
            installDRT();
        }

        CommercialTrafficConfigGroup ctcg = CommercialTrafficConfigGroup.get(getConfig());

        //read input
        Carriers carriers = new Carriers();
        new CarrierPlanXmlReader(carriers).readFile(ctcg.getCarriersFileUrl(getConfig().getContext()).getFile());
        CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(vehicleTypes).readFile(ctcg.getCarriersVehicleTypesFileUrl(getConfig().getContext()).getFile());
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);

        //bind commercial Traffic stuff
        bind(CommercialJobScoreCalculator.class).toInstance(new DefaultCommercialServiceScore(ctcg.getMaxJobScore(), ctcg.getMinJobScore(), ctcg.getZeroUtilityDelay()));
        bind(Carriers.class).toInstance(carriers);

        bind(ScoreCommercialJobs.class).in(Singleton.class);
        bind(TourLengthAnalyzer.class).in(Singleton.class);
        addControlerListenerBinding().to(CommercialJobGenerator.class);
        addControlerListenerBinding().to(CommercialTrafficAnalysisListener.class);
//        addControlerListenerBinding().to(ScoreCommercialJobs.class);

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

}
