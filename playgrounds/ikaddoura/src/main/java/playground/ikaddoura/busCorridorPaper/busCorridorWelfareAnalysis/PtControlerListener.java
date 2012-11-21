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

package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.controler.events.StartupEvent;

import org.matsim.core.controler.listener.StartupListener;

/**
 * @author Ihab
 *
 */

public class PtControlerListener implements StartupListener {
	private final static Logger log = Logger.getLogger(InternalControler.class);

	private final double fare;
	private final PtLegHandler ptLegHandler;
	private Scenario scenario;

	public PtControlerListener(Scenario scenario, double fare, PtLegHandler ptLegHandler){
		this.fare = fare;
		this.ptLegHandler = ptLegHandler;
		this.scenario = scenario;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		EventsManager eventsManager = event.getControler().getEvents();
		CalculateFareForBusTripHandler fareCalculator = new CalculateFareForBusTripHandler(eventsManager, this.fare);
		event.getControler().getEvents().addHandler(fareCalculator);
		event.getControler().getEvents().addHandler(ptLegHandler);
	}

}
