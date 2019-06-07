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

package org.matsim.contrib.decongestion.handler;

import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;

import com.google.inject.Inject;

import org.matsim.contrib.decongestion.data.DecongestionInfo;


/**
 * 
 * Keeps track of which vehicle is used by which person.
 * Stores the last person who has entered the vehicle and ignores transit vehicles.
 * 
 * @author ikaddoura
 */

public class PersonVehicleTracker implements PersonEntersVehicleEventHandler, TransitDriverStartsEventHandler {

	@Inject
	private DecongestionInfo congestionInfo;

	@Override
	public void reset(int iteration) {
		this.congestionInfo.getVehicleId2personId().clear();
		this.congestionInfo.getTransitVehicleIDs().clear();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!this.congestionInfo.getTransitVehicleIDs().contains(event.getVehicleId())) {
			this.congestionInfo.getVehicleId2personId().put(event.getVehicleId(), event.getPersonId());
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.congestionInfo.getTransitVehicleIDs().add(event.getVehicleId());
	}
}

