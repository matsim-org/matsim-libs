/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package tutorial.programming.example06EventsHandling;

import org.matsim.core.controler.Controler;

/**
 * @author nagel
 *
 */
public class RunEventsHandlingWithControlerExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//set a default config for convenience...
		String [] config = {"examples/tutorial/programming/example7-config.xml"};

		//Create an instance of the controler
		Controler controler = new Controler(config);
		
		// add the events handlers
		controler.getEvents().addHandler( new MyEventHandler1() );
		controler.getEvents().addHandler( new MyEventHandler2( 500 ) );
		controler.getEvents().addHandler( new MyEventHandler3() );

		//call run() to start the simulation
		controler.run();
	}

}
