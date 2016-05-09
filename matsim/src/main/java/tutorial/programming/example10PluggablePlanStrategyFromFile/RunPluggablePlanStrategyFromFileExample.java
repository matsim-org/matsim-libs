/* *********************************************************************** *
 * project: org.matsim.*
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

package tutorial.programming.example10PluggablePlanStrategyFromFile;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;

public class RunPluggablePlanStrategyFromFileExample {

	public static void main(final String[] args) {
		ControlerUtils.initializeOutputLogging();
		

		Config config;
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig( "examples/tutorial/programming/pluggablePlanStrategy-config.xml" ) ;
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}

		final Controler controler = new Controler(config);
		controler.run();

	}

}
