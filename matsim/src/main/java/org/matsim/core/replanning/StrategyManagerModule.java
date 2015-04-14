/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * StrategyManagerModule.java
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

package org.matsim.core.replanning;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.replanning.selectors.GenericPlanSelector;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class StrategyManagerModule extends AbstractModule {
    @Override
    public void install() {
        install(new DefaultPlanStrategiesModule());
        bind(StrategyManager.class).toProvider(StrategyManagerProvider.class).in(Singleton.class);
        bind(ReplanningContext.class).to(ReplanningContextImpl.class);
    }

    private static class StrategyManagerProvider implements Provider<StrategyManager> {

        private Injector injector;
        private Map<String, GenericPlanSelector<Plan, Person>> planSelectorsDeclaredByModules;
        private Map<String, PlanStrategy> planStrategiesDeclaredByModules;

        @Inject
        StrategyManagerProvider(com.google.inject.Injector injector, Map<String, GenericPlanSelector<Plan, Person>> planSelectorsDeclaredByModules, Map<String, PlanStrategy> planStrategiesDeclaredByModules) {
            this.injector = Injector.fromGuiceInjector(injector);
            this.planSelectorsDeclaredByModules = planSelectorsDeclaredByModules;
            this.planStrategiesDeclaredByModules = planStrategiesDeclaredByModules;
        }

        @Override
        public StrategyManager get() {
            StrategyManager manager = new StrategyManager();
            StrategyManagerConfigLoader.load(injector, this.planStrategiesDeclaredByModules, this.planSelectorsDeclaredByModules, manager);
            return manager;
        }
    }
}
