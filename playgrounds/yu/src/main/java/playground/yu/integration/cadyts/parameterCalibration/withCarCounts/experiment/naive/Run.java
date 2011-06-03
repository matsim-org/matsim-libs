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
public class Run {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		double minTravVal = Double.parseDouble(config.findParam("naivePC",
				"minTravVal")), maxTravVal = Double.parseDouble(config
				.findParam("naivePC", "maxTravVal")), stepSize = Double
				.parseDouble(config.findParam("naivePC", "stepSize"));
		String outputPath = config.controler().getOutputDirectory();
		for (double val = minTravVal; val <= maxTravVal; val += stepSize) {
			config.planCalcScore().setTraveling_utils_hr(val);
			config.controler().setOutputDirectory(outputPath + val);

			System.out
					.println("################################################\nNAIVE Tests mit\t\"traveling\"\t=\t"
							+ val + "\tBEGAN!");

			Controler ctl = new PCCtl(config);
			ctl.setCreateGraphs(false);
			ctl.setOverwriteFiles(true);
			ctl.run();

			System.out
					.println("NAIVE Tests mit\t\"traveling\"\t=\t"
							+ val
							+ "\tENDED!\n################################################");
		}
	}

}
