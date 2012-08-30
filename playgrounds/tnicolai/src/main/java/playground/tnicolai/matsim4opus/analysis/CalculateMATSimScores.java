/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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
package playground.tnicolai.matsim4opus.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.kuhmo.UserWelfareCalculator;

/**
 * @author thomas
 *
 */
public class CalculateMATSimScores {
	
	private static final Logger logger = Logger.getLogger(CalculateMATSimScores.class);
	
	public static void main(String args[]){
		
		try {
			String configFile = args[0];
			String plansFile = args[1];
			String outputDir = args[2];
				
			String outputFileName = outputDir + "/matsim_score.txt";
			BufferedWriter bw = IOUtils.getBufferedWriter(outputFileName);
			
			Config config = new Config();
			config.addCoreModules();
			new MatsimConfigReader(config).readFile(configFile);
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
			PlansConfigGroup plansCG = (PlansConfigGroup) scenario.getConfig().getModule(PlansConfigGroup.GROUP_NAME);
			// set input plans file
			plansCG.setInputFile( plansFile );
			scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();
			Population pop = scenario.getPopulation();
			
			
			UserWelfareCalculator userWelfareCalculator = new UserWelfareCalculator(configFile);
			
			double matsimScore = userWelfareCalculator.calculateLogsum(pop);
			// users with no valid plan
			int personWithNoValidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
			logger.warn("Users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + personWithNoValidPlanCnt);
		
			bw.write("matsim_score,invalid_plans");
			bw.newLine();
			bw.write(String.valueOf(matsimScore) + "," + String.valueOf(personWithNoValidPlanCnt));
			bw.newLine();
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Done!");
	}

}
