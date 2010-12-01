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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.run;

import java.io.IOException;

import org.matsim.core.controler.Controler;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utilityCorrection.BseUCControler;

/**
 * @author yu
 * 
 */
public class Run {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void runNew(String[] args) throws IOException {
		final Controler ctl;
		if (args.length == 0) {
			ctl = new playground.yu.integration.cadyts.demandCalibration.withCarCounts.utilityCorrection.BseUCControler(
					new String[] { "../integration-parameterCalibration/test/cali3/cfgC3step0TestLocal_DC2.xml" });
		} else {
			ctl = new BseUCControler(args);
		}

		ctl.setOverwriteFiles(true);
		ctl.setCreateGraphs(false);
		ctl.run();
	}

	public static void runOld(String[] args) throws IOException {
		final Controler ctl;
		if (args.length == 0) {
			ctl = new playground.yu.integration.cadyts.demandCalibration.withCarCounts.utilityCorrection.BseUCControler(
					new String[] { "../integration-parameterCalibration/test/cali3/oldIntegrationCodeTest_MatsimCalibrator.xml" });
		} else {
			ctl = new BseUCControler(args);
		}

		ctl.setOverwriteFiles(true);
		ctl.setCreateGraphs(false);
		ctl.run();
	}

	public static void main(String[] args) throws IOException {
		runOld(args);
		// runNew(args);
	}
}
