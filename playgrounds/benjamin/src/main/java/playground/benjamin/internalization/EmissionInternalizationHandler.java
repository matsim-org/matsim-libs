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
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;


/**
 * @author benjamin
 *
 */
public class EmissionInternalizationHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionInternalizationHandler.class);

	private final EventsManager eventsManager;
//	Map<Id, Double> personId2emissionCosts = new HashMap<Id, Double>();

	public EmissionInternalizationHandler(Controler controler) {
		this.eventsManager = controler.getEvents();
	}

	@Override
	public void reset(int iteration) {
//		personId2emissionCosts.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id personId = event.getVehicleId();
		double time = event.getTime();
		double warmEmissionCosts = calculateWarmEmissionCosts(event);
		double amount2Pay = - warmEmissionCosts;
		
		Event moneyEvent = new AgentMoneyEventImpl(time, personId, amount2Pay);
//		Event moneyEvent = new AgentMoneyEventImpl(time, personId, 0);
		eventsManager.processEvent(moneyEvent);
		
//		if(personId2emissionCosts.get(personId) != null){
//			double emissionCostsSoFar = personId2emissionCosts.get(personId);
//			emissionCosts = emissionCostsSoFar + emissionCosts;
//			logger.info("setting emission costs for person " + personId + " from " + emissionCostsSoFar + " to " + emissionCosts);
//			personId2emissionCosts.put(personId, emissionCosts);
//		} else {
//			logger.info("initialising emission account for person " + personId + " with " + emissionCosts);
//			personId2emissionCosts.put(personId, emissionCosts);
//		}
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id personId = event.getVehicleId();
		double time = event.getTime();
		double coldEmissionCosts = calculateColdEmissionCosts(event);
		double amount2Pay = - coldEmissionCosts;
		
		Event moneyEvent = new AgentMoneyEventImpl(time, personId, amount2Pay);
//		Event moneyEvent = new AgentMoneyEventImpl(time, personId, 0);
		eventsManager.processEvent(moneyEvent);
		
//		if(personId2emissionCosts.get(personId) != null){
//			double emissionCostsSoFar = personId2emissionCosts.get(personId);
//			emissionCosts = emissionCostsSoFar + emissionCosts;
//			logger.info("setting emission costs for person " + personId + " from " + emissionCostsSoFar + " to " + emissionCosts);
//			personId2emissionCosts.put(personId, emissionCosts);
//		} else {
//			logger.info("initialising emission account for person " + personId + " with " + emissionCosts);
//			personId2emissionCosts.put(personId, emissionCosts);
//		}
	}
	
	private double calculateColdEmissionCosts(ColdEmissionEvent event) {
		double coldEmissionCosts = 5.0;
		
		for(ColdPollutant cp : event.getColdEmissions().keySet()){
			
		}
		
		return coldEmissionCosts;
	}

	private double calculateWarmEmissionCosts(WarmEmissionEvent event) {
		double warmEmissionCosts = 10.0;
		
		for(WarmPollutant wp : event.getWarmEmissions().keySet()){
			
		}
		
		return warmEmissionCosts;
	}

//	public Map<Id, Double> getPersonId2EmissionCosts(){
//		return personId2emissionCosts;
//	}

}
