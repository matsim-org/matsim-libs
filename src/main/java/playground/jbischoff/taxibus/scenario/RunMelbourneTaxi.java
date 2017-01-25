/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.taxibus.run.configuration.ConfigBasedTaxibusLaunchUtils;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;
import playground.jbischoff.taxibus.sharedtaxi.analysis.SharedTaxiConfigGroup;
import playground.jbischoff.taxibus.sharedtaxi.analysis.SharedTaxiContolerListener;
import playground.jbischoff.taxibus.sharedtaxi.analysis.SharedTaxiTripAnalyzer;

/**
 * @author jbischoff
 *
 */
public class RunMelbourneTaxi {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig(args[0], new TaxibusConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
	

		SharedTaxiConfigGroup stt = new SharedTaxiConfigGroup();
		stt.setDiscountForSharing(0.4);
		stt.setHourlyTaxiFare(10.0);
		stt.setScoreRides(true);
		config.addModule(stt);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		new ConfigBasedTaxibusLaunchUtils(controler).initiateTaxibusses();
		
		
		//Analysis code:
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
			bind(SharedTaxiTripAnalyzer.class).asEagerSingleton();
			addControlerListenerBinding().to(SharedTaxiContolerListener.class);
			}
		});
		controler.run();
	}
}
