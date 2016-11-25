/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.utils.prepare.PopulationTools;

/**
* @author ikaddoura
*/

public class PopulationActivitySplitter {
	
	private static final Logger log = Logger.getLogger(PopulationActivitySplitter.class);

	// input
	final private String inputPopulatlionFile = "../../../public-svn/matsim/scenarios/countries/de/berlin/car-traffic-only-1pct-2014-08-01/run_160.150.plans_selected.xml.gz";

//	final private String inputPopulatlionFile = "../../../runs-svn/bln-time/input/bvg.run189.10pct.100.plans.selected.genericPt.xml.gz";
//	final private String inputPopulatlionFile = "../../../runs-svn/congestion-pricing/output/NoPricing/output_plans.xml.gz";
	
	// output
	final private String outputDirectory = "../../../runs-svn/berlin-dz-time/input/";
	final private String outputPopulationFile = "run_160.150.plans_selected_splitActivityTypes.xml.gz";

//	final private String outputDirectory = "../../../runs-svn/bln-an-time/input/";
//	final private String outputPopulationFile = "baseCase_output_plans_it.100.selected.genericPt.splitActivityTypes.xml.gz";	

	// settings
	final private double timeCategorySize = 3600.;
		
	public static void main(String[] args) {
		PopulationActivitySplitter b = new PopulationActivitySplitter();
		b.run();	
	}

	private void run() {
				
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		log.info("Input Population: " + inputPopulatlionFile);

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPopulatlionFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
				
		PopulationTools.setActivityTypesAccordingToDurationAndMergeOvernightActivities(scenario.getPopulation(), timeCategorySize);			
		PopulationTools.addActivityTimesOfSelectedPlanToPersonAttributes(scenario.getPopulation());
		PopulationTools.setScoresToZero(scenario.getPopulation());
		PopulationTools.analyze(scenario.getPopulation());
		PopulationTools.removeNetworkSpecificInformation(scenario.getPopulation());
		
		PopulationWriter pw = new PopulationWriter(scenario.getPopulation());
		pw.write(outputDirectory + outputPopulationFile);		
	}
}

