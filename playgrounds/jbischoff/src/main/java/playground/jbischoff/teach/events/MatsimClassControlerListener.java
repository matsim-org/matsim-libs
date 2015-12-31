/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
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
package playground.jbischoff.teach.events;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
/**
 * @author jbischoff
 * A basic Controler Listener used in the MATSim class at TU Berlin.
 */
public class MatsimClassControlerListener implements StartupListener, IterationEndsListener {

	MyEventHandler myEventHandler;
	 
	@Override
	public void notifyStartup(StartupEvent event) {
		myEventHandler = new MyEventHandler(); 
		event.getServices().getEvents().addHandler(myEventHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		myEventHandler.printPersonWithHighestWorkingTime();
	}

}