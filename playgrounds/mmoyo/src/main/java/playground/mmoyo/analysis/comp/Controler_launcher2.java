/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptedControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.analysis.comp;

import org.matsim.api.core.v01.Scenario;
//import org.matsim.contrib.cadyts.pt.CadytsPtPlanStrategy;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;

import playground.mmoyo.utils.calibration.NullifyingScoringFunctionFactory;

public class Controler_launcher2 {
	
	public static void main(String[] args) {
		String configFile; 
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../../ptManuel/calibration/my_config.xml";
		}

		Config config = null;
		config = ConfigUtils.loadConfig(configFile);
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		
		StrategySettings stratSets = new StrategySettings(new IdImpl(lastStrategyIdx+1));
		stratSets.setModuleName("myCadyts");
		stratSets.setProbability(1.0);
		config.strategy().addStrategySettings(stratSets);
		
		
		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		

//		 controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory()
//		    {
//		      public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
//		        CadytsPtPlanStrategy cadytsPlanStrategy = new CadytsPtPlanStrategy(scenario, eventsManager);
//		        controler.addControlerListener(cadytsPlanStrategy);
//		        return cadytsPlanStrategy;
//		      }
//		    });
		
		
		//add nullifying scoring for effective brute force
		if (Boolean.parseBoolean(config.getParam("cadytsPt", "useBruteForce"))){
			controler.setScoringFunctionFactory(new NullifyingScoringFunctionFactory());
		}
		
		controler.run();
	} 
}
