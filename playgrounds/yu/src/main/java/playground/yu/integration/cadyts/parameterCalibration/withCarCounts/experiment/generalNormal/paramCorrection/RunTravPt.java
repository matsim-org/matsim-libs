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

/**
 * starts tests with different initial betaTravelingPt values.
 *
 * @author yu
 *
 */
public class RunTravPt {
	private static void run(Config config, String outputPath, double val) {
		// /////////////////////////////////////////////////////
		config.planCalcScore().setTravelingPt_utils_hr(val);
		// ///////////////////////////////////////////////////////
		config.controler().setOutputDirectory(outputPath + val);
		// //////////////////////////////////////////////////////
		System.out
				.println("################################################\n Tests mit\t\"travelingPt\"\t=\t"
						+ val + "\tBEGAN!");

		Controler ctl = new PCCtl(config);
		ctl.setCreateGraphs(false);
		ctl.setOverwriteFiles(true);
		ctl.run();

		System.out.println("Tests mit\t\"travelingPt\"\t=\t" + val
				+ "\tENDED!\n################################################");
	}

	/**
	 * @param args0
	 *            configfile (required)
	 * @param args1
	 *            travelPt<=0 ? travelPt*(-10d) : travelPt*10d (required)
	 * @param args2
	 *            travelPt>0 ? "positive" : (nothing)
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		String outputPath = config.controler().getOutputDirectory();
		/* please without "/" at the end */

		double travPtVal = -Double.parseDouble(args[1]/*
													 * to be used value * -10d
													 */) / 10d;
		if (args.length == 3 && args[2].equals("positive")) {
			travPtVal = -travPtVal;
		}
		run(config, outputPath, travPtVal);
	}

}
