/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyThrowEventHandler.java
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

import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

/**
 * @author Ihab
 *
 */
public class CalculateFareForBusTripHandler implements PersonEntersVehicleEventHandler {

	private final EventsManager events;
	private final double fare;

	public CalculateFareForBusTripHandler(EventsManager events, double fare) {
		this.events = events;
		this.fare = fare;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().toString().contains("person") && event.getVehicleId().toString().contains("bus")){
			double fareForTrip = calculateFare();
			AgentMoneyEvent moneyEvent = new AgentMoneyEvent(event.getTime(), event.getPersonId(), fareForTrip);
			this.events.processEvent(moneyEvent);
		}
	}

	// this method needs to be extended when differentiated fares apply.
	private double calculateFare() {
		return this.fare;
	}
}