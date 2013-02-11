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

package playground.ikaddoura.optimization;

import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.controler.events.StartupEvent;

import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ikaddoura.optimization.handler.ConstantFareHandler;
import playground.ikaddoura.optimization.handler.MarginalCostFareHandler;
import playground.ikaddoura.optimization.handler.PtLegHandler;

/**
 * @author Ihab
 *
 */

public class OptControlerListener implements StartupListener {

	private final double fare;
	private final PtLegHandler ptScoringHandler;
	private final ScenarioImpl scenario;

	public OptControlerListener(double fare, PtLegHandler ptLegHandler, ScenarioImpl scenario){
		this.fare = fare;
		this.ptScoringHandler = ptLegHandler;
		this.scenario = scenario;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getControler().getEvents();
		
		ConstantFareHandler fareCalculator = new ConstantFareHandler(eventsManager, this.fare);
		MarginalCostFareHandler mcFareCalculator = new MarginalCostFareHandler(eventsManager, scenario);

		event.getControler().getEvents().addHandler(fareCalculator);
		event.getControler().getEvents().addHandler(mcFareCalculator);
		
		event.getControler().getEvents().addHandler(ptScoringHandler);
	}

}
