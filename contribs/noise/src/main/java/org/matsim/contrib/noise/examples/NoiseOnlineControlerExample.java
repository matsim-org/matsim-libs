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

import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

/**
 * An example how to use the noise module during a MATSim run (= online noise computation).
 * Depending on the specifications in the noise config group, noise damages may be internalized (average cost pricing vs. marginal cost pricing).
 * 
 * @author ikaddoura
 *
 */
public class NoiseOnlineControlerExample {
	
	private static String configFile = "/pathToConfigFile/config.xml";

	public static void main(String[] args) throws IOException {
				
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());		
		Controler controler = new Controler(config);
		controler.addControlerListener(new NoiseCalculationOnline(controler));
		controler.run();
				
		// processing the output, not necessary
		String workingDirectory = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immisions/";
		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		ProcessNoiseImmissions readNoiseFile = new ProcessNoiseImmissions(workingDirectory, receiverPointsFile, noiseParameters.getReceiverPointGap());
		readNoiseFile.run();
		
	}
	
}
