/* *********************************************************************** *
 * project: org.matsim.*
 * GettingStarted.java
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
package tryouts.multiagentsimulation.hw1;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.controller.Controller;
import org.matsim.core.config.Config;
import org.matsim.vis.otfvis.OTFClientFile;



/**
 * @author thomas
 *
 */
public class GettingStarted {
	
	private static Logger log = Logger.getLogger(GettingStarted.class);
	
	private static Controller controller;
	private static String config;
	private static OTFClientFile otfClientFile;
	
	private static EventHandler1 eventHandler;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		init();
		
		log.info("GettingStarted ...");
		
		GettingStarted.controller.setOverwriteFiles( true );
		GettingStarted.controller.run();
		
		log.info("Starting OTFVis.");
		// gather available information about output directory and iteration number
		Scenario sc = GettingStarted.controller.getScenario() ;
		Config cf = sc.getConfig() ;
		String dir = cf.controler().getOutputDirectory();	// get output directory
		int iterNumber = cf.controler().getLastIteration();	// get itaration number
		String otfFileLocation = dir + "/ITERS/it."+iterNumber+"/"+iterNumber+".otfvis.mvi";
		log.info("Loding file " + otfFileLocation + " into OTFVis...");
		// init and run OTFVis
		GettingStarted.otfClientFile = new OTFClientFile(otfFileLocation);
		GettingStarted.otfClientFile.run();
		
		GettingStarted.eventHandler.writeChart(dir + "/ITERS/it."+iterNumber+"/chart.png");
		
		log.info("Program shut down ...");
	}
	
	/**
	 * init all parameter
	 */
	private static void init(){
		
		log.info("Initializing parameter...");
		log.info("");
		GettingStarted.config = "./tnicolai/configs/example5-config.xml";
		log.info("Loading config: " + GettingStarted.config);
		GettingStarted.controller = new Controller( GettingStarted.config );
		log.info("Create new Controller");
		GettingStarted.eventHandler = new EventHandler1();
		log.info("Create new event listener");
		GettingStarted.controller.addEventHandler( eventHandler );
	}

}

