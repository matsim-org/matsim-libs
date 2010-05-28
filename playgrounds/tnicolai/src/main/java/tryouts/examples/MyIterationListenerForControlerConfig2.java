/* *********************************************************************** *
 * project: org.matsim.*
 * MyIterationListenerForControlerConfig2.java
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

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author thomas
 *
 */
public class MyIterationListenerForControlerConfig2 implements IterationEndsListener, StartupListener{

	private static final Logger log = Logger.getLogger(MyIterationListenerForControlerConfig2.class);
	
	private MyEventHandler3 eventHandler;
	
	/**
	 * On startup the event handler is initialized and added to the controler
	 */
	public void notifyStartup(StartupEvent event){
		log.info("Adding event handler...");
		// init event handler
		this.eventHandler = new MyEventHandler3();
		// add eventhandler to controler
		event.getControler().getEvents().addHandler( this.eventHandler );
	}
	
	/**
	 * At the end of the last iteration some results are presented...
	 */
	public void notifyIterationEnds(IterationEndsEvent event){
		if(event.getIteration() == event.getControler().getLastIteration()){
			log.info("This will be the last iteration !!! Iteration = " + event.getIteration() );
			
			log.info("Showing some results for travel time (tt):");
			for(int i = 0; i < 24; i++)
				log.info("tt at " + i + "h: " + this.eventHandler.getTravelTime(i));
		}
	}
}

