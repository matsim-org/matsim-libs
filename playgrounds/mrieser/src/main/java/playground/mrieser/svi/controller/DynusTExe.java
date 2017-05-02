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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.UncheckedIOException;
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

				"parameter.dat",
				"node.csv"
		};

		log.info("Creating iteration-directory...");
		File iterDir = new File(this.tmpDir);
		if (!iterDir.exists()) {
			iterDir.mkdir();
		}
		log.info("Copying application files to iteration-directory...");
		String exeName = null;
		List<String> exeFiles = new ArrayList<String>();
		for (File f : new File(this.dynusTDir).listFiles()) {
			if (f.isFile()) {
				log.info("  Copying " + f.getName());
				exeFiles.add(f.getName());
                try {
                    Files.copy(f.toPath(), new File(this.tmpDir + "/" + f.getName()).toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                String lcName = f.getName().toLowerCase(Locale.ROOT);
				if (lcName.endsWith(".exe") && lcName.contains("dynust")) {
					log.info("    Found DynusT executable");
					exeName = f.getName();
				}
			}
		}
		log.info("Copying model files to iteration-directory...");
		for (String filename : modelFiles) {
			log.info("  Copying " + filename);
            try {
                Files.copy(new File(this.modelDir + "/" + filename).toPath(), new File(this.tmpDir + "/" + filename).toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
		for (File f : new File(this.modelDir).listFiles()) {
			if (f.getName().toLowerCase(Locale.ROOT).endsWith(".dws")) {
				log.info("  Copying " + f.getName());
                try {
                    Files.copy(f.toPath(), new File(this.tmpDir + "/" + f.getName()).toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
		}

		// adapt workingdir.ini
		File file = new File(this.tmpDir + "/workingdir.ini");
		try {
			FileWriter write = new FileWriter(file, false);
			PrintWriter prntln = new PrintWriter(write);
			prntln.print(this.tmpDir);
			prntln.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		String logfileName = this.tmpDir + "/dynus-t.log";

		String cmd = this.tmpDir + "/" + exeName;
		log.info("running command: " + cmd + " in directory " + this.tmpDir);
		int timeout = 3600; // 1 hour should hopefully be enough
		int errorCnt = 0;
		int maxTries = 3;
		while (errorCnt < maxTries) {
			if (errorCnt > 0) {
				log.warn("Trying to re-run Dynus-T");
			}
			int exitcode = ExeRunner.run(cmd, logfileName, timeout, this.tmpDir);
			if (exitcode != 0) {
				log.error("There was a problem running Dynus-T. exit code: " + exitcode);
				errorCnt++;
				if (errorCnt >= maxTries) {
					throw new RuntimeException("Too many failures trying to run Dynus-T.");
				}
			} else {
				break;
			}
		}

		if (cleanUp) {
			for (String filename : exeFiles) {
				log.info("  Deleting " + filename);
				new File(this.tmpDir + "/" + filename).delete();
			}
		}
	}
}
