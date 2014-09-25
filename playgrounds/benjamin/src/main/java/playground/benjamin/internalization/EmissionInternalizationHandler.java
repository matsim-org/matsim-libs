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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;

import java.util.Set;



/**
 * @author benjamin
 *
 */
public class EmissionInternalizationHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionInternalizationHandler.class);

	EventsManager eventsManager;
	EmissionCostModule emissionCostModule;
	Set<Id> hotspotLinks;

	public EmissionInternalizationHandler(Controler controler, EmissionCostModule emissionCostModule, Set<Id> hotspotLinks) {
		this.eventsManager = controler.getEvents();
		this.emissionCostModule = emissionCostModule;
		this.hotspotLinks = hotspotLinks;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id linkId = event.getLinkId();
		if(hotspotLinks == null){
			// pricing applies for all links
			calculateWarmEmissionCostsAndThrowEvent(event);
		} else {
			// pricing applies for the current link
			if(hotspotLinks.contains(linkId)){
				calculateWarmEmissionCostsAndThrowEvent(event);
			}
			// pricing applies not for the current link
			else ;
		}
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id linkId = event.getLinkId();
		if(hotspotLinks == null){
			// pricing applies for all links
			calculateColdEmissionCostsAndThrowEvent(event);
		} else {
			// pricing applies for the current link
			if(hotspotLinks.contains(linkId)){
				calculateColdEmissionCostsAndThrowEvent(event);
			}
			// pricing applies not for the current link
			else ;
		}
	}

	private void calculateColdEmissionCostsAndThrowEvent(ColdEmissionEvent event) {
		Id personId = event.getVehicleId();
		double time = event.getTime();
		double coldEmissionCosts = emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions());
		double amount2Pay = - coldEmissionCosts;
		
		Event moneyEvent = new PersonMoneyEvent(time, personId, amount2Pay);
		
		eventsManager.processEvent(moneyEvent);
	}

	private void calculateWarmEmissionCostsAndThrowEvent(WarmEmissionEvent event) {
		Id personId = event.getVehicleId();
		double time = event.getTime();
		double warmEmissionCosts = emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions());
		double amount2Pay = - warmEmissionCosts;
		
		Event moneyEvent = new PersonMoneyEvent(time, personId, amount2Pay);
		
		eventsManager.processEvent(moneyEvent);
	}
	
}
