/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
package playground.southafrica.population.freight;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jwjoubert
 *
 */
public class RunNationalFreight {
	private final static Logger LOG = Logger.getLogger(RunNationalFreight.class);
	
	private final static String NETWORK = "/Users/jwjoubert/Documents/workspace/Data-southAfrica/network/southAfrica_20131202_coarseNetwork_clean.xml.gz";
	private final static String POPULATION = "/Users/jwjoubert/Documents/Hobbes/data/digicore/2009/clustering/5_5/freight_national_5000.xml";
	private final static String OUTPUT_DIRECTORY = "/Users/jwjoubert/Documents/Temp/freightPopulation";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/* Config stuff */
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(OUTPUT_DIRECTORY);
		config.controler().setLastIteration(1);
		config.controler().setWriteEventsInterval(1);
		
		config.network().setInputFile(NETWORK);
		config.plans().setInputFile(POPULATION);
		
		config.global().setCoordinateSystem("WGS84_SA_Albers");
		
		config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		
			/* PlanCalcScore */
		ActivityParams major = new ActivityParams("major");
		major.setTypicalDuration(10*3600);
		config.planCalcScore().addActivityParams(major);

		ActivityParams minor = new ActivityParams("minor");
		minor.setTypicalDuration(1880);
		config.planCalcScore().addActivityParams(minor);
		
			/* Strategy */
//		StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		StrategySettings changeExpBetaStrategySettings = new StrategySettings(new IdImpl("1"));
		changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
		changeExpBetaStrategySettings.setProbability(0.8);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		
		/* Scenario stuff */
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		/* Run the controler */
		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
