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

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

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
	
	public void runDynusT() {
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
		File iterDir = new File(tmpDir);
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
		
		String cmd = this.dynusTDir + "/DynusT.exe";
		log.info("running command: " + cmd);
//		String logfileName = "dynus-t.log";
//		int timeout = 3600;
//		int exitcode = ExeRunner.run(cmd, logfileName, timeout);
//		if (exitcode != 0) {
//			throw new RuntimeException("There was a problem running Dynus-T. exit code: " + exitcode);
//		}
	}
}
