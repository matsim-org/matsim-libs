/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
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
package playground.ikaddoura.incidents;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * @author ikaddoura
 *
 */
public class IncidentControler {
	
	private static final Logger log = Logger.getLogger(IncidentControler.class);
	
	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String networkChangeEventsFile;
	private static String networkChangeEventsFileDirectory;
	private static int run2_nrOfRuns;

	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];		
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];		
			log.info("runId: "+ runId);
			
			networkChangeEventsFile = args[3];		
			log.info("networkChangeEventsFile: "+ networkChangeEventsFile);
			
			networkChangeEventsFileDirectory = args[4];		
			log.info("networkChangeEventsFileDirectory: "+ networkChangeEventsFileDirectory);
			
			run2_nrOfRuns = Integer.valueOf(args[5]);		
			log.info("run2_nrOfRuns: "+ run2_nrOfRuns);
			
		} else {
			log.info("Using default parameters...");
			
			runId = "0";
						
			if (runId.equals("0")) {
				
				configFile = "../../../runs-svn/incidents/input/config.xml";
				outputDirectory = "../../../runs-svn/incidents/output/baseCase/";
				
				networkChangeEventsFile = null;
				networkChangeEventsFileDirectory = null;
				run2_nrOfRuns = Integer.MIN_VALUE;
				
			} else if (runId.equals("1")) {
				
				configFile = "../../../runs-svn/incidents/input/config.xml";
				outputDirectory = "../../../runs-svn/incidents/output/2016-03-15/";
				networkChangeEventsFile = "../../../runs-svn/incidents/input/networkChangeEvents_2016-03-15.xml.gz";
				
				networkChangeEventsFileDirectory = null;
				run2_nrOfRuns = Integer.MIN_VALUE;
				
			} else if (runId.equals("2a")) {
				
				configFile = "../../../runs-svn/incidents/input/config_2a_reroute0.5.xml";
				outputDirectory = "../../../runs-svn/incidents/output/run_2a_reroute0.5/";
				networkChangeEventsFileDirectory = "../../../runs-svn/incidents/input/nce/";
				run2_nrOfRuns = 27;

				networkChangeEventsFile = null;
				
			} else if (runId.equals("2b")) {
				
				configFile = "../../../runs-svn/incidents/input/config_2b_reroute1.0.xml";
				outputDirectory = "../../../runs-svn/incidents/output/run_2b_reroute1.0/";
				networkChangeEventsFileDirectory = "../../../runs-svn/incidents/input/nce/";
				run2_nrOfRuns = 27;

				networkChangeEventsFile = null;
				
			} else if (runId.equals("3")) {
				
				configFile = "../../../runs-svn/incidents/input/config_3.xml";
				outputDirectory = "../../../runs-svn/incidents/output/run_3_nce/";
				networkChangeEventsFileDirectory = "../../../runs-svn/incidents/input/nce/";

				networkChangeEventsFile = null;
				run2_nrOfRuns = Integer.MIN_VALUE;
				
			} else {
				throw new RuntimeException("Unknown run Id. Aborting...");
			}
		}
		
		IncidentControler main = new IncidentControler();	
		if (runId.equals("0")) {		
			main.run0();
		} else if (runId.equals("1")) {
			main.run1();
		} else if (runId.equals("2a")) {
			main.run2();
		} else if (runId.equals("2b")) {
			main.run2();
		} else if (runId.equals("3")) {
			main.run3();
		} else {
			throw new RuntimeException("Unknown run Id. Aborting...");
		}
	}
	
	/**
	 * Start a default run.
	 *
	 */
	private void run0() {
		
		log.info("Starting a default run.");
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(outputDirectory);
		
		config.network().setTimeVariantNetwork(false);
		
		Controler controler = new Controler(config);
		controler.run();
	}
	
	/**
	 * Start a run using a single network change events file.
	 *
	 */
	private void run1() {
		
		log.info("Starting a run using the same network change events file for all iterations.");
						
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(outputDirectory);
		
		config.network().setTimeVariantNetwork(true);
		config.network().setChangeEventsInputFile(networkChangeEventsFile);
		
		Controler controler = new Controler(config);
		controler.run();
	}
	
	/**
	 * Start a several (short) runs using one network change events file for each run.
	 *
	 */
	private void run2() {
		
		log.info("Starting several runs using a different network change events file for each run.");

		// Run 1 iteration and use for each run another network change events file; and assume X% to be allowed to re-route.
		// For each day: compare the simulated congestion patterns with the real-world congestion information.
		
		for (int i = 0; i <= run2_nrOfRuns; i++) {
			
			final String networkChangeFileThisRun = networkChangeEventsFileDirectory + "nce_" + i + ".xml.gz";
			final String outputDirectoryThisRun = outputDirectory + "nce_" + i + "/";
			
			Config config = ConfigUtils.loadConfig(configFile);
			
			config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
			config.controler().setOutputDirectory(outputDirectoryThisRun);
			
			config.network().setTimeVariantNetwork(true);
			config.network().setChangeEventsInputFile(networkChangeFileThisRun);
			
			Controler controler = new Controler(config);
			controler.run();
		}
	}
	
	/**
	 * Start a run using one network change events file for each day.
	 *
	 */
	private void run3() {
		
		log.info("Starting a run using a different network change events file in each iteration.");
		
		// Run 100 iterations and use for each day another network change events file (weekdays).
		// Compute avg. travel time per link, compare traffic flows of final iteration with default 100 iterations run.
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(outputDirectory);
		
		config.network().setTimeVariantNetwork(true);
		
		Controler controler = new Controler(config);
		controler.addControlerListener(new IncidentControlerListener(controler, networkChangeEventsFileDirectory));
		controler.run();
	}
	
}
	
