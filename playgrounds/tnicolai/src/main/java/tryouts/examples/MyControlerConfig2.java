/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerConfig2.java
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

/**
 * 
 */
package tryouts.examples;

import java.io.File;

import org.matsim.core.controler.Controler;

/**
 * @author thomas
 *
 */
public class MyControlerConfig2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try{
			System.out.println(new File(".").getCanonicalPath() );
		}
		catch(Exception e){}
		
		// create an instance of the controler
		Controler controler = new Controler("./tnicolai/configs/multipleIterations.xml");

		controler.setOverwriteFiles(true);
		// sets, whether Outputfiles are overwritten
		controler.setCreateGraphs(false);
		// sets, whether output Graphs are created – set false, if you don't need them
		controler.setWriteEventsInterval(5);
		// sets, how often events are written. Set 0 to disable it completely

		// add own controler listener
		MyIterationListenerForControlerConfig2 listener = new MyIterationListenerForControlerConfig2();
		controler.addControlerListener(listener);
		
		// now  run the controler
		controler.run();

	}

}