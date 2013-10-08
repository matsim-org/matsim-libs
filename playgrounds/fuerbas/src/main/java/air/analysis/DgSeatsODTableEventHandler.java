/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSeatsODTable
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
package air.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import air.demand.FlightODRelation;


/**
 * @author dgrether
 *
 */
public class DgSeatsODTableEventHandler implements
VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler{

	private SortedMap<String, SortedMap<String, FlightODRelation>> fromAirport2FlightOdRelMap;
	private Vehicles vehicles;
	private Map<Id, VehicleDepartsAtFacilityEvent> vehDepartsEventsByVehicleId = new HashMap<Id, VehicleDepartsAtFacilityEvent>();
	
	public DgSeatsODTableEventHandler(Vehicles vehicles){
		this.vehicles = vehicles;
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.fromAirport2FlightOdRelMap = new TreeMap<String, SortedMap<String, FlightODRelation>>();
		this.vehDepartsEventsByVehicleId.clear();
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehDepartsEventsByVehicleId.put(event.getVehicleId(), event);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		VehicleDepartsAtFacilityEvent departureEvent = this.vehDepartsEventsByVehicleId.get(event.getVehicleId());
		if (departureEvent != null){
			Vehicle vehicle = this.vehicles.getVehicles().get(event.getVehicleId());
			int seats = vehicle.getType().getCapacity().getSeats() - 1;
			this.addSeats(departureEvent.getFacilityId().toString(), event.getFacilityId().toString(), seats);
		}
	}
	
	private void addSeats(String from, String to, int seats){
		SortedMap<String, FlightODRelation> m = this.fromAirport2FlightOdRelMap.get(from);
		if (m == null){
			m = new TreeMap<String, FlightODRelation>();
			this.fromAirport2FlightOdRelMap.put(from, m);
		}
		FlightODRelation odRel = m.get(to);
		if (odRel == null){
			odRel = new FlightODRelation(from, to, (double) seats);
			m.put(to, odRel);
		}
		else {
			odRel.setNumberOfTrips(odRel.getNumberOfTrips() + seats);
		}
	}

	public SortedMap<String, SortedMap<String, FlightODRelation>> getODSeats() {
		return this.fromAirport2FlightOdRelMap;
	}

	





}
