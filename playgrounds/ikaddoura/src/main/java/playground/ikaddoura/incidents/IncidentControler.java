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
	
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			throw new RuntimeException("Not implemented.");	
		}
		
		IncidentControler main = new IncidentControler();
//		main.run0();
//		main.run1a();
//		main.run1b();
		main.run2();

	}
	
	/**
	 * Start a default run.
	 *
	 */
	private void run0() {
		
		log.info("Starting a default run.");
		
		final String configFile = "../../../runs-svn/incidents/input/config.xml";
		final String outputDirectory = "../../../runs-svn/incidents/output/baseCase/";
		
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
	private void run1a() {
		
		log.info("Starting a run using the same network change events file for all iterations.");
		
		final String day = "2016-03-15";
		
		final String configFile = "../../../runs-svn/incidents/input/config.xml";
		final String networkChangeFile = "../../../runs-svn/incidents/input/networkChangeEvents_" + day + ".xml.gz";
		final String outputDirectory = "../../../runs-svn/incidents/output/" + day + "/";
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(outputDirectory);
		
		config.network().setTimeVariantNetwork(true);
		config.network().setChangeEventInputFile(networkChangeFile);
		
		Controler controler = new Controler(config);
		controler.run();
	}
	
	/**
	 * Start a several (short) runs using one network change events file for each run.
	 *
	 */
	private void run1b() {
		
		log.info("Starting several runs using a different network change events file for each run.");

		// run 1 iteration and use for each run another network change events file; and assume X% to be allowed to re-route
		// --> for each day: compare the simulated congestion patterns with the traffic infos
		
		// TODO
		
		final String day = "2016-03-15";
		
		final String configFile = "../../../runs-svn/incidents/input/config.xml";
		final String networkChangeFile = "../../../runs-svn/incidents/input/networkChangeEvents_" + day + ".xml.gz";
		final String outputDirectory = "../../../runs-svn/incidents/output/" + day + "/";
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(outputDirectory);
		
		config.network().setTimeVariantNetwork(true);
		config.network().setChangeEventInputFile(networkChangeFile);
		
		Controler controler = new Controler(config);
		controler.run();
	}
	
	/**
	 * Start a run using one network change events file for each day.
	 *
	 */
	private void run2() {
		
		log.info("Starting a run using a different network change events file in each iteration.");
		
		final String configFile = "../../../runs-svn/incidents/input/config.xml";
		final String networkChangeFileDirectory = "../../../runs-svn/incidents/input/nce/";
		final String outputDirectory = "../../../runs-svn/incidents/output/nce/";
		
		// use case 1: run 100 iterations and use for each day another network change events file (Monday-Friday)
		// --> compute avg. travel time per link, compare traffic flows of final iteration with default 100 iterations run
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(outputDirectory);
		
		config.network().setTimeVariantNetwork(true);
		
		Controler controler = new Controler(config);
		controler.addControlerListener(new IncidentControlerListener(controler, networkChangeFileDirectory));
		controler.run();
	}
	
}
	
