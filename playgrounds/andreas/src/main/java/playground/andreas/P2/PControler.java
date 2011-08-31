/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.andreas.P2;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.PtConstants;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.schedule.PTransitRouterImplFactory;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionConfigGroup;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionFactory;


/**
 * Entry point, registers all necessary hooks
 * 
 * @author aneumann
 */
public class PControler{

	private final static Logger log = Logger.getLogger(PControler.class);

	public static void main(final String[] args) {
		
		if(args.length == 0){
			log.info("Arg 1: config.xml");
			System.exit(1);
		}
		
		Config config = new Config();
		config.addModule(PConfigGroup.GROUP_NAME, new PConfigGroup());
		ConfigUtils.loadConfig(config, args[0]);
		
		PScenarioImpl scenario = new PScenarioImpl(config);
		ScenarioUtils.loadScenario(scenario);
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		
		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		PTransitRouterImplFactory pFact = new PTransitRouterImplFactory(controler);
		controler.addControlerListener(pFact);		
		controler.setTransitRouterFactory(pFact);
		controler.setScoringFunctionFactory(new BvgScoringFunctionFactory(controler.getConfig().planCalcScore(), new BvgScoringFunctionConfigGroup(controler.getConfig()), controler.getNetwork()));

		controler.run();
	}		
}