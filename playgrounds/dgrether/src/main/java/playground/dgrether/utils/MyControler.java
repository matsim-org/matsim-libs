/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import org.matsim.core.controler.Controler;

import tutorial.programming.example07ControlerListener.MyControlerListener;



/**
 * @author dgrether
 *
 */
public class MyControler {

	private static final String EQUILCONF = "../testData/examples/equil/config.xml";

	private static final String EQUILNOROUTESCONF = "../testData/equilNoRoutes/config.xml";

	private static final String EQUILONE = "../testData/equil1Agent/config.xml";

	private static final String EQUILPT = "../testData/equilPt/config.xml";

//	private static String usedConfig = EQUILNOROUTESCONF;

	private static String usedConfig = EQUILPT;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//set a default config for convenience...
		String [] config = {usedConfig};
		//Create an instance of the controler and
		Controler controler = new Controler(config);
		//so we don't have to delete the output directory
		controler.setOverwriteFiles(true);
		//add an instance of this class as ControlerListener
		controler.addControlerListener(new MyControlerListener());
		//call run() to start the simulation
		controler.run();
		//open snapshot of the 10th iteration
//		String[] visargs = {"../testData/output/equilNoRoutes/ITERS/it.10/Snapshot"};
//		NetVis.main(visargs);
	}


}
