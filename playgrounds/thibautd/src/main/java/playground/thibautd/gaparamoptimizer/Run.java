/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
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
package playground.thibautd.gaparamoptimizer;

import org.matsim.core.controler.Controler;

import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * @author thibautd
 */
public class Run {
	public static void main(String[] args) {
		String configFile = args[0];

		Controler controler = JointControlerUtils.createControler(configFile);
		//controler.setOverwriteFiles(true);
		//controler.addControlerListener(new JointReplanningControlerListener());
		controler.addControlerListener(new JPOParametersOptimizerListener());

		controler.run();
	}
}

