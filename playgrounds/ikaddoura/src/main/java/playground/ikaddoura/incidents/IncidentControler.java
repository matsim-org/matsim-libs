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
	
	static String configFile;
	static String networkChangeFile;
	static String outputDirectory;
	
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			networkChangeFile = args[1];
			log.info("network change file: " + networkChangeFile);
			
			outputDirectory = args[2];
			log.info("output directory: " + outputDirectory);
			
		} else {
			configFile = "../../../runs-svn/incidents/input/config.xml";
			networkChangeFile = "../../../runs-svn/incidents/input/networkChangeEvents_2016-03-15.xml.gz";
			outputDirectory = "../../../runs-svn/incidents/output/2016-03-15-b/";
		}
		
		IncidentControler main = new IncidentControler();
		main.run();
	}
	
	private void run() {
				
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(outputDirectory);
		
		config.network().setTimeVariantNetwork(true);
		config.network().setChangeEventInputFile(networkChangeFile);
		
		Controler controler = new Controler(config);
		controler.run();
	}
	
}
	
