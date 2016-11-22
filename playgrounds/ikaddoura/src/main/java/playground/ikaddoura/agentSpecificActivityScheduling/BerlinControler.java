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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripCongestionNoiseAnalysisMain;
import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.ikaddoura.integrationCNE.CNEIntegration;

/**
* @author ikaddoura
*/

public class BerlinControler {
	
	private static final Logger log = Logger.getLogger(BerlinControler.class);
	
	private static String configFile;
	private static boolean addModifiedActivities;
	private static double activityDurationBin;
	private static double tolerance;
	private static boolean pricing;
	
	public static void main(String[] args) throws IOException {

		if (args.length > 0) {
		
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			addModifiedActivities = Boolean.parseBoolean(args[1]);
			log.info("addModifiedActivities: "+ addModifiedActivities);

			activityDurationBin = Double.parseDouble(args[2]);
			log.info("activityDurationBin: "+ activityDurationBin);
			
			tolerance = Double.parseDouble(args[3]);
			log.info("tolerance: "+ tolerance);

			pricing = Boolean.parseBoolean(args[4]);			
			log.info("pricing: "+ pricing);

		} else {
			
			configFile = "../../../runs-svn/berlin-dz-time/input/config_test.xml";
//			configFile = "../../../runs-svn/berlin-an-time/input/config_test.xml";
			
			addModifiedActivities = true;
			activityDurationBin = 3600.;
			tolerance = 900.;
			
			pricing = false;
			
		}
		
		BerlinControler berlin = new BerlinControler();
		berlin.run();
	}

	private void run() throws IOException {
		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
		if (addModifiedActivities) {		
			AgentSpecificActivityScheduling aa = new AgentSpecificActivityScheduling(controler);
			aa.setActivityDurationBin(activityDurationBin);
			aa.setTolerance(tolerance);
			controler = aa.prepareControler();			
		}
				
		if (pricing) {
			CNEIntegration cne = new CNEIntegration(controler);
			cne.setCongestionPricing(true);
			controler = cne.prepareControler();
		}
			
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// analysis
		
		PersonTripCongestionNoiseAnalysisMain analysis = new PersonTripCongestionNoiseAnalysisMain(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		MATSimVideoUtils.createLegHistogramVideo(controler.getConfig().controler().getOutputDirectory());
	}

}

