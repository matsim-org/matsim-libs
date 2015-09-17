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

package tutorial.config.example5iterations;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * runs trip-based iterations (=DTA) and writes events files.  
 * See the config file for configuration details.
 * 
 * @author nagel
 *
 */
public class RunExample5Trips {
	private static Logger log = Logger.getLogger(RunExample5Trips.class);

	public static void main(final String[] args) {
		String configFile = "examples/tutorial/config/example5trips-config.xml" ;
		
		Config config = ConfigUtils.loadConfig( configFile ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;

		controler.run() ;
		
		String dir = config.controler().getOutputDirectory();
		log.warn("Output is in " + dir + "." ) ; 
	}

}
