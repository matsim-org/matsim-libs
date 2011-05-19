/* *********************************************************************** *
 * project: org.matsim.*
 * run.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.run;

import org.matsim.core.controler.Controler;

import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * Class responsible of running the custom controler for joint planning
 * @author thibautd
 */
public class run {
	/**
	 * run the simulation.
	 * @param args the config file to use.
	 */
	public static void main(String[] args) {
		String configFile = args[0];

		Controler controler = JointControlerUtils.createControler(configFile);
		controler.setOverwriteFiles(true);
		//controler.addControlerListener(new JointReplanningControlerListener());

		controler.run();
	}
}
