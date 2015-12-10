/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ScenarioElementsModule.java
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

package org.matsim.core.scenario;

import com.google.inject.Provides;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;

public class ScenarioElementsModule extends AbstractModule {

    public ScenarioElementsModule() {
    }

    public ScenarioElementsModule(Config config) {
        super(config);
    }

    @Override
    public void install() {
        bind(Network.class).toProvider(NetworkProvider.class);
        bind(Population.class).toProvider(PopulationProvider.class);
        bind(PopulationFactory.class).to(PopulationFactoryImpl.class).asEagerSingleton();
        bind(ModeRouteFactory.class).asEagerSingleton();
        if (getConfig().transit().isUseTransit()) {
            bind(TransitSchedule.class).toProvider(TransitScheduleProvider.class);
        }
    }

    @Provides
    ActivityFacilities provideActivityFacilities(Scenario scenario) {
        return scenario.getActivityFacilities();
    }

    private static class NetworkProvider implements Provider<Network> {

        @Inject
        Scenario scenario;

        @Override
        public Network get() {
            return scenario.getNetwork();
        }

    }

    private static class PopulationProvider implements Provider<Population> {

        @Inject
        Scenario scenario;


        @Override
        public Population get() {
            return scenario.getPopulation();
        }

    }

    private static class TransitScheduleProvider implements Provider<TransitSchedule> {

        @Inject
        Scenario scenario;

        @Override
        public TransitSchedule get() {
            return scenario.getTransitSchedule();
        }

    }

}
