/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.dziemke.cadyts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.car.CadytsPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;

public class CadytsController {
	// new
	private final static Logger log = Logger.getLogger(CadytsController.class);
	
	// private final static boolean USE_BRUTE_FORCE = true;
	//

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]) ;
		
		// changed id to 3
		// before run_34_c: change id to 2
		StrategySettings stratSets = new StrategySettings(new IdImpl(2));
		stratSets.setModuleName("ccc");
		// before run_34_c: change probability from 1.0 to 0.9; set back before run_36_c
		stratSets.setProbability(1.0);
		config.strategy().addStrategySettings(stratSets);
		
		Controler controler = new Controler(config);
		
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		// 2013-04-23, switch on brute force
		// CadytsCarConfigGroup cadytsCarConfigGroup = new CadytsCarConfigGroup();
		// cadytsCarConfigGroup.setUseBruteForce(USE_BRUTE_FORCE);
		//
		controler.addControlerListener(cContext);
		
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(cContext);

				planSelector.setCadytsWeight(30.*scenario2.getConfig().planCalcScore().getBrainExpBeta() ) ;
				// set cadyts weight very high = close to brute force
				
				return new PlanStrategyImpl(planSelector);
			}
		});
		
		
		// 2013-04-22, switching off scoring (as in BseUCControler)
//			ZeroScoringFunctionFactory zeroScoringFunctionFactory = new ZeroScoringFunctionFactory();
//			controler.setScoringFunctionFactory(zeroScoringFunctionFactory);
		//
		
		
//		controler.addControlerListener(new KaiAnalysisListener());
		
		controler.run();
	}
}
