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
package playground.benjamin.scenarios.munich.exposure;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;


/**
 * @author benjamin
 *
 */
public class EmissionResponsibilityInternalizationHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	EventsManager eventsManager;
	EmissionResponsibilityCostModule emissionResponsibilityCostModule;

	public EmissionResponsibilityInternalizationHandler(MatsimServices controler, EmissionResponsibilityCostModule emissionCostModule) {
		this.eventsManager = controler.getEvents();
		this.emissionResponsibilityCostModule = emissionCostModule;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		calculateWarmEmissionCostsAndThrowEvent(event);
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
			calculateColdEmissionCostsAndThrowEvent(event);
	}

	private void calculateColdEmissionCostsAndThrowEvent(ColdEmissionEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		double time = event.getTime();
		double coldEmissionCosts = emissionResponsibilityCostModule.calculateColdEmissionCosts(event.getColdEmissions(), event.getLinkId(), time);
		double amount2Pay = - coldEmissionCosts;
		
		Event moneyEvent = new PersonMoneyEvent(time, personId, amount2Pay);
		
		eventsManager.processEvent(moneyEvent);
	}

	private void calculateWarmEmissionCostsAndThrowEvent(WarmEmissionEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		double time = event.getTime();
		double warmEmissionCosts = emissionResponsibilityCostModule.calculateWarmEmissionCosts(event.getWarmEmissions(), event.getLinkId(), time);
		double amount2Pay = - warmEmissionCosts;
		
		Event moneyEvent = new PersonMoneyEvent(time, personId, amount2Pay);
		
		eventsManager.processEvent(moneyEvent);
	}
	
}
