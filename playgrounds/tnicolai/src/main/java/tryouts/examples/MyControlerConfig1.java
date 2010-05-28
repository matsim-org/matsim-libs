/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package tryouts.examples;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.run.Controler;


/**
 * runs trip-based iterations (=DTA) and writes events and visualizer files.  
 * See the config file for configuration details.
 * 
 * @author nagel
 *
 */
public class MyControlerConfig1 {
	private static Logger log = Logger.getLogger(MyControlerConfig1.class);

	public static void main(final String[] args) {
		
		log.info("Starting MyControlerConfig1 ...");
		
		String root = System.getProperty("user.dir");
		String configFile = "/tnicolai/configs/example_config2.xml" ;
		log.info("Loading config file: " + root + configFile);
		
		Controler controler = new Controler( root + configFile ) ;
		
		controler.setOverwriteFiles(true) ;
		controler.run() ;
		
		Scenario sc = controler.getScenario() ;
		Config cf = sc.getConfig() ;
		String dir = cf.controler().getOutputDirectory();
		log.warn("Output is in " + dir + ".  Use otfvis (preferably hardware-accelerated) to play movies." ) ; 
	}

}
