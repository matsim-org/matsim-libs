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

package playground.singapore.typesPopulation.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.*;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.selectors.*;
import playground.singapore.typesPopulation.config.groups.StrategyPopsConfigGroup;
import playground.singapore.typesPopulation.config.groups.StrategyPopsConfigGroup.StrategySettings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser, sergioo
 */
public final class StrategyManagerPopsConfigLoader {

	public static final String LOCATION_CHOICE = "LocationChoice";

	private static final Logger log = Logger.getLogger(StrategyManagerPopsConfigLoader.class);

	private static int externalCounter = 0;

	public static void load(final Controler controler, final StrategyManagerPops manager) {
		PlanStrategyRegistrar planStrategyFactoryRegistrar = new PlanStrategyRegistrar();
		PlanStrategyFactoryRegister planStrategyFactoryRegister = planStrategyFactoryRegistrar.getFactoryRegister();
		load(controler, manager, planStrategyFactoryRegister);
	}

	public static void load(final Controler controler, final StrategyManagerPops manager, PlanStrategyFactoryRegister planStrategyFactoryRegister) {
		Config config = controler.getConfig();
		StrategyPopsConfigGroup strategyPops = (StrategyPopsConfigGroup) config.getModule(StrategyPopsConfigGroup.GROUP_NAME);
		manager.setMaxPlansPerAgent(0);
		for(String populationId:strategyPops.getPopulationIds()) {
			manager.setMaxPlansPerAgent(strategyPops.getMaxAgentPlanMemorySize(populationId), Id.create(populationId, Population.class));
			
			int globalInnovationDisableAfter = (int) ( (config.controler().getLastIteration() - config.controler().getFirstIteration()) 
					* strategyPops.getFractionOfIterationsToDisableInnovation(populationId) + config.controler().getFirstIteration() ) ;
			Logger.getLogger("blabla").info( "global innovation switch of after iteration: " + globalInnovationDisableAfter ) ;
	
			for (StrategySettings settings : strategyPops.getStrategySettings(populationId)) {
				double rate = settings.getProbability();
				if (rate == 0.0) {
					continue;
				}
				String moduleName = settings.getModuleName();
	
				if (moduleName.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
					moduleName = moduleName.replace("org.matsim.demandmodeling.plans.strategies.", "");
				}
	
				PlanStrategy strategy = loadStrategy(controler, moduleName, settings, planStrategyFactoryRegister);
	
				if (strategy == null) {
					throw new RuntimeException("Could not initialize strategy named " + moduleName);
				}
	
				manager.addStrategy(strategy, rate, Id.create(populationId,Population.class));
	
				// now check if this modules should be disabled after some iterations
				int maxIter = settings.getDisableAfter();
				// --- begin new ---
				if ( maxIter > globalInnovationDisableAfter || maxIter==-1 ) {
					boolean innovative = true ;
					for ( DefaultSelector sel : DefaultPlanStrategiesModule.DefaultSelector.values() ) {
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
						manager.addChangeRequest(maxIter + 1, strategy, 0.0, populationId);
					} else {
						/* The controler starts at a later iteration than this change request is scheduled for.
						 * make the change right now.					 */
						manager.changeWeightOfStrategy(strategy, 0.0, Id.create(populationId,Population.class));
					}
				}
			}
			String name = strategyPops.getPlanSelectorForRemoval(populationId) ;
			if ( name != null ) {
				// yyyy ``manager'' has a default setting.  I do not want to override this here except when it is configured.
				// Presumably, this is not the desired approach and the default should be in the config file?  kai, feb'12
				GenericPlanSelector<Plan, Person> planSelector = null ;
				if ( name.equals("WorstPlanSelector") ) { 
					planSelector = new WorstPlanForRemovalSelector() ; 
				} else if ( name.equals("SelectRandom") ) {
					planSelector = new RandomPlanSelector() ;
				} else if ( name.equals("SelectExpBeta") ) {
					planSelector = new ExpBetaPlanSelector( - config.planCalcScore().getBrainExpBeta() ) ;
					// yyyy this will select _good_ plans for removal--?
				} else if ( name.equals("ChangeExpBeta") ) {
					planSelector = new ExpBetaPlanChanger( - config.planCalcScore().getBrainExpBeta() ) ;
					// yyyy this will select _good_ plans for removal--?
					// yyyy might just use -beta as parameter??
				} else if ( name.equals("BestPlanSelector") ) {
					planSelector = new BestPlanSelector<Plan, Person>() ;
					// yyyy this will select _good_ plans for removal--?
				} else if ( name.equals("PathSizeLogitSelector") ) {
					planSelector = new PathSizeLogitSelector(config.planCalcScore().getPathSizeLogitBeta(), -config.planCalcScore().getBrainExpBeta(), 
							controler.getScenario().getNetwork() ) ;
					// yyyy this will select good? bad? plans for removal--?
				} else {
					throw new RuntimeException("Unknown 'plan selector for removal'.");
				}
				manager.setPlanSelectorForRemoval(planSelector, Id.create(populationId,Population.class)) ;
			}
		}
	}

	private static PlanStrategy loadStrategy(final Controler controler, final String name, final StrategySettings settings, PlanStrategyFactoryRegister planStrategyFactoryRegister) {
		// Special cases, scheduled to go away.
		if (name.equals(LOCATION_CHOICE)) {
			PlanStrategy strategy = tryToLoadPlanStrategyByName(controler, "org.matsim.contrib.locationchoice.LocationChoicePlanStrategy");
			return strategy;
		} else if (name.equals("ExternalModule")) {
			externalCounter++;
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			String exePath = settings.getExePath();
			ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controler.getControlerIO(), controler.getScenario());
			strategy.addStrategyModule(em);
			return strategy;
		} else if (name.contains(".")) {
			PlanStrategy strategy = tryToLoadPlanStrategyByName(controler, name);
			return strategy;
		} else {
			PlanStrategyFactory planStrategyFactory = planStrategyFactoryRegister.getInstance(name);
			PlanStrategy strategy = planStrategyFactory.get();
			return strategy;
		} 
	} 


	private static PlanStrategy tryToLoadPlanStrategyByName(final Controler controler, final String name) {
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
				strategy = c.newInstance(controler.getScenario());
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
