/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.fareRouter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author droeder
 *
 */
public class TicketMachineImpl implements TicketMachine{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(TicketMachineImpl.class);
	private TicketFactory ticketFactory;

	public TicketMachineImpl(TicketFactory ticketFactory) {
		this.ticketFactory = ticketFactory;
	}

	
	@Override
	public Ticket getNewTicket(Id routeId, Id lineId, Person person, Double time, Double expectedTravelTime) {
		return this.ticketFactory.createTicket(routeId, lineId, person, time, expectedTravelTime);
	}
	
	@Override
	public boolean isValid(Ticket t, Id routeId, Id lineId, Double time, Double expectedTravelTime, Double travelledDistance) {
		if(t.timeExpired(time + expectedTravelTime)) return false;
		if(t.distanceExpired(travelledDistance)) return false;
		if(t.lineForbidden(lineId)) return false;
		if(t.routeForbidden(routeId)) return false;
		return true;
	}


	@Override
	public Ticket upgrade(Ticket ticketToUpgrade, Id routeId, Id lineId, Person person, Double time, Double expectedTravelTime, Double travelledDistance) {
		return this.ticketFactory.upgrade(ticketToUpgrade, routeId, lineId, person, expectedTravelTime, time, travelledDistance);
	}
}


