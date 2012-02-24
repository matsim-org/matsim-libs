/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.anhorni.surprice.preprocess.CreateScenario;

public class MultiDayControler {
	
	private final static Logger log = Logger.getLogger(MultiDayControler.class);
	
	public static void main (final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		String configFile = args[0];		
				
		for (String day : CreateScenario.days) {
			Config config = ConfigUtils.loadConfig(configFile);
			
			String outPath = config.controler().getOutputDirectory();
			String plansPath = config.plans().getInputFile();
			
			config.setParam("controler", "outputDirectory", outPath + "/" + day);
			config.setParam("plans", "inputPlansFile", plansPath + "/" + day + "/plans.xml");
			config.setParam("controler", "runId", day);
			
			DayControler controler = new DayControler(config);
			controler.run();
		}
		log.info("Week simulated, yep, ..........");
    }
}
