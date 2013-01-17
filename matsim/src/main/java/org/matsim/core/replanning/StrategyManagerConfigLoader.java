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
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyFactoryRegister;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
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

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);

	private static int externalCounter = 0;

	public static void load(final Controler controler, final StrategyManager manager) {
		PlanStrategyRegistrar planStrategyFactoryRegistrar = new PlanStrategyRegistrar();
		PlanStrategyFactoryRegister planStrategyFactoryRegister = planStrategyFactoryRegistrar.getFactoryRegister();
		load(controler, manager, planStrategyFactoryRegister);
	}

	public static void load(final Controler controler, final StrategyManager manager, PlanStrategyFactoryRegister planStrategyFactoryRegister) {
		Config config = controler.getConfig();
		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}

			PlanStrategy strategy = loadStrategy(controler, classname, settings, planStrategyFactoryRegister);

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			if (settings.getDisableAfter() >= 0) {
				int maxIter = settings.getDisableAfter();
				if (maxIter >= config.controler().getFirstIteration()) {
					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
				} else {
					/* The controler starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					manager.changeWeightOfStrategy(strategy, 0.0);
				}
			}
		}
		String name = config.strategy().getPlanSelectorForRemoval() ;
		if ( name != null ) {
			// yyyy ``manager'' has a default setting.  I do not want to override this here except when it is configured.
			// Presumably, this is not the desired approach and the default should be in the config file?  kai, feb'12
			PlanSelector planSelector = null ;
			if ( name.equals("WorstPlanSelector") ) { 
				planSelector = new WorstPlanForRemovalSelector() ; 
			} else if ( name.equals("SelectRandom") ) {
				planSelector = new RandomPlanSelector() ;
			} else if ( name.equals("SelectExpBeta") ) {
				planSelector = new ExpBetaPlanSelector(config.planCalcScore() ) ;
				// yyyy this will select _good_ plans for removal--?
			} else if ( name.equals("ChangeExpBeta") ) {
				planSelector = new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()) ;
				// yyyy this will select _good_ plans for removal--?
				// yyyy might just use -beta as parameter??
			} else if ( name.equals("BestPlanSelector") ) {
				planSelector = new BestPlanSelector() ;
				// yyyy this will select _good_ plans for removal--?
			} else if ( name.equals("PathSizeLogitSelector") ) {
				planSelector = new PathSizeLogitSelector(config.planCalcScore(), controler.getScenario().getNetwork() ) ;
				// yyyy this will select good? bad? plans for removal--?
			} else {
				planSelector = tryToLoadPlanSelectorByName(controler, name);
			}
			manager.setPlanSelectorForRemoval(planSelector) ;
		}
	}

	private static PlanStrategy loadStrategy(final Controler controler, final String name, final StrategyConfigGroup.StrategySettings settings, PlanStrategyFactoryRegister planStrategyFactoryRegister) {
		// Special cases, scheduled to go away.
		if (name.equals("LocationChoice")) {
			PlanStrategy strategy = tryToLoadPlanStrategyByName(controler, "org.matsim.contrib.locationchoice.LocationChoicePlanStrategy");
			return strategy;
		} else if (name.equals("ExternalModule")) {
			externalCounter++;
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			String exePath = settings.getExePath();
			ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controler, controler.getScenario());
			strategy.addStrategyModule(em);
			return strategy;
		} else if (name.contains(".")) {
			PlanStrategy strategy = tryToLoadPlanStrategyByName(controler, name);
			return strategy;
		} else {
			PlanStrategyFactory planStrategyFactory = planStrategyFactoryRegister.getInstance(name);
			PlanStrategy strategy = planStrategyFactory.createPlanStrategy(controler.getScenario(), controler.getEvents());
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
				try{
					c = klas.getConstructor(args);
					strategy = c.newInstance(controler.getScenario());
				} catch(NoSuchMethodException e){
					log.info("Cannot find Constructor in PlanStrategy " + name + " with single argument of type Scenario. " +
							"This is not fatal, trying to find other constructor, however a constructor expecting Scenario as " +
							"single argument is recommended!" );
					log.info("(People who need access to events should ignore this warning.)") ;
					// I think that one needs events fairly often. kai, sep'10
					args[0] = Controler.class;
					c = klas.getConstructor(args);
					strategy = c.newInstance(controler);
					log.info("Loaded PlanStrategy (known as `module' in the config) from class " + name);
				}

			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		return strategy;
	}

	private static PlanSelector tryToLoadPlanSelectorByName(final Controler controler, final String name) {
		PlanSelector strategy = null;
		//classes loaded by name must not be part of the matsim core
		if (name.startsWith("org.matsim.") && !name.startsWith("org.matsim.contrib.")) {
			log.error("PlanSelectors in the org.matsim package must not be loaded by name!");
		} else {
			try {
				Class<? extends PlanSelector> klas = (Class<? extends PlanSelector>) Class.forName(name);
				Class<?>[] args = new Class[1];
				args[0] = Scenario.class;
				Constructor<? extends PlanSelector> c = null;
				try{
					c = klas.getConstructor(args);
					strategy = c.newInstance(controler.getScenario());
				} catch(NoSuchMethodException e){
					log.info("Cannot find Constructor in PlanSelector " + name + " with single argument of type Scenario. " +
							"This is not fatal, trying to find other constructor ...\n" ) ;
				}
				if ( c == null ) {
					try{
						c = klas.getConstructor(args);
						strategy = c.newInstance(controler.getScenario(),controler.getEvents()); 
					} catch(NoSuchMethodException e){
						log.info("Cannot find Constructor in PlanSelector " + name + " with argument of type (Scenario, EventsManager). " +
								"This is not fatal, trying to find other constructor ...\n" ) ;
					}
				}
				if (c == null){
					args[0] = Controler.class;
					c = klas.getConstructor(args);
					strategy = c.newInstance(controler);
				}
				log.info("Loaded PlanStrategy (known as `module' in the config) from class " + name);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		return strategy;
	}

}
