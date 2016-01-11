/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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

/**
 * 
 */

package playground.vsp.congestion.controler;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author ihab
 *
 */

public class CongestionAnalysisControlerListener implements StartupListener {
	private final Logger log = Logger.getLogger(CongestionAnalysisControlerListener.class);

	private EventHandler congestionHandler;
	
	/**
	 * @param handler must be one of the implementation for congestion pricing 
	 */
	public CongestionAnalysisControlerListener(EventHandler congestionHandler){
		this.congestionHandler = congestionHandler;
		log.info("Congestion effects are only computed for analysis purposes. They are not internalized!");
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getServices().getEvents();
		eventsManager.addHandler(this.congestionHandler);
	}
}
