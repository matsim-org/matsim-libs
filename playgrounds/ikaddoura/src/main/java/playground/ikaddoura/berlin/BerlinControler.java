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
	
//	final static String configFile = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/baseCase/input/config_detailed-network.xml";
	final static String configFile = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/baseCase/input/config_test.xml";
	
	final private int activityDurationHRS = 1;
	final private boolean pricing = false;
	
	public static void main(String[] args) throws IOException {

		BerlinControler berlin = new BerlinControler();
		berlin.run();		
	}

	private void run() throws IOException {
		
		Config config = ConfigUtils.loadConfig(configFile);
		final String outputDirectory = config.controler().getOutputDirectory();
		
		// home
		for (int n = 1; n <= 24 ; n = n + this.activityDurationHRS) {
			ActivityParams params = new ActivityParams("home_" + n);
			params.setTypicalDuration(n * 3600.);
			params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
			config.planCalcScore().addActivityParams(params);
		}
		
		// work
		for (int n = 1; n <= 24 ; n = n + this.activityDurationHRS) {
			ActivityParams params = new ActivityParams("work_" + n);
			params.setTypicalDuration(n * 3600.);
			params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
			config.planCalcScore().addActivityParams(params);
		}
		
		// shop
		for (int n = 1; n <= 24 ; n = n + this.activityDurationHRS) {
			ActivityParams params = new ActivityParams("shop_" + n);
			params.setTypicalDuration(n * 3600.);
			params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
			config.planCalcScore().addActivityParams(params);
		}
		
		// leis
		for (int n = 1; n <= 24 ; n = n + this.activityDurationHRS) {
			ActivityParams params = new ActivityParams("leis_" + n);
			params.setTypicalDuration(n * 3600.);
			params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
			config.planCalcScore().addActivityParams(params);
		}
		
		// other
		for (int n = 1; n <= 24 ; n = n + this.activityDurationHRS) {
			ActivityParams params = new ActivityParams("other_" + n);
			params.setTypicalDuration(n * 3600.);
			params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
			config.planCalcScore().addActivityParams(params);
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
		if (pricing) {
			CNEIntegration cne = new CNEIntegration(controler);
			cne.setCongestionPricing(true);
			controler = cne.prepareControler();
		}
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		
		// adjusted scoring
		controler.addOverridingModule( new AbstractModule() {
			
			@Override
			public void install() {
				
				CountActEventHandler actCount = new CountActEventHandler();
								
				this.addEventHandlerBinding().toInstance(actCount);
				this.bindScoringFunctionFactory().toInstance(new IKScoringFunctionFactory(scenario, actCount));
				// TODO: see if there are nicer ways to plug this together...

			}
		});
			
		controler.run();
		
		// analysis
		
		PersonTripCongestionNoiseAnalysisMain analysis = new PersonTripCongestionNoiseAnalysisMain(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		MATSimVideoUtils.createLegHistogramVideo(outputDirectory);
	}

}

