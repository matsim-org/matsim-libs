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

import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import javax.inject.Inject;
import javax.inject.Provider;

public class StrategyManagerModule extends AbstractModule {
	@Override
	public void install() {
		int externalCounter = 0;
		install(new DefaultPlanStrategiesModule());
		bind(StrategyManager.class).in(Singleton.class);
		bind(ReplanningContext.class).to(ReplanningContextImpl.class).asEagerSingleton();
		MapBinder<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategyMapBinder = MapBinder.newMapBinder(binder(), StrategyConfigGroup.StrategySettings.class, PlanStrategy.class);
		for (StrategyConfigGroup.StrategySettings settings : getConfig().strategy().getStrategySettings()) {
			String name = settings.getStrategyName() ;
			if (name.equals("ExternalModule")) {
				externalCounter++;
				planStrategyMapBinder.addBinding(settings).toProvider(new ExternalModuleProvider(externalCounter, settings.getExePath()));
			} else if (name.contains(".")) {
				if (name.startsWith("org.matsim.") && !name.startsWith("org.matsim.contrib.")) {
					throw new RuntimeException("Strategies in the org.matsim package must not be loaded by name!");
				} else {
					try {
						Class klass = Class.forName(name);
						if (PlanStrategy.class.isAssignableFrom(klass)) {
							planStrategyMapBinder.addBinding(settings).to(klass);
						} else if (Provider.class.isAssignableFrom(klass)) {
							planStrategyMapBinder.addBinding(settings).toProvider(klass);
						} else {
							throw new RuntimeException("You specified a class name as a strategy, but it is neither a PlanStrategy nor a Provider.");
						}
					} catch (ClassNotFoundException e) {
						throw new RuntimeException("You specified something which looks like a class name as a strategy, but the class could not be found.", e);
					}
				}
			} else {
				planStrategyMapBinder.addBinding(settings).to(Key.get(PlanStrategy.class, Names.named(settings.getStrategyName())));
			}
		}
	}

	private static class ExternalModuleProvider implements Provider<PlanStrategy> {

		@Inject
		private OutputDirectoryHierarchy controlerIO;

		@Inject
		private Scenario scenario;

		private int externalCounter;
		private String exePath;

		public ExternalModuleProvider(int externalCounter, String exePath) {
			this.externalCounter = externalCounter;
			this.exePath = exePath;
		}

		@Override
		public PlanStrategy get() {
			PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>());
			builder.addStrategyModule(new ExternalModule(exePath, "ext" + externalCounter, controlerIO, scenario));
			return builder.build();
		}
	}
}
