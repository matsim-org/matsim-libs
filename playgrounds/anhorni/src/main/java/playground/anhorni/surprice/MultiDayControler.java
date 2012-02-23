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
			DayControler controler = new DayControler(configFile);
			String outPath = controler.getConfig().controler().getOutputDirectory();
			String plansPath = controler.getConfig().plans().getInputFile();
			controler.getConfig().setParam("controler", "outputDirectory", outPath + "/" + day);
			controler.getConfig().setParam("plans", "inputPlansFile", plansPath + "/" + day);
			controler.run();
		}
    }
}
