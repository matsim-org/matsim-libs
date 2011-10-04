/* *********************************************************************** *
 * project: org.matsim.*
 * FareEventHandler.java
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
package playground.ikaddoura.busCorridor.version3_controlerListener;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

/**
 * @author Ihab
 *
 */
public class FareEventHandler implements PersonEntersVehicleEventHandler {

	private EventsManager events;
	private double fare;
	private Population population;
	public static final Logger logger = Logger.getLogger(FareEventHandler.class);

	public FareEventHandler(EventsManager events, Population population) {
		this.events = events;
		this.population = population;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.fare = calculateFare(event, population);
		AgentMoneyEvent moneyEvent = new AgentMoneyEventImpl(event.getTime(), event.getPersonId(), fare);
		logger.info("Person enters Vehicle --> AgentMoneyEvent thrown.");
		this.events.processEvent(moneyEvent); //schickt das MoneyEvent an den EventManager
	}

	private double calculateFare(PersonEntersVehicleEvent event, Population population) {
		
		// Berechnung des Fahrpreises abhängig von mit/ohne Zeitkarte: event --> personId --> Plan (hat Zeitkarte: ID: z1_ID/hat keine Zeitkarte: ID: z0_ID)
		// Berechnung des Fahrpreises abhängig vom Alter: event --> personID --> Plan (unter 6 Jahre: fare = 0 / über 6 Jahre: fare = ...)
		// Berechnung des Fahrpreises abhängig von Distanzen etc.: event --> personId --> Plan --> durchschnittliche Distanz der Legs (unter 2km --> KurzstreckenPreis, über 2km --> Normalpreis)
		double fare = -2.3;
		return fare;
	}

}
