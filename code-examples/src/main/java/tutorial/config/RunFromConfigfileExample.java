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

package tutorial.config;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * runs a mobsim and writes events output.  See the config file for configuration details.
 * 
 * @author nagel
 *
 */
public class RunFromConfigfileExample {

	public static void main(final String[] args) {
		String configFile ;
		if ( args!=null && args.length>=1 ) {
			configFile = args[0] ;
		} else {
			configFile = "examples/tutorial/config/example1-config.xml" ;
//			configFile = "examples/tutorial/config/example5-config.xml" ;
//			configFile = "examples/tutorial/config/example5trips-config.xml" ;
		}

		Config config = ConfigUtils.loadConfig(configFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}

}
