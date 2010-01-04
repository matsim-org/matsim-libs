/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
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

package tutorial.config.example2vis;

import org.apache.log4j.Logger;

import org.matsim.run.Controler;


/**
 * runs a mobsim and writes events and visualizer files.  See the config file for configuration details.
 * 
 * @author nagel
 *
 */
public class MyControler2 {
	private static Logger log = Logger.getLogger(MyControler2.class);

	public static void main(final String[] args) {
		String configFile = "examples/tutorial/config/example2-config.xml" ;
		
		Controler controler = new Controler( configFile ) ;
		
		controler.setOverwriteFiles(true) ;
		controler.run() ;
		
		// the following lines are there to start the non-hardware-accelerated visualizer.  This is NOT the recommended
		// way of doing this; it is just here to produce some visible output at an early stage of the tutorial.
//		Scenario sc = controler.getScenario() ;
//		Config cf = sc.getConfig() ;
//		String dir = cf.controler().getOutputDirectory();
//		log.info("The following should bring up the slow gui.  This sometimes hangs ..." );
//		log.info("There is also kmz output in " + dir + "/ITERS/it.0" ) ;
//		log.warn("However, if you want to run larger scenarios, you HAVE to learn how to use the graphics-hardware-accelerated otfvis." ) ;
//		new OTFClientSwing(dir + "/ITERS/it.0/0.otfvis.mvi").start();

	}

}
