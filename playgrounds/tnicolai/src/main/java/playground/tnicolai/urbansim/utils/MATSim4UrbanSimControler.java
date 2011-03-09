/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.tnicolai.urbansim.utils;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimControler extends Controler {
	
	public MATSim4UrbanSimControler(ScenarioImpl scenarioData){
		super(scenarioData);
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.Controler#loadStrategyManager()
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		
		PlanStrategy changeExpBeta = new PlanStrategyImpl(new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()));
		manager.addStrategy(changeExpBeta, 0.8);
	
//		PlanStrategy reroute = new PlanStrategyImpl(new RandomPlanSelector());
//		reroute.addStrategyModule(new ReRoute(this));
//		manager.addStrategy(reroute, 0.1);
//		manager.addChangeRequest(101, reroute, 0.);
		
		PlanStrategy timeAllocationMutator = new PlanStrategyImpl(new RandomPlanSelector());
		TimeAllocationMutator tam = new TimeAllocationMutator(config);
		timeAllocationMutator.addStrategyModule(tam);
		manager.addStrategy(timeAllocationMutator, 0.1);
		manager.addChangeRequest(101, timeAllocationMutator, 0.);
		
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

}
