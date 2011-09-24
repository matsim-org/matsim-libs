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
package playground.ikaddoura.busCorridor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */

public class BusCorridorLinkLeaveEventHandler implements LinkLeaveEventHandler{
	
	Scenario scenario;
	
	public BusCorridorLinkLeaveEventHandler(Scenario scenario) {
		this.scenario = scenario;
	}
	
	public void reset(int iteration) {
	}

	public void handleEvent(LinkLeaveEvent event) { // wird jedes mal bei Verlassen eines Links aufgerufen		
	
		System.out.println(Time.writeTime(event.getTime(), Time.TIMEFORMAT_HHMMSS) +" Uhr: Link "+event.getLinkId()+ " wurde von "+event.getPersonId()+" verlassen.");
	}
}
