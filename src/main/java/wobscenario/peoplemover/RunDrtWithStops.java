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

package wobscenario.peoplemover;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.common.collect.ImmutableSet;

public class RunDrtWithStops {

	public static void main(String[] args) {
		String configFile = "../../../shared-svn/projects/vw_rufbus/projekt2/input/configPM122.10.xml";
		RunDrtWithStops.run(configFile, false);
	}

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new MultiModeTaxiConfigGroup(),
				new OTFVisConfigGroup(), new DrtConfigGroup());
		//	for debugging:
		//		config.plans().setInputFile("singleDrtAgent.xml");
		DrtConfigGroup drt = (DrtConfigGroup)config.getModules().get(DrtConfigGroup.GROUP_NAME);

		drt.setEstimatedBeelineDistanceFactor(1.3);
		drt.setEstimatedDrtSpeed(30 / 3.6);
		drt.setMaxWalkDistance(500);
		drt.setMaxTravelTimeAlpha(1.5);
		drt.setMaxTravelTimeBeta(300);
		drt.setTransitStopFile("network/stopsWRS_300m.xml");
		drt.setOperationalScheme(OperationalScheme.stopbased);
		int threads = Runtime.getRuntime().availableProcessors() - 1;
		System.out.println(threads + " threads used for drt.");
		drt.setNumberOfThreads(threads);

		DvrpConfigGroup.get(config).setNetworkModes(ImmutableSet.of("av"));

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.qsim().setStartTime(0);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.controler().setWriteEventsInterval(1);
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) {

		Controler controler = DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis);

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

}
