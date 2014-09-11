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


/**
 * @author droeder
 *
 */
class RsaMain {

//	private static final Logger log = Logger.getLogger(RsaMain.class);
//
//	private RsaMain() {
////		do not allow to instantiate this class
//	}
//	
//	private static String[] ARGS = {
//		"E:/VSP/svn/droeder/southAfrica/configNew.xml",
//		"true"
//	};
//	
//	public static void main(String[] args) {
//		OutputDirectoryLogging.catchLogEntries();
//		String[] arguments = null;
//		PConfigGroup pConfig = null;
//		boolean overwrite = false;
//		
//		if(args.length == 0){
//			// prepare the local testcase
//			log.warn("running testcase. Overwriting Files");
//			arguments = ARGS;
////			pConfig = createPconfigForTest();
//			overwrite = true;
//		}else{
//			// or run a ''real'' sim from command-line
//			arguments = args;
////			pConfig = new PConfigGroup();
//			overwrite =  false;
//		}
//		
//		// check number of arguments
//		if(!(arguments.length == 2)){
//			log.error("illegal number of arguments. Expecting [configFile, (boolean) fixedSubmode]");
//			System.exit(-1);
//		}
//		
//		String configFile = arguments[0];
//		boolean fixedSubMode = Boolean.parseBoolean(arguments[1]);
//
//		log.info("Class\t\t: " + RsaMain.class.getCanonicalName());
//		log.info("configFile\t: " + configFile);
//		log.info("fixedSubMode\t: " + fixedSubMode);
//		
//		Config config = new Config();
//		pConfig = new PConfigGroup();
//		config.addModule(pConfig);
//		ConfigUtils.loadConfig(config, configFile);
//
//        Scenario scenario = ScenarioUtils.createScenario(config);
//		ScenarioUtils.loadScenario(scenario);
//		
//		Controler controler = new Controler(scenario);
//		// overwrite only the testCase
//		controler.setOverwriteFiles(overwrite);
//		controler.setCreateGraphs(true);
//		
//		// manipulate config
//		// add "pt interaction" cause controler.init() is called too late and in a protected way
//		// actually I'm not sure if this is still necessary \\DR, jul '13
//		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
//		transitActivityParams.setTypicalDuration(120.0);
//		scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
//		
//		PtSubModeControlerListener ptSubModeListener = new PtSubModeControlerListener(fixedSubMode);
//		PHook pHook = new PHook(controler, new Mode2LineSetterRSA(), ptSubModeListener.getTransitRouterFactory(), null, PtSubModeTripRouterFactory.class);
//		controler.addControlerListener(pHook);	
//		controler.addControlerListener(ptSubModeListener);
//		
//		controler.run();
//	}

//	/**
//	 * @return
//	 */
//	private static PConfigGroup createPconfigForTest() {
//		PConfigGroup pConfig = new PConfigGroup();
//		pConfig.addParam("gridSize", "499.0");
//		pConfig.addParam("useFranchise", "false");
//		pConfig.addParam("timeSlotSize", "600.0");
//		pConfig.addParam("OperationMode", "taxi");
//		return pConfig;
//	}
}

