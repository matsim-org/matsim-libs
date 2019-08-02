/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingsearch;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSlotVisualiser;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff An example how to use parking search in MATSim.
 *         Technically, all you need as extra input is a facilities file
 *         containing "car interaction" locations.
 *
 *
 */

public class RunParkingSearchExample {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig("parkingsearch/config.xml", new ParkingSearchConfigGroup());
		//all further input files are set in the config.
		
		//get the parking search config group to set some parameters, like agent's search strategy or average parking slot length
		ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
		configGroup.setParkingSearchStrategy(ParkingSearchStrategy.Benenson);

        // set to false, if you don't require visualisation, then the example will run for 10 iterations, with OTFVis, only one iteration is performed.
        boolean otfvis = false;
		if (otfvis) {
			config.controler().setLastIteration(0);
		} else {
			config.controler().setLastIteration(10);
		}
		new RunParkingSearchExample().run(config,otfvis);

	}

	/**
	 * @param config
	 * 			a standard MATSim config
	 * @param otfvis
	 *            turns otfvis visualisation on or off
	 */
	public void run(Config config, boolean otfvis) {
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
				addEventHandlerBinding().toInstance(visualiser);
				addControlerListenerBinding().toInstance(visualiser);
			}
		});
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		SetupParking.installParkingModules(controler);
		controler.run();
	}

}
