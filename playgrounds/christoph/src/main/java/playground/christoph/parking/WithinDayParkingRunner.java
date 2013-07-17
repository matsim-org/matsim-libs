/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayParkingRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.NetworkUtils;

public class WithinDayParkingRunner {

	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("");
		} else {
			Config config = ConfigUtils.loadConfig(args[0], MultiModalConfigGroup.class);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
			// if multi-modal simulation is enabled
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
			if (multiModalConfigGroup != null && multiModalConfigGroup.isMultiModalSimulationEnabled()) {
				/*
				 * If the network is not multi-modal but multi-modal simulation is enabled,
				 * convert it to multi-modal.
				 */
				if (!NetworkUtils.isMultimodal(scenario.getNetwork())) {
					new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
				}				
			}
			
			Controler controler = new Controler(scenario);
			
			WithinDayParkingControlerListener controlerListener = new WithinDayParkingControlerListener(controler);
			controler.addControlerListener(controlerListener);

			// controler listener that initializes the multi-modal simulation
			MultiModalControlerListener listener = new MultiModalControlerListener();
			controler.addControlerListener(listener);
			
			controler.setOverwriteFiles(true);
			controler.run();
			
			System.exit(0);			
		}
		
	}
}
