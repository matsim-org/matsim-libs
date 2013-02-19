/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptedControler.java
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

package playground.mmoyo.analysis.comp;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import playground.mmoyo.utils.calibration.NullifyingScoringFunctionFactory;

public class Controler_launcher2 {
	
	public static void main(String[] args) {
		String configFile; 
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../../ptManuel/calibration/my_config.xml";
		}

		Config config = null;
		config = ConfigUtils.loadConfig(configFile);
		
		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		
		//add nullifying scoring for effective brute force
		if (Boolean.parseBoolean(config.getParam("cadytsPt", "useBruteForce"))){
			controler.setScoringFunctionFactory(new NullifyingScoringFunctionFactory());
		}
		
		controler.run();
	} 
}
