/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractTravelTimeCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.trafficmonitoring;

import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.router.util.TravelTimeI;


/**
 * @author laemmel
 */
public abstract class AbstractTravelTimeCalculator implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI, 
		EventHandlerAgentArrivalI, TravelTimeI {


	public AbstractTravelTimeCalculator() {
	}

	/**
	 * Resets the travel times information on all links
	 */
	public abstract void resetTravelTimes();


	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
