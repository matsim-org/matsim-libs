/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.berlin;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
* @author ikaddoura
*/

public class BerlinControler {
	
	final static String configFile = "../../../public-svn/matsim/scenarios/countries/de/berlin/car-traffic-only-1pct-2014-08-01/config_be_1pct_ik.xml";
	final static String outputDirectory = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/run0/";

	public static void main(String[] args) {

		BerlinControler berlin = new BerlinControler();
		berlin.run();		
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outputDirectory);
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		controler.run();
	}

}

