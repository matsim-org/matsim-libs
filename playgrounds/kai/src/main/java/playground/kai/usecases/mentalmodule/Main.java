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

package playground.kai.usecases.mentalmodule;

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;

class Main {

	public static void main(final String[] args) throws IOException {

		Config config;
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig("./kai/src/main/java/playground/kai/other/myconfig.xml");
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}

		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
