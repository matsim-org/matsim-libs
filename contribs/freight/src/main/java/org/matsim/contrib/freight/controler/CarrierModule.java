/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CarrierModule.java
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

package org.matsim.contrib.freight.controler;

import com.google.inject.Provider;
import org.matsim.contrib.freight.CarrierConfig;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.mobsim.FreightQSimFactory;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.controler.AbstractModule;

public class CarrierModule extends AbstractModule {

    // Not a real config group yet, but could be one.
    private CarrierConfig carrierConfig = new CarrierConfig();

    private Carriers carriers;
    private CarrierPlanStrategyManagerFactory strategyManagerFactory;
    private CarrierScoringFunctionFactory scoringFunctionFactory;


    public CarrierModule(Carriers carriers, CarrierPlanStrategyManagerFactory strategyManagerFactory, CarrierScoringFunctionFactory scoringFunctionFactory) {
        this.carriers = carriers;
        this.strategyManagerFactory = strategyManagerFactory;
        this.scoringFunctionFactory = scoringFunctionFactory;
    }

    public CarrierModule(String carrierPlansFilename, CarrierPlanStrategyManagerFactory strategyManagerFactory, CarrierScoringFunctionFactory scoringFunctionFactory) {
        this.carriers = new Carriers();
        new CarrierPlanXmlReaderV2(carriers).read(carrierPlansFilename);
        this.strategyManagerFactory = strategyManagerFactory;
        this.scoringFunctionFactory = scoringFunctionFactory;
    }

    @Override
    public void install() {
        // We put some things under dependency injection.
        bind(CarrierConfig.class).toInstance(carrierConfig);
        bind(Carriers.class).toInstance(carriers);
        bind(CarrierPlanStrategyManagerFactory.class).toInstance(strategyManagerFactory);
        bind(CarrierScoringFunctionFactory.class).toInstance(scoringFunctionFactory);

        // First, we need a ControlerListener.
        final CarrierControlerListener carrierControlerListener = new CarrierControlerListener(carriers, strategyManagerFactory, scoringFunctionFactory);
        addControlerListenerBinding().toInstance(carrierControlerListener);

        // We export CarrierAgentTracker, which is kept by the ControlerListener, which happens to re-create it every iteration.
        // The freight QSim needs it (see below).
        bind(CarrierAgentTracker.class).toProvider(new Provider<CarrierAgentTracker>() {
            @Override
            public CarrierAgentTracker get() {
                return carrierControlerListener.getCarrierAgentTracker();
            }
        });

        // Set the Mobsim. The FreightQSimFactory needs the CarrierAgentTracker (see constructor).
        bindMobsim().toProvider(FreightQSimFactory.class);
    }

    public void setPhysicallyEnforceTimeWindowBeginnings(boolean physicallyEnforceTimeWindowBeginnings) {
        this.carrierConfig.setPhysicallyEnforceTimeWindowBeginnings(physicallyEnforceTimeWindowBeginnings);
    }

}
