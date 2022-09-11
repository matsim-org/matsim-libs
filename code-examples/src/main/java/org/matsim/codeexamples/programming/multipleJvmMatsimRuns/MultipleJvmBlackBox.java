/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.programming.multipleJvmMatsimRuns;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

import java.net.URL;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * This class is a completely self-contained class that only requires some 
 * inputs passed as arguments.
 * 
 * @author jwjoubert
 */
public class MultipleJvmBlackBox {
	final private static Logger LOG = LogManager.getLogger(MultipleJvmBlackBox.class);

	public static void main(String[] args) {
		LOG.info("Start...");
		run(args);
		LOG.info("End.");
	}
	
	public static void run(String[] args) {
		final URL url = ExamplesUtils.getTestScenarioURL( "equil" );
		final URL configUrl = IOUtils.extendUrl( url, "config.xml" );;
		Config config = ConfigUtils.loadConfig( configUrl ) ;
		config.controler().setOutputDirectory( "./output/" );
		
		config.controler().setLastIteration( 2 );
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		Controler controler = new Controler( scenario ) ;
		controler.run() ;
	}
	
	
}
