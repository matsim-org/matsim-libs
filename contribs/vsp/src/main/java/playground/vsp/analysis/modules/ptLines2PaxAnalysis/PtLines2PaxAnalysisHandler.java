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

package playground.vsp.analysis.modules.ptLines2PaxAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.AnalysisVehicle;

/**
 * @author sfuerbas after droeder
 * 
 */

public class PtLines2PaxAnalysisHandler implements
		// we need to know the position of the vehicle and when it departs
		VehicleArrivesAtFacilityEventHandler,
		VehicleDepartsAtFacilityEventHandler,
		// we want to be notified when agents enter/leave vehicles
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		// we don't want to count the transitDrivers, but we need to know which
		// vehicles they are using
		TransitDriverStartsEventHandler {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtLines2PaxAnalysisHandler.class);
	private HashMap<Id, TransitLines2PaxCounts> linesPaxCounts;
	private Map<Id, AnalysisVehicle> transitVehicles;
	private List<Id> drivers;
	private Vehicles vehicles;

	public PtLines2PaxAnalysisHandler(double interval, int maxSlice,
			Map<Id<TransitLine>, TransitLine> lines, Vehicles vehicles) {
		this.drivers = new ArrayList<Id>();
		this.transitVehicles = new HashMap<Id, AnalysisVehicle>();
		this.linesPaxCounts = new HashMap<Id, TransitLines2PaxCounts>();
		this.vehicles = vehicles;
		for (TransitLine l : lines.values()) {
			this.linesPaxCounts.put(l.getId(), new TransitLines2PaxCounts(l, interval, maxSlice));
		}
	}

	
	public HashMap<Id, TransitLines2PaxCounts> getLinesPaxCounts() {
		return linesPaxCounts;
	}

	@Override
	public void reset(int iteration) {
		// do nothing
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.drivers.add(event.getDriverId());
		Vehicle v = this.vehicles.getVehicles().get(event.getVehicleId());
		double seats = (v.getType().getCapacity().getSeats() == null) ? 0 : v
				.getType().getCapacity().getSeats();
		double standing = (v.getType().getCapacity().getStandingRoom() == null) ? 0
				: v.getType().getCapacity().getStandingRoom();
		this.transitVehicles.put(
				event.getVehicleId(),
				new AnalysisVehicle(v.getId(), null, seats + standing, event
						.getTransitLineId(), event.getTransitRouteId()));
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// dont count the driver
		if (this.drivers.contains(event.getPersonId())) {
			// but finish his route and remove him and the vehicle
			this.drivers.remove(event.getPersonId());
		} else {
			// only count boarding/alighting for transit
			if (!this.transitVehicles.keySet().contains(event.getVehicleId()))
				return;
			AnalysisVehicle v = this.transitVehicles.get(event.getVehicleId());
			v.personAlights();
			this.linesPaxCounts.get(v.getLineId()).paxAlighting(v.getLocationId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// dont count the driver
		if (this.drivers.contains(event.getPersonId()))
			return;
		// only count boarding/alighting for transit
		if (!this.transitVehicles.keySet().contains(event.getVehicleId()))
			return;
		AnalysisVehicle v = this.transitVehicles.get(event.getVehicleId());
		v.personBoards();
		this.linesPaxCounts.get(v.getLineId()).paxBoarding(v.getLocationId(), event.getTime());
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		AnalysisVehicle v = this.transitVehicles.get(event.getVehicleId());
		this.linesPaxCounts.get(v.getLineId()).vehicleDeparts(event.getTime(),
				v.getCapacity(), v.getSeatsOccupied(), v.getLocationId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.transitVehicles.get(event.getVehicleId()).setLocationId(
				event.getFacilityId());
	}

}
