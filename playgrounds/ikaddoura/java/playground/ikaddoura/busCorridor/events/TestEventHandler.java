/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorEventHandler.java
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
package playground.ikaddoura.busCorridor.events;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */

public class TestEventHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, AgentMoneyEventHandler, PersonEntersVehicleEventHandler, VehicleDepartsAtFacilityEventHandler {
	
	Scenario scenario;
	
	public TestEventHandler() {
	}
	
	public void reset(int iteration) {
		// wird am Ende jeder Iteration ausgeführt (zum resetten von Variablen...)
	}

	public void handleEvent(LinkLeaveEvent event) { // wird jedes mal bei Verlassen eines Links aufgerufen		
	
//		System.out.println(Time.writeTime(event.getTime(), Time.TIMEFORMAT_HHMMSS) +" Uhr: Link "+event.getLinkId()+ " wurde von "+event.getPersonId()+" verlassen.");
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		System.out.println("Person "+event.getPersonId()+" enters Vehicle "+event.getVehicleId()+".");
//		AgentMoneyEventImpl moneyEventImpl = new AgentMoneyEventImpl(event.getTime(), event.getPersonId(), -2.3);
	
	}

	@Override
	public void handleEvent(AgentMoneyEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		// TODO Auto-generated method stub
		
	}
}
