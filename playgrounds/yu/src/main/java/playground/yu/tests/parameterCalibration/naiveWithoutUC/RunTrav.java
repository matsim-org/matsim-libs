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
package playground.yu.tests.parameterCalibration.naiveWithoutUC;

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
public class RunTrav {

	private static void run(Config config, String outputPath, double val) {
		config.planCalcScore().setTraveling_utils_hr(val);
		config.controler().setOutputDirectory(outputPath + val);

		System.out
				.println("################################################\nNAIVE Tests mit\t\"traveling\"\t=\t"
						+ val + "\tBEGAN!");

		Controler ctl = new Controler(config);
		ctl.addControlerListener(new SimCntLogLikelihoodCtlListener());
		ctl.setCreateGraphs(false);
		ctl.setOverwriteFiles(true);
		ctl.run();

		System.out.println("NAIVE Tests mit\t\"traveling\"\t=\t" + val
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

		if (args[1].equals("sequence")) { // sequence senarios
			double minVal = Double.parseDouble(config.findParam("naivePC",
					"minVal"))//
			, maxVal = Double
					.parseDouble(config.findParam("naivePC", "maxVal"))//
			, stepSize = Double.parseDouble(config.findParam("naivePC",
					"stepSize"));

			for (double val = minVal; val <= maxVal; val += stepSize) {
				run(config, outputPath, val);
			}
		} else if (args[1].equals("parallel")) { // parallel senarios
			double val = -Double.parseDouble(args[2]) / 10d;
			if (args.length == 4 && args[3].equals("positive")) {
				val = -val;
			}
			run(config, outputPath, val);
		}

	}

}
