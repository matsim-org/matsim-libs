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

package playground.ikaddoura.decongestion.handler;

import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;

import com.google.inject.Inject;

import playground.ikaddoura.decongestion.data.DecongestionInfo;


/**
 * 
 * Keeps track of which vehicle is used by which person.
 * 
 * Assumes the taxi driver to be the last one enters the vehicle.
 * 
 * @author ikaddoura
 */

public class PersonVehicleTracker implements PersonEntersVehicleEventHandler {

	@Inject
	private DecongestionInfo congestionInfo;

	@Override
	public void reset(int iteration) {
		this.congestionInfo.getVehicleId2personId().clear();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.congestionInfo.getVehicleId2personId().put(event.getVehicleId(), event.getPersonId());
	}
}

