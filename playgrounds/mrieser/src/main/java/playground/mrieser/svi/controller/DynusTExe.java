/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.controller;

import java.io.File;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ExeRunner;

/**
 * @author mrieser
 */
public class DynusTExe {

	private final static Logger log = Logger.getLogger(DynusTExe.class);

	private final String dynusTDir;
	private final String modelDir;
	private final String tmpDir;

	public DynusTExe(final String dynusTExeDir, final String modelDir, final String tmpDir) {
		this.dynusTDir = dynusTExeDir;
		this.modelDir = modelDir;
		this.tmpDir = tmpDir;
	}

	public void runDynusT(final boolean cleanUp) {
		final String[] exeFiles = new String[] {
				"DynusT.exe",
				"DLL_ramp.dll",
				"Ramp_Meter_Fixed_CDLL.dll",
				"Ramp_Meter_Feedback_CDLL.dll",
				"Ramp_Meter_Feedback_FDLL.dll",
				"libifcoremd.dll",
				"libmmd.dll",
				"Ramp_Meter_Fixed_FDLL.dll",
				"libiomp5md.dll"
		};
		final String[] modelFiles = new String[] {
				"network.dat",
				"scenario.dat",
				"control.dat",
				"ramp.dat",
				"incident.dat",
				"movement.dat",
				"vms.dat",
				"origin.dat",
				"destination.dat",
				"StopCap4Way.dat",
				"StopCap2Way.dat",
				"YieldCap.dat",
				"WorkZone.dat",
				"GradeLengthPCE.dat",
				"leftcap.dat",
				"system.dat",
				"output_option.dat",
				"bg_demand_adjust.dat",

				"xy.dat",
				"TrafficFlowModel.dat",

				"parameter.dat"
		};

		log.info("Creating iteration-directory...");
		File iterDir = new File(this.tmpDir);
		if (!iterDir.exists()) {
			iterDir.mkdir();
		}
		log.info("Copying application files to iteration-directory...");
		for (String filename : exeFiles) {
			log.info("  Copying " + filename);
			IOUtils.copyFile(new File(this.dynusTDir + "/" + filename), new File(this.tmpDir + "/" + filename));
		}
		log.info("Copying model files to iteration-directory...");
		for (String filename : modelFiles) {
			log.info("  Copying " + filename);
			IOUtils.copyFile(new File(this.modelDir + "/" + filename), new File(this.tmpDir + "/" + filename));
		}
		for (File f : new File(this.modelDir).listFiles()) {
			if (f.getName().toLowerCase(Locale.ROOT).endsWith(".dws")) {
				log.info("  Copying " + f.getName());
				IOUtils.copyFile(f, new File(this.tmpDir + "/" + f.getName()));
			}
		}

		String logfileName = this.tmpDir + "/dynus-t.log";

		String cmd = this.tmpDir + "/DynusT.exe";
		log.info("running command: " + cmd + " in directory " + this.tmpDir);
		int timeout = 7200; // 2 hours should hopefully be enough
		int exitcode = ExeRunner.run(cmd, logfileName, timeout, this.tmpDir);
		if (exitcode != 0) {
			throw new RuntimeException("There was a problem running Dynus-T. exit code: " + exitcode);
		}

		if (cleanUp) {
			for (String filename : exeFiles) {
				log.info("  Deleting " + filename);
				new File(this.tmpDir + "/" + filename).delete();
			}
		}
	}
}
