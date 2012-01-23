/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionInternalizationModule.java
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
package playground.benjamin.internalization;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.AgentMoneyEventImpl;

import playground.benjamin.emissions.events.ColdEmissionEvent;
import playground.benjamin.emissions.events.ColdEmissionEventHandler;
import playground.benjamin.emissions.events.WarmEmissionEvent;
import playground.benjamin.emissions.events.WarmEmissionEventHandler;


/**
 * @author benjamin
 *
 */
public class EmissionInternalizationHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionInternalizationHandler.class);

	EventsManager eventsManager;
	EmissionCostModule costModule = new EmissionCostModule();

	public EmissionInternalizationHandler(Controler controler) {
		this.eventsManager = controler.getEvents();
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id personId = event.getVehicleId();
		double time = event.getTime();
		double warmEmissionCosts = costModule.calculateWarmEmissionCosts(event.getWarmEmissions());
		double amount2Pay = - warmEmissionCosts;
		
		Event moneyEvent = new AgentMoneyEventImpl(time, personId, amount2Pay);
		
		eventsManager.processEvent(moneyEvent);
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id personId = event.getVehicleId();
		double time = event.getTime();
		double coldEmissionCosts = costModule.calculateColdEmissionCosts(event.getColdEmissions());
		double amount2Pay = - coldEmissionCosts;
		
		Event moneyEvent = new AgentMoneyEventImpl(time, personId, amount2Pay);
		
		eventsManager.processEvent(moneyEvent);
	}
	
}
