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

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
* @author ikaddoura
*/

public class BerlinControler {
	
// 	run0
//	final static String configFile = "../../../public-svn/matsim/scenarios/countries/de/berlin/car-traffic-only-1pct-2014-08-01/config_be_1pct_ik.xml";
	
	final private String configFile = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/run1/input/config1.xml";
	final private String outputDirectory = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/run1/output/";
	
	final private int activityDurationHRS = 1;
	
	public static void main(String[] args) {

		BerlinControler berlin = new BerlinControler();
		berlin.run();		
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outputDirectory);
		
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
			params.setOpeningTime(6 * 3600.);
			params.setClosingTime(20 * 3600.);
			params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
			config.planCalcScore().addActivityParams(params);
		}
		
		// shop
		for (int n = 1; n <= 24 ; n = n + this.activityDurationHRS) {
			ActivityParams params = new ActivityParams("shop_" + n);
			params.setTypicalDuration(n * 3600.);
			params.setOpeningTime(8 * 3600.);
			params.setClosingTime(20 * 3600.);
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
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		
		// TODO: add some analysis
		
		controler.run();
	}

}

