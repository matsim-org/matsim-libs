/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * BerlinRunUncongested2.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.cdr;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class BerlinRunUncongested3 implements Runnable {
	
	final static String BERLIN_PATH = "/Users/michaelzilske/shared-svn/studies/countries/de/berlin/";
	
	public static void main(String[] args) {
		BerlinRunUncongested3 berlinRun = new BerlinRunUncongested3();
		berlinRun.run();
	}
	
	@Override
	public void run() {
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).parse(this.getClass().getResourceAsStream("2kW.15.xml"));
		config.plans().setInputFile(BERLIN_PATH + "plans/baseplan_car_only.xml.gz");
		config.network().setInputFile(BERLIN_PATH + "counts/iv_counts/network.xml.gz");
		config.counts().setCountsFileName(BERLIN_PATH + "counts/iv_counts/vmz_di-do.xml");
        config.controler().setOutputDirectory("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested3/output-berlin");
        config.counts().setOutputFormat("kml");
        config.counts().setWriteCountsInterval(1);
        config.counts().setAverageCountsOverIterations(1);
        config.controler().setMobsim(MobsimType.qsim.toString());
        config.controler().setLastIteration(0);
		config.qsim().setFlowCapFactor(100);
		config.qsim().setStorageCapFactor(100);
		config.qsim().setRemoveStuckVehicles(false);
		config.planCalcScore().setWriteExperiencedPlans(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		

		
		final Controler controller = new Controler(scenario);
		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controller.run();
		

	}
}