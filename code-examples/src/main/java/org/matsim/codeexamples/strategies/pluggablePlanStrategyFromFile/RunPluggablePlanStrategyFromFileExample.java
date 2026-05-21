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

package org.matsim.codeexamples.strategies.pluggablePlanStrategyFromFile;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryLogging;

public class RunPluggablePlanStrategyFromFileExample {

	public static void main(final String[] args) {
//		ControlerUtils.initializeOutputLogging();
		OutputDirectoryLogging.catchLogEntries();
		
		
		Config config;
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig( "scenarios/equil/config-with-pluggablePlanStrategy.xml" ) ;
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}

		final Controler controler = new Controler(config);
		controler.run();

	}

}
