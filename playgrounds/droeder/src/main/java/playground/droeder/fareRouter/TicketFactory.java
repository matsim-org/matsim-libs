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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;


/**
 * @author droeder
 *
 */
public interface TicketFactory {
	

	/**
	 * @param routeId
	 * @param lineId
	 * @param person
	 * @param time
	 * @param expectedTravelTime
	 * @return
	 */
	public Ticket createTicket(Id routeId, Id lineId, Person person, Double time, Double expectedTravelTime);
	
	/**
	 * 
	 * @param ticketToUpgrade
	 * @param routeId
	 * @param lineId
	 * @param person
	 * @param time
	 * @param expectedTravelTime
	 * @param travelledDistance
	 * @return either the upgraded ticket or null if no update is possible
	 */
	public Ticket upgrade(Ticket ticketToUpgrade, Id routeId, Id lineId, Person person, Double time, Double expectedTravelTime, Double travelledDistance) ;

}

