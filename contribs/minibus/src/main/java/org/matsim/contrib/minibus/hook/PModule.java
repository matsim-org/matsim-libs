/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Builder.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package org.matsim.contrib.minibus.hook;

import com.google.inject.Provider;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.stats.PStatsModule;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.PtMode2LineSetter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.TripRouter;

public class PModule {
    private AgentsStuckHandlerImpl agentsStuckHandler = null;
    private PersonReRouteStuckFactory stuckFactory = null;
    private PtMode2LineSetter lineSetter = null;
    private PTransitRouterFactory pTransitRouterFactory = null;
    private Class<? extends javax.inject.Provider<TripRouter>> tripRouterFactory = null;

    public void setLineSetter(PtMode2LineSetter lineSetter) {
        this.lineSetter = lineSetter;
    }
    public void setPTransitRouterFactory(PTransitRouterFactory pTransitRouterFactory) {
        this.pTransitRouterFactory = pTransitRouterFactory;
    }
    public void setStuckFactory(PersonReRouteStuckFactory stuckFactory) {
        this.stuckFactory = stuckFactory;
    }
    public void setTripRouterFactory(Class<? extends javax.inject.Provider<TripRouter>> tripRouterFactory) {
        this.tripRouterFactory = tripRouterFactory;
    }
    public void configureControler(final Controler controler) {
        PConfigGroup pConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PConfigGroup.GROUP_NAME, PConfigGroup.class);
        pConfig.validate(controler.getConfig().planCalcScore().getMarginalUtilityOfMoney());
        PBox pBox = new PBox(pConfig);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider(new Provider<Mobsim>() {
                    @Override
                    public Mobsim get() {
                        return new PQSimFactory().createMobsim(controler.getScenario(), controler.getEvents());
                    }
                });
            }
        });

        if (pTransitRouterFactory == null) {
            pTransitRouterFactory = new PTransitRouterFactory(pConfig.getPtEnabler(), pConfig.getPtRouter(), pConfig.getEarningsPerBoardingPassenger(), pConfig.getEarningsPerKilometerAndPassenger() / 1000.0);

            // For some unknown reason, the core starts failing when I start requesting a trip router out of an injected trip router in 
            // ScoreStatsControlerListener.  Presumably, the manually constructed build procedure here conflicts with the (newer) standard guice
            // procedure.  For the time being, the following two lines seem to fix it.  kai, nov'16
            pTransitRouterFactory.createTransitRouterConfig(controler.getConfig());
            pTransitRouterFactory.updateTransitSchedule(controler.getScenario().getTransitSchedule());
        }
        controler.setTripRouterFactory(PTripRouterFactoryFactory.getTripRouterFactoryInstance(controler, tripRouterFactory, this.pTransitRouterFactory));

        if (pConfig.getReRouteAgentsStuck()) {
            this.agentsStuckHandler = new AgentsStuckHandlerImpl();
            if(stuckFactory == null) {
                this.stuckFactory = new PersonReRouteStuckFactoryImpl();
            }
        }

        PStatsModule.configureControler(controler, pConfig, pBox, lineSetter);

        PControlerListener pHook = new PControlerListener(controler, pBox, pTransitRouterFactory, stuckFactory, agentsStuckHandler);
        controler.addControlerListener(pHook);


    }
}
