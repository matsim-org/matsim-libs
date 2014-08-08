/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * BerlinRunCongested.java
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

package playground.mzilske.sensors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class BerlinRunCongested implements Runnable {
	
	final static String BERLIN_PATH = "/Users/michaelzilske/shared-svn/studies/countries/de/berlin/";
	
	public static void main(String[] args) {
		BerlinRunCongested berlinRun = new BerlinRunCongested();
		berlinRun.run();
	}
	
	@Override
	public void run() {
        Scenario scenario = getScenario();
		

		
		final Controler controller = new Controler(scenario);
		controller.run();
		

	}

    static Scenario getScenario() {
        Config config = ConfigUtils.loadConfig("/Users/michaelzilske/runs-svn/synthetic-cdr/bluetooth/config.xml");
        config.plans().setInputFile(BERLIN_PATH + "plans/baseplan_car_only.xml.gz");  // 18377 persons
        config.network().setInputFile(BERLIN_PATH + "network/bb_5_v_scaled_simple.xml.gz"); // only till secondary roads (4), dumped from OSM 20090603, contains 35336 nodes and 61920 links
        config.counts().setCountsFileName(BERLIN_PATH + "counts/counts4bb_5_v_notscaled_simple.xml");
        config.controler().setOutputDirectory("/Users/michaelzilske/runs-svn/synthetic-cdr/bluetooth/output-berlin");
        config.controler().setMobsim(MobsimType.qsim.toString());

        return ScenarioUtils.loadScenario(config);
    }
}