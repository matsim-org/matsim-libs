/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerConfigLoader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import java.util.Map;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser
 */
public final class StrategyManagerConfigLoader {

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);

	private static int externalCounter = 0;

	public static void load(final Controler controler, final StrategyManager manager) {
		load(controler.getInjector(), controler.getInjector().getPlanStrategies(), controler.getInjector().getPlanSelectorsForRemoval(), manager);
	}

    public static void load(Injector injector, Map<String, PlanStrategy> planStrategies, Map<String, GenericPlanSelector<Plan, Person>> planSelectorsForRemoval, StrategyManager manager) {
        Config config = injector.getInstance(Config.class);
        manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

        int globalInnovationDisableAfter = (int) ((config.controler().getLastIteration() - config.controler().getFirstIteration())
                * config.strategy().getFractionOfIterationsToDisableInnovation() + config.controler().getFirstIteration());
        log.info("global innovation switch off after iteration: " + globalInnovationDisableAfter);

        manager.setSubpopulationAttributeName(
                config.plans().getSubpopulationAttributeName());
        for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
            String strategyName = settings.getStrategyName();

            PlanStrategy strategy = loadStrategy(strategyName, settings, planStrategies, injector);

            if (strategy == null) {
                throw new RuntimeException("Strategy named " + strategyName + " not found.");
            }

            manager.addStrategy(strategy, settings.getSubpopulation(), settings.getWeight());

            // now check if this modules should be disabled after some iterations
            int maxIter = settings.getDisableAfter();
            if ( maxIter > globalInnovationDisableAfter || maxIter==-1 ) {
                if (!PlanStrategies.isOnlySelector(strategy)) {
                    maxIter = globalInnovationDisableAfter ;
                }
            }

            if (maxIter >= 0) {
                if (maxIter >= config.controler().getFirstIteration()) {
                    manager.addChangeRequest(maxIter + 1, strategy, settings.getSubpopulation(), 0.0);
                } else {
                    /* The controler starts at a later iteration than this change request is scheduled for.
                     * make the change right now.					 */
                    manager.changeWeightOfStrategy(strategy, settings.getSubpopulation(), 0.0);
                }
            }
        }
        String name = config.strategy().getPlanSelectorForRemoval();
        if ( name != null ) {
            // ``manager'' has a default setting.
            GenericPlanSelector<Plan, Person> planSelectorForRemoval = planSelectorsForRemoval.get(name);
            if (planSelectorForRemoval == null) {
                throw new RuntimeException("Plan selector for removal named " + name + " not found.");
            }
            manager.setPlanSelectorForRemoval(planSelectorForRemoval) ;
        }
    }

    private static PlanStrategy loadStrategy(final String name, final StrategyConfigGroup.StrategySettings settings, Map<String, PlanStrategy> planStrategyFactoryRegister, Injector injector) {
		if (name.equals("ExternalModule")) {
			externalCounter++;
            String exePath = settings.getExePath();
            PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>());
            builder.addStrategyModule(new ExternalModule(exePath, "ext" + externalCounter, injector.getInstance(OutputDirectoryHierarchy.class), injector.getInstance(Scenario.class)));
			return builder.build();
		} else if (name.contains(".")) {
            return tryToLoadPlanStrategyByName(name, injector);
		} else {
			return planStrategyFactoryRegister.get(name);
		}
	} 

	private static PlanStrategy tryToLoadPlanStrategyByName(final String name, Injector injector) {
		PlanStrategy strategy;
		//classes loaded by name must not be part of the matsim core
		if (name.startsWith("org.matsim.") && !name.startsWith("org.matsim.contrib.")) {
			throw new RuntimeException("Strategies in the org.matsim package must not be loaded by name!");
		} else {
			try {
				Class<?> klas = Class.forName(name);
                // Instantiates the class and injects it.
                strategy = (PlanStrategy) injector.getJITInstance(klas);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
        }
		return strategy;
	}

}
