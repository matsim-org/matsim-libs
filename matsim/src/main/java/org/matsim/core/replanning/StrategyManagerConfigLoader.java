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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.PlanStrategyFactoryRegister;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.controler.PlanStrategyRegistrar.Selector;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser
 */
public final class StrategyManagerConfigLoader {

	public static final String LOCATION_CHOICE = "LocationChoice";

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);

	private static int externalCounter = 0;

	public static void load(final Controler controler, final StrategyManager manager) {
		PlanStrategyRegistrar planStrategyFactoryRegistrar = new PlanStrategyRegistrar();
		PlanStrategyFactoryRegister planStrategyFactoryRegister = planStrategyFactoryRegistrar.getFactoryRegister();
		load(controler.getScenario(), controler.getControlerIO(), controler.getEvents(), manager, planStrategyFactoryRegister);
	}

	public static void load(Scenario scenario, OutputDirectoryHierarchy controlerIO, EventsManager events, final StrategyManager manager, PlanStrategyFactoryRegister planStrategyFactoryRegister) {
		Config config = scenario.getConfig();
		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());
		
		int globalInnovationDisableAfter = (int) ((config.controler().getLastIteration() - config.controler().getFirstIteration()) 
				* config.strategy().getFractionOfIterationsToDisableInnovation() + config.controler().getFirstIteration());
		log.info("global innovation switch off after iteration: " + globalInnovationDisableAfter);

		manager.setSubpopulationAttributeName(
				config.plans().getSubpopulationAttributeName() );
		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
				// yyyy is this so practical?  Some people might want to instantiate it, and set the rate/probability/weight to something different
				// from zero at a later iteration.  (Not possible from config, but possible in code.)  kai, nov'13
				// yy It is neither a rate nor a probability, since either would need to be normalized. kai, nov'13
			}
			String moduleName = settings.getModuleName();

			if (moduleName.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				moduleName = moduleName.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}

			PlanStrategy strategy = loadStrategy(scenario, controlerIO, events, moduleName, settings, planStrategyFactoryRegister);

			if (strategy == null) {
				throw new RuntimeException("Could not initialize strategy named " + moduleName);
			}

			manager.addStrategy(strategy, settings.getSubpopulation() , rate);

			// now check if this modules should be disabled after some iterations
			int maxIter = settings.getDisableAfter();
			// --- begin new ---
			if ( maxIter > globalInnovationDisableAfter || maxIter==-1 ) {
				boolean innovative = true ;
				for ( Selector sel : Selector.values() ) {
					System.out.flush();
					if ( moduleName.equals( sel.toString() ) ) {
						innovative = false ;
						break ;
					}
				}
				if ( innovative ) {
					maxIter = globalInnovationDisableAfter ;
				}
			}
			// --- end new ---
			if (maxIter >= 0) {
				if (maxIter >= config.controler().getFirstIteration()) {
					manager.addChangeRequest(maxIter + 1, strategy, settings.getSubpopulation() , 0.0);
				} else {
					/* The controler starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					manager.changeWeightOfStrategy(strategy, settings.getSubpopulation() , 0.0);
				}
			}
		}
		String name = config.strategy().getPlanSelectorForRemoval();
		if ( name != null ) {
			// yyyy ``manager'' has a default setting.  I do not want to override this here except when it is configured.
			// Presumably, this is not the desired approach and the default should be in the config file?  kai, feb'12
			GenericPlanSelector<Plan> planSelector = null ;
			if ( name.equals("WorstPlanSelector") ) { 
				planSelector = new WorstPlanForRemovalSelector<Plan>(); 
			} else if ( name.equals("SelectRandom") ) {
				planSelector = new RandomPlanSelector();
			} else if ( name.equals("SelectExpBeta") ) {
				planSelector = new ExpBetaPlanSelector( - config.planCalcScore().getBrainExpBeta());
			} else if ( name.equals("ChangeExpBeta") ) {
				planSelector = new ExpBetaPlanChanger( - config.planCalcScore().getBrainExpBeta());
//			} else if ( name.equals("BestPlanSelector") ) {
//				planSelector = new BestPlanSelector();
				// does not make sense, thus commented out. kai, oct'13
			} else if ( name.equals("PathSizeLogitSelector") ) {
				planSelector = new PathSizeLogitSelector(config.planCalcScore().getPathSizeLogitBeta(), -config.planCalcScore().getBrainExpBeta(), 
						scenario.getNetwork());
			} else {
				throw new RuntimeException("Unknown 'plan selector for removal'.");
			}
			manager.setPlanSelectorForRemoval(planSelector) ;
		}
	}

	private static PlanStrategy loadStrategy(Scenario scenario, OutputDirectoryHierarchy controlerIO, EventsManager events, final String name, final StrategyConfigGroup.StrategySettings settings, PlanStrategyFactoryRegister planStrategyFactoryRegister) {
		// Special cases, scheduled to go away.
		if (name.equals(LOCATION_CHOICE)) {
			PlanStrategy strategy = tryToLoadPlanStrategyByName(scenario, "org.matsim.contrib.locationchoice.LocationChoicePlanStrategy");
			return strategy;
		} else if (name.equals("ExternalModule")) {
			externalCounter++;
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			String exePath = settings.getExePath();
			ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controlerIO, scenario);
			strategy.addStrategyModule(em);
			return strategy;
		} else if (name.contains(".")) {
			PlanStrategy strategy = tryToLoadPlanStrategyByName(scenario, name);
			return strategy;
		} else {
			PlanStrategyFactory planStrategyFactory = planStrategyFactoryRegister.getInstance(name);
			PlanStrategy strategy = planStrategyFactory.createPlanStrategy(scenario, events);
			return strategy;
		} 
	} 


	private static PlanStrategy tryToLoadPlanStrategyByName(final Scenario scenario, final String name) {
		PlanStrategy strategy;
		//classes loaded by name must not be part of the matsim core
		if (name.startsWith("org.matsim.") && !name.startsWith("org.matsim.contrib.")) {
			throw new RuntimeException("Strategies in the org.matsim package must not be loaded by name!");
		} else {
			try {
				Class<? extends PlanStrategy> klas = (Class<? extends PlanStrategy>) Class.forName(name);
				Class<?>[] args = new Class[1];
				args[0] = Scenario.class;
				Constructor<? extends PlanStrategy> c = null;
				c = klas.getConstructor(args);
				strategy = c.newInstance(scenario);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				log.info("Cannot find Constructor in PlanStrategy " + name + " with single argument of type Scenario. ");
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		return strategy;
	}

}
