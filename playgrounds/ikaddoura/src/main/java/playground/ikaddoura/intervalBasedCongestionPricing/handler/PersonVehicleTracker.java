/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.intervalBasedCongestionPricing.handler;

import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;

import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionInfo;

/**
 * 
 * Keeps track of which vehicle is used by which person.
 * 
 * @author ikaddoura
 */

public class PersonVehicleTracker implements PersonEntersVehicleEventHandler {

	private final CongestionInfo congestionInfo;
	
	public PersonVehicleTracker(CongestionInfo congestionInfo) {
		this.congestionInfo = congestionInfo;
	}

	@Override
	public void reset(int iteration) {
		this.congestionInfo.getVehicleId2personId().clear();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.congestionInfo.getVehicleId2personId().put(event.getVehicleId(), event.getPersonId());
	}
}

