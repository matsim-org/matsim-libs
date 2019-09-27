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

package commercialtraffic.integration;/*
 * created by jbischoff, 03.05.2019
 */

import com.google.inject.Singleton;
import commercialtraffic.analysis.CommercialTrafficAnalysisListener;
import commercialtraffic.analysis.TourLengthAnalyzer;
import commercialtraffic.commercialJob.CommercialJobManager;
import commercialtraffic.commercialJob.FreightAgentInserter;
import commercialtraffic.replanning.ChangeDeliveryServiceOperator;
import commercialtraffic.scoring.DefaultCommercialServiceScore;
import commercialtraffic.scoring.DeliveryScoreCalculator;
import commercialtraffic.scoring.ScoreCommercialServices;
import org.matsim.api.core.v01.TransportMode;
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

    private final Config config;
    private final MultiModeDrtConfigGroup multiModeDrtCfgGroup = null;
    private final CarrierJSpritIterations iterationsForCarrier;
    private CarrierMode carrierMode;

    public CommercialTrafficModule(Config config, CarrierJSpritIterations iterationsForCarrier){
        super();
        this.config = config;
        this.iterationsForCarrier = iterationsForCarrier;
    }

    public CommercialTrafficModule(Config config, CarrierJSpritIterations iterationsForCarrier, CarrierMode carrierMode){
        super();
        this.carrierMode = carrierMode;
        this.config = config;
        this.iterationsForCarrier = iterationsForCarrier;
    }


    @Override
    public void install() {

        if(MultiModeDrtConfigGroup.get(config) != null){
            installDRT();
        }

        CommercialTrafficConfigGroup ctcg = CommercialTrafficConfigGroup.get(getConfig());

        //read input
        Carriers carriers = new Carriers();
        new CarrierPlanXmlReader(carriers).readFile(ctcg.getCarriersFileUrl(getConfig().getContext()).getFile());
        CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(vehicleTypes).readFile(ctcg.getCarriersVehicleTypesFileUrl(getConfig().getContext()).getFile());
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);

        //check consistency
        CommercialTrafficChecker consistencyChecker = new CommercialTrafficChecker();
        bind(CommercialTrafficChecker.class).toInstance(consistencyChecker);
        if (consistencyChecker.checkCarrierConsistency(carriers, getConfig())) {
            throw new RuntimeException("Carrier definition is invalid. Please check the log for details.");
        }

//        bind commercial Traffic stuff
        bind(DeliveryScoreCalculator.class).toInstance(new DefaultCommercialServiceScore(ctcg.getMaxDeliveryScore(), ctcg.getMinDeliveryScore(), ctcg.getZeroUtilityDelay()));
        bind(Carriers.class).toInstance(carriers);
        bind(CommercialJobManager.class).in(Singleton.class);
        bind(ScoreCommercialServices.class).in(Singleton.class);
        bind(TourLengthAnalyzer.class).in(Singleton.class);
        bind(FreightAgentInserter.class).in(Singleton.class);
        bind(CarrierJSpritIterations.class).toInstance(iterationsForCarrier);
        if(this.carrierMode == null){
            bind(CarrierMode.class).toInstance(carrierId -> TransportMode.car);
//            bind(CarrierMode.class).toInstance(carrierId -> TransportMode.drt);
        } else {
            bind(CarrierMode.class).toInstance(carrierMode);
        }
        addControlerListenerBinding().to(CommercialJobManager.class);
        addControlerListenerBinding().to(CommercialTrafficAnalysisListener.class);
        addMobsimListenerBinding().to(ScoreCommercialServices.class);
        addMobsimListenerBinding().to(CommercialTrafficChecker.class);

        //bind strategy that enables to choose between operators
        addPlanStrategyBinding(ChangeDeliveryServiceOperator.SELECTOR_NAME).toProvider(new Provider<PlanStrategy>() {
            @Inject
            Config config;
            @Inject
            CommercialJobManager manager;
            @Override
            public PlanStrategy get() {
                final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
                builder.addStrategyModule(new ChangeDeliveryServiceOperator(config.global(),manager));
                return builder.build();
            }
        });

    }


    private void installDRT(){
        install(new MultiModeDrtModule());
        install(new DvrpModule());
    }

}
