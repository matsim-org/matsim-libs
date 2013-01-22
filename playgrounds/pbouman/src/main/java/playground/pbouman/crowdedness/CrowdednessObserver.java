/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.pbouman.crowdedness;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vehicles.Vehicles;

/**
 * @author nagel
 *
 */
public class CrowdednessObserver implements
		VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {
	
	private EventsManager ev;
	private Scenario sc;
	private Vehicles vehs;

	public CrowdednessObserver( Scenario sc, EventsManager ev ) {
		this.sc = sc ;
		this.ev = ev ;
		this.vehs = ((ScenarioImpl)this.sc).getVehicles() ;
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		ev.processEvent(new PersonCrowdednessEvent(event.getTime()) ) ;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id vehId = event.getVehicleId() ;
		this.vehs.getVehicles().get(vehId) ;
		// get size of vehicl etc.

	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
