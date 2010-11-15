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

package playground.mmoyo.ptRouterAdapted;

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mmoyo.ptRouterAdapted.replanning.AdapContListener;

/**
 * @author manuel
 * 
 * runs transit simulation with adapter router, loading it as a pluggablePlanStrategy
  */
public class SimWithAdapRouterLauncher {

	public static void main(String[] args) throws IOException{
		Config config;
		if ( args.length==0 ) {
			//config = ConfigUtils.loadConfig( "../playgrounds/mmoyo/test/input/playground/mmoyo/EquilCalibration/equil_config.xml" ) ;
			config = ConfigUtils.loadConfig( "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml" ) ;
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}
		
		config.scenario().setUseTransit(true);  //just in case that it is not set in config file
		config.scenario().setUseVehicles(true);

		final Controler controler = new Controler(config);
		controler.setCreateGraphs(true);
		controler.setOverwriteFiles(true);
		controler.setWriteEventsInterval(5); 
		controler.addControlerListener(new AdapContListener(controler)) ;
		controler.run();
	}
}
