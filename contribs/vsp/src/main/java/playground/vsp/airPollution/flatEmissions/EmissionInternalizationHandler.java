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
package playground.vsp.airPollution.flatEmissions;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;


/**
 * @author benjamin
 *
 */
public class EmissionInternalizationHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	EventsManager eventsManager;
	EmissionCostModule emissionCostModule;
	Set<Id<Link>> hotspotLinks;

	public EmissionInternalizationHandler(MatsimServices controler, EmissionCostModule emissionCostModule, Set<Id<Link>> hotspotLinks) {
		this.eventsManager = controler.getEvents();
		this.emissionCostModule = emissionCostModule;
		this.hotspotLinks = hotspotLinks;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Link> linkId = event.getLinkId();
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
		Id<Link> linkId = event.getLinkId();
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
		Id<Person> personId = Id.create(event.getVehicleId(), Person.class);
		double time = event.getTime();
		double coldEmissionCosts = emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions());
		double amount2Pay = - coldEmissionCosts;
		
		Event moneyEvent = new PersonMoneyEvent(time, personId, amount2Pay);
		eventsManager.processEvent(moneyEvent);
		
		PersonLinkMoneyEvent moneyLinkEvent = new PersonLinkMoneyEvent(time, personId, event.getLinkId(), amount2Pay, time, "airPollution");
		eventsManager.processEvent(moneyLinkEvent);
	}

	private void calculateWarmEmissionCostsAndThrowEvent(WarmEmissionEvent event) {
		Id<Person> personId = Id.create(event.getVehicleId(), Person.class);
		double time = event.getTime();
		double warmEmissionCosts = emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions());
		double amount2Pay = - warmEmissionCosts;
		
		Event moneyEvent = new PersonMoneyEvent(time, personId, amount2Pay);
		eventsManager.processEvent(moneyEvent);
		
		PersonLinkMoneyEvent moneyLinkEvent = new PersonLinkMoneyEvent(time, personId, event.getLinkId(), amount2Pay, time, "airPollution");
		eventsManager.processEvent(moneyLinkEvent);
	}
	
}
