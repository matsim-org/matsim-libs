/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.southAfrica;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.hook.PHook;
import playground.andreas.P2.hook.PTransitRouterFactory;
import playground.droeder.ptSubModes.PtSubModeControlerListener;
import playground.droeder.ptSubModes.routing.PtSubModeTripRouterFactory;

/**
 * @author droeder
 *
 */
public class RsaMain {

	private static final Logger log = Logger.getLogger(RsaMain.class);

	private RsaMain() {
//		do not allow to instantiate this class
	}
	
	private static String[] ARGS = {
		"../../org.matsim/examples/pt-tutorial/configExtended.xml",
		"true"
	};
	
	public static void main(String[] args) {
		String[] arguments = null;
		PConfigGroup pConfig = null;
		if(args.length == 0){
			log.warn("running testcase.");
			arguments = ARGS;
			pConfig = getTestPConfig();
		}else if(args.length == 2){
			pConfig = new PConfigGroup();
			arguments = args;
		}else{
			log.error("illegal number of arguments. Expecting [configFile, (boolean) fixedSubmode]");
			System.exit(-1);
		}
		
		String configFile = arguments[0];
		boolean fixedSubMode = Boolean.getBoolean(arguments[1]);

		log.info("Class: " + RsaMain.class.getCanonicalName());
		log.info("configFile: " + configFile);
		log.info("fixedSubMode: " + fixedSubMode);
		
		Config config = new Config();
		config.addModule(pConfig);
		ConfigUtils.loadConfig(config, configFile);
		
		PScenarioImpl scenario = new PScenarioImpl(config);
		ScenarioUtils.loadScenario(scenario);
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		
		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		// actually I'm not sure if this is still necessary \\DR, jul '13
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		PtSubModeControlerListener ptSubModeListener = new PtSubModeControlerListener(fixedSubMode);
		PHook pHook = new PHook(controler, new Mode2LineSetterRSA(), ptSubModeListener.getTransitRouterFactory(), null, PtSubModeTripRouterFactory.class);
		controler.addControlerListener(pHook);	
		controler.addControlerListener(new PtSubModeControlerListener(fixedSubMode));
		
		controler.run();
		
	}

	/**
	 * @return
	 */
	private static PConfigGroup getTestPConfig() {
		PConfigGroup pConfig = new PConfigGroup();
		pConfig.addParam("gridSize", "499.0");
		pConfig.addParam("useFranchise", "false");
		pConfig.addParam("timeSlotSize", "600.0");
		return pConfig;
	}
}

