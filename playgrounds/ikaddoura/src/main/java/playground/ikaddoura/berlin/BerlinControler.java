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

package playground.ikaddoura.berlin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.controler.AbstractModule;
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
	private static int activityDurationHRS;
	private static int hrs;
	private static boolean pricing;
	
	public static void main(String[] args) throws IOException {

		if (args.length > 0) {
		
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			addModifiedActivities = Boolean.parseBoolean(args[1]);
			log.info("addModifiedActivities: "+ addModifiedActivities);

			activityDurationHRS = Integer.parseInt(args[2]);
			log.info("activityDurationHRS: "+ activityDurationHRS);
			
			hrs = Integer.parseInt(args[3]);
			log.info("hrs: "+ hrs);

			pricing = Boolean.parseBoolean(args[4]);			
			log.info("pricing: "+ pricing);

		} else {
			
//			configFile = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/baseCase/input/config_detailed-network.xml";
			configFile = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/baseCase/input/config_test.xml";
			
			addModifiedActivities = true;
			activityDurationHRS = 1;
			hrs = 25;
			
			pricing = false;
			
		}
		
		BerlinControler berlin = new BerlinControler();
		berlin.run();
	}

	private void run() throws IOException {
		
		Config config = ConfigUtils.loadConfig(configFile);
		final String outputDirectory = config.controler().getOutputDirectory();
				
		if (addModifiedActivities) {
			
			List<ActivityParams> newActivityParams = new ArrayList<>();

			for (ActivityParams actParams : config.planCalcScore().getActivityParams()) {
				String activityType = actParams.getActivityType();
				
				if (activityType.contains("interaction")) {
					log.info("Skipping activity " + activityType + "...");
					
				} else {
					
					log.info("Splitting activity " + activityType + " in duration-specific activities.");

					for (int n = 1; n <= hrs ; n = n + activityDurationHRS) {
						ActivityParams params = new ActivityParams(activityType + "_" + n);
						params.setTypicalDuration(n * 3600.);
						params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
						newActivityParams.add(params);
					}
				}
			}
			
			// add new activity parameters to config
			
			for (ActivityParams actParams : newActivityParams) {
				config.planCalcScore().addActivityParams(actParams);
			}				
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
		if (pricing) {
			CNEIntegration cne = new CNEIntegration(controler);
			cne.setCongestionPricing(true);
			controler = cne.prepareControler();
		}
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		
		// adjusted scoring
		if (addModifiedActivities) {
			controler.addOverridingModule( new AbstractModule() {
				
				@Override
				public void install() {
					
					CountActEventHandler actCount = new CountActEventHandler();
									
					this.addEventHandlerBinding().toInstance(actCount);
					this.bindScoringFunctionFactory().toInstance(new IKScoringFunctionFactory(scenario, actCount));
					// TODO: see if there are nicer ways to plug this together...
				}
			});
		}
			
		controler.run();
		
		// analysis
		
		PersonTripCongestionNoiseAnalysisMain analysis = new PersonTripCongestionNoiseAnalysisMain(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		MATSimVideoUtils.createLegHistogramVideo(outputDirectory);
	}

}

