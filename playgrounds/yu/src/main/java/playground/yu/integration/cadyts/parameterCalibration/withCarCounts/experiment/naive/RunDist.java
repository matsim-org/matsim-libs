/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.naive;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * tries to observes log-likelihood values with different parameter
 * (traveling...) values
 *
 * @author yu
 *
 */
public class RunDist {

	private static void run(Config config, String outputPath, double val) {
		config.planCalcScore().setMonetaryDistanceCostRateCar(val);
		config.controler().setOutputDirectory(outputPath + val);

		System.out
				.println("################################################\nNAIVE Tests mit\t\"monetaryDistanceCostRateCar\"\t=\t"
						+ val + "\tBEGAN!");

		Controler ctl = new PCCtl(config);
		ctl.setCreateGraphs(false);
		ctl.setOverwriteFiles(true);
		ctl.run();

		System.out
				.println("NAIVE Tests mit\t\"monetaryDistanceCostRateCar\"\t=\t"
						+ val
						+ "\tENDED!\n################################################");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		String outputPath = config.controler().getOutputDirectory();
		/*
		 * the outputPath should NOT end with"/", otherwise there could be
		 * problems by command in linux
		 */

		if (args[1].equals("small")) { // small senarios
			double minTravVal = Double.parseDouble(config.findParam("naivePC",
					"minTravVal")), maxTravVal = Double.parseDouble(config
					.findParam("naivePC", "maxTravVal")), stepSize = Double
					.parseDouble(config.findParam("naivePC", "stepSize"));

			for (double val = minTravVal; val <= maxTravVal; val += stepSize) {
				run(config, outputPath, val);
			}
		} else if (args[1].equals("real")) { // real senarios
			double distVal = -Double.parseDouble(args[2]) / 10000d;
			if (args.length == 4 && args[3].equals("positive")) {
				distVal = -distVal;
			}
			run(config, outputPath, distVal);
		}

	}

}
