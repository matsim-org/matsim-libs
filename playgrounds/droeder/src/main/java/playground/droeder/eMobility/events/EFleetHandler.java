/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.eMobility.events;

import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.droeder.eMobility.fleet.EFleet;

/**
 * @author droeder
 *
 */
public class EFleetHandler implements LinkEnterEventHandler, LinkLeaveEventHandler{
	
	private EFleet fleet;

	public EFleetHandler(EFleet fleet){
		this.fleet = fleet;
	}
	
	public EFleet getFleet(){
		return this.fleet;
	}

	@Override
	public void reset(int iteration) {
		System.err.println("fleet in EFleetHandler: " + fleet);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.fleet.processEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.fleet.processEvent(event);
	}

}
