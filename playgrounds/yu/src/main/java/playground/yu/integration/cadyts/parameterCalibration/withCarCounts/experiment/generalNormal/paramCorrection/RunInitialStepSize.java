/* *********************************************************************** *
 * project: org.matsim.*
 * RunTravPt.java
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.paramCorrection;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;

/**
 * starts tests with different initialStepSize in cadyts values.
 *
 * @author yu
 *
 */
public class RunInitialStepSize {
	private static void run(Config config, String outputPath, double val) {
		// /////////////////////////////////////////////////////
		config.setParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"initialStepSize", Double.toString(val));
		// ///////////////////////////////////////////////////////
		config.controler().setOutputDirectory(outputPath + val);
		// //////////////////////////////////////////////////////
		System.out
				.println("################################################\n P.C. travPt Tests mit\t\"initialStepSize\"\t=\t"
						+ val + "\tBEGAN!");

		Controler ctl = new PCCtl(config);
		ctl.setCreateGraphs(false);
		ctl.setOverwriteFiles(true);
		ctl.run();

		System.out.println(" P.C. travPt Tests mit\t\"initialStepSize\"\t=\t"
				+ val
				+ "\tENDED!\n################################################");
	}

	/**
	 * @param args0
	 *            configfile (required)
	 * @param args1
	 *            initialStepSize (positive real number)
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		String outputPath = config.controler().getOutputDirectory();
		/* please without "/" at the end */

		double initialStepSize = Double.parseDouble(args[1]
		/* to be used value * 10d */) / 10d;
		run(config, outputPath, initialStepSize);
	}

}
