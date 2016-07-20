/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.decongestion;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.TollingApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;

/**
 * 
 * Tests a router which accounts for the agent's real VTTS.
 * The VTTS is computed by {@link VTTSHandler}. 
 * 
 * @author ikaddoura
 *
 */

public class DecongestionPricingTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * There are two agents. One agent is more pressed for time than the other one which results in different VTTS.
	 * There are two routes. One expensive but fast route. One cheap but slow route.
	 * 
	 * Accounting for the different VTTS results in a different outcome than assuming the default VTTS (as it is the case in the default router).
	 * 
	 */
	@Test
	public final void test1() {
		
		final String configFile = testUtils.getPackageInputDirectory() + "bottleneck/configVTTS.xml";
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = outputBaseDirectory +
				"total" + config.controler().getLastIteration() + "it" + 
				"_timeBinSize" + config.travelTimeCalculator().getTraveltimeBinSize() +
				"_BrainExpBeta" + config.planCalcScore().getBrainExpBeta() +
				"_timeMutation" + config.timeAllocationMutator().getMutationRange() +
				"_" + decongestionSettings.getTOLLING_APPROACH();
		
		if (decongestionSettings.getTOLLING_APPROACH().toString().equals(TollingApproach.NoPricing.toString())) {
			// no relevant parameters
		
		} else {
			
			outputDirectory = outputDirectory
					+ "_priceUpdate" + decongestionSettings.getUPDATE_PRICE_INTERVAL() + "_it"
					+ "_toleratedDelay" + decongestionSettings.getTOLERATED_AVERAGE_DELAY_SEC()
					+ "_start" + decongestionSettings.getFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT()
					+ "_end" + decongestionSettings.getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT();
			
			if (decongestionSettings.getTOLLING_APPROACH().toString().equals(TollingApproach.BangBang.toString())) {			
				outputDirectory = outputDirectory + 
						"_init" + decongestionSettings.getINITIAL_TOLL() +
						"_adj" + decongestionSettings.getTOLL_ADJUSTMENT();
			
			} else if (decongestionSettings.getTOLLING_APPROACH().toString().equals(TollingApproach.PID.toString())) {
				outputDirectory = outputDirectory +
						"_Kp" + decongestionSettings.getKp() +
						"_Ki" + decongestionSettings.getKi() +
						"_Kd" + decongestionSettings.getKd();
			}
		}
			
		log.info("Output directory: " + outputDirectory);
		
		config.controler().setOutputDirectory(outputDirectory + "/");
		final Scenario scenario = ScenarioUtils.loadScenario(config);
				
		final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
		final Decongestion decongestion = new Decongestion(info);
		
		final Controler controler = decongestion.getControler();
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
        controler.run();   
		
	}
	
}
