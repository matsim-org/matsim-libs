/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package org.matsim.contrib.noise.examples;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * An example how to use the noise module during a MATSim run (= online noise computation).
 * 
 * The {@link NoiseConfigGroup} specifies parameters that are relevant for the noise computation and if noise damages are internalized.
 * For the internalization of noise damages, there is an average and a marginal cost pricing approach, see {@link NoiseAllocationApproach}.
 * 
 * For an example of how to compute noise levels, damages etc. for a final iteration (= offline noise computation), see {@link NoiseOfflineCalculationExample}. 
 * 
 * @author ikaddoura
 *
 */
public class NoiseOnlineControlerExample {
	
	private static final String configFile = "./test/input/org/matsim/contrib/noise/config.xml";

	public static void main(String[] args) throws IOException {
				
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.addControlerListener(new NoiseCalculationOnline(controler));
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
				
		// optionally process the output data
		String workingDirectory = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(workingDirectory, receiverPointsFile, noiseParameters.getReceiverPointGap());
		processNoiseImmissions.run();	
	}
	
}
