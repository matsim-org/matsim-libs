/* *********************************************************************** *
 * project: org.matsim.*
 * Run2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.run;

import java.io.IOException;

import org.matsim.core.controler.Controler;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.paramCorrection.PCCtl;

/**
 * @author yu
 * 
 */
public class Run2 {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final Controler ctl;
		if (args.length == 0) {
			// ctl = new
			// playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.experiment.onlyTraveling.newStrMn.PCCtl(
			// new String[] { "test/cali3/cfgC3onlyTravStep0newStrMn.xml" });
			// ctl = new
			// playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.experiment.dummyParam.MATSimChoiceCalibrator.PCCtl(
			// new String[] {
			// "test/cali3/newIntegrationCodeTestLocal_MatsimCalibrator.xml" });
			ctl = new PCCtl(
					new String[] { "test/cali3/newIntegrationCodeTestLocalC1p3.xml" });

		} else {
			ctl = new PCCtl(args);
		}
		ctl.setOverwriteFiles(true);
		ctl.setCreateGraphs(false);
		ctl.run();
	}
}
