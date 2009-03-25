/* *********************************************************************** *
 * project: org.matsim.*
 * MyRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.msieg;

import org.matsim.core.controler.Controler;

public class MyRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String config = "./examples/test/sioux/config.xml";
		if(args.length > 0)
			config = args[0];
		
		Controler controler = new Controler(config);
		controler.run();
		
		String[] args2 = {controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/" + controler.getConfig().controler().getLastIteration() + ".otfvis.mvi"};
		
		//OTFVis.main(args2);

	}

}

