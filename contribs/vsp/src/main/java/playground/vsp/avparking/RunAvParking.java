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
package playground.vsp.avparking;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.avparking.optimizer.PrivateAVOptimizerProvider;
import playground.vsp.avparking.optimizer.PrivateAVTaxiDispatcher.AVParkBehavior;

/**
 * @author jbischoff An example how to use parking search in MATSim.
 *         Technically, all you need as extra input is a facilities file
 *         containing "car interaction" locations.
 *
 *
 */

public class RunAvParking {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig(args[0], new DvrpConfigGroup(), new MultiModeTaxiConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		AVParkBehavior b;
		switch (args[1]) {
			case "0":
				b = AVParkBehavior.findfreeSlot;
				break;
			case "1":
				b = AVParkBehavior.garage;
				break;
			case "2":
				b = AVParkBehavior.cruise;
				break;
			case "3":
				b = AVParkBehavior.randombehavior;
				break;
			default:
				throw new RuntimeException();

		}
		new RunAvParking().run(config, b);

	}

	/**
	 * @param config
	 * 			a standard MATSim config
	 *
	 */
	public void run(Config config, AVParkBehavior b) {
		config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "/" + b.toString());
		config.qsim().setStartTime(0);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
		config.global().setNumberOfThreads(8);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		PrivateAVFleetGenerator fleet = new PrivateAVFleetGenerator(scenario);
		List<Id<Link>> avParkings = new ArrayList<>();
		avParkings.add(Id.createLinkId(35464));
		AvParkingContext context = new AvParkingContext(avParkings, b);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Fleet.class).toInstance(fleet);
				bind(AvParkingContext.class).toInstance(context);
				addControlerListenerBinding().toInstance(fleet);
			}
		});
		controler.addOverridingModule(new ParkingTaxiModule(PrivateAVOptimizerProvider.class));
		controler.addOverridingModule(new MultiModeTaxiModule());
		controler.run();
	}

}
