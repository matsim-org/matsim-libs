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

package playground.anhorni.surprice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.anhorni.surprice.analysis.Analyzer;

public class MultiDayControler {
	
	private final static Logger log = Logger.getLogger(MultiDayControler.class);
				
	public static void main (final String[] args) {		
		if (args.length != 2) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		String configFile = args[0];
		Config config = ConfigUtils.loadConfig(configFile);
		String incomesFile = args[1];
		String path = config.plans().getInputFile();
		String outPath = config.controler().getOutputDirectory();
						
		ObjectAttributes preferences = new ObjectAttributes();
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(preferences);
		preferencesReader.parse(path + "/preferences.xml");
		
		Population populationPreviousDay = null;
		int finalIterations[] = new int[7];		
		for (String day : Surprice.days) {			
			config.setParam("controler", "outputDirectory", outPath + "/" + day);
			config.setParam("plans", "inputPlansFile", path + "/" + day + "/plans.xml");
			config.setParam("controler", "runId", day);
			
			AgentMemories memories = new AgentMemories();
			
			DayControler controler = new DayControler(config, memories, day, preferences, populationPreviousDay);
			controler.run();
			
			if (day.equals("sat")) {
				populationPreviousDay = null;
			}
			else {
				populationPreviousDay = controler.getPopulation();
			}
			finalIterations[Surprice.days.indexOf(day)] = controler.getFinalIteration();
		}
		// only used for small-scale scenario
//		UtilityAnalyzer analyzer = new UtilityAnalyzer();
//		Config configCreate = ConfigUtils.loadConfig("C:/l/studies/surprice/configCreateSC.xml");
//		double sideLength = Double.parseDouble(configCreate.findParam(Surprice.SURPRICE_PREPROCESS, "sideLength"));
//		analyzer.analyze(config, outPath, sideLength);
		
		Analyzer analyzer = new Analyzer();
		analyzer.setFinalIterations(finalIterations);
		analyzer.init(configFile, incomesFile);
		analyzer.run();
		
		log.info("Week simulated, yep, .................................................................");
    }
}
