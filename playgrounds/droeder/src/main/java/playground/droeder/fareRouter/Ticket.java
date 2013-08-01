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



/**
 * @author droeder
 *
 */
interface Ticket {

	/**
	 * @return the fare on the first request or zero otherwise
	 */
	public Double getFare();

	/**
	 * @param time
	 * @return
	 */
	public boolean timeExpired(Double time);

	/**
	 * @param lineId
	 * @return
	 */
	public boolean lineForbidden(Id lineId);

	/**
	 * @param routeId
	 * @return
	 */
	public boolean routeForbidden(Id routeId);

	/**
	 * @param travelledDistance
	 * @return
	 */
	public boolean distanceExpired(Double travelledDistance);
	
	
	/**
	 * 
	 * @return the original fare of the ticket
	 */
	public Double getOriginalFare();

	/**
	 * 
	 * @return the name of the ticketType
	 */
	public String getType();
	
}

