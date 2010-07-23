/* *********************************************************************** *
 * project: org.matsim.*
 * Controler_eThekwini_Census.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim.controler;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class Controler_eThekwini_Census {
	private static int numberOfIterations;
	private static boolean overwrite;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2){
			throw new IllegalArgumentException("Need number of iterations and logical overwrite argument");
		} else{
			numberOfIterations = Integer.parseInt(args[0]);
			overwrite = Boolean.parseBoolean(args[1]);
		}
		/*
		 * Set up basic config.
		 */
		MyBasicConfig mbc = new MyBasicConfig();
		Config config = mbc.getConfig();
		/*
		 * Customise the config file.
		 */
		config.global().setCoordinateSystem("WGS84_UTM36S");
		config.controler().setLastIteration(numberOfIterations);
		config.network().setInputFile("./input/output_network_100_Emme.xml.gz");
		config.plans().setInputFile("./input/Generated_plans_100.xml.gz");

		Controler c = new Controler(config);
		c.setCreateGraphs(true);
		c.setWriteEventsInterval(20);
		c.setOverwriteFiles(overwrite);
		c.run();
	}

}
