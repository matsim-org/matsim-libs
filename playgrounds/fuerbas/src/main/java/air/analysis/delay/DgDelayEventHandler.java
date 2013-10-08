/* *********************************************************************** *
 * project: org.matsim.*
 * DgDelayEventHandler
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
package air.analysis.delay;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;

import air.scenario.oag.DgOagFlight;
import air.scenario.oag.DgOagFlightsData;


/**
 * @author dgrether
 */
public class DgDelayEventHandler implements TransitDriverStartsEventHandler, 
	VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonLeavesVehicleEventHandler {

	private DgOagFlightsData oagFlights;

	private Map<Id, TransitDriverStartsEvent> vehicleIdTransitDriverStartsEventMap  = new HashMap<Id, TransitDriverStartsEvent>();
	private Map<String, DgFlightDelay> flightDesignatorDelayMap = new HashMap<String, DgFlightDelay>();
	
	public DgDelayEventHandler(DgOagFlightsData oagFlights) {
		this.oagFlights = oagFlights;
	}

	public Map<String, DgFlightDelay> getFlightDelaysByFlightDesignatorMap(){
		return this.flightDesignatorDelayMap;
	}
	
	@Override
	public void reset(int iteration) {
		this.vehicleIdTransitDriverStartsEventMap.clear();
		this.flightDesignatorDelayMap.clear();
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		TransitDriverStartsEvent e = this.vehicleIdTransitDriverStartsEventMap.get(event.getVehicleId());
		String flightDesignator = e.getTransitRouteId().toString().split("_")[2];
		DgOagFlight flight = this.oagFlights.getFlightDesignatorFlightMap().get(flightDesignator);
		if (event.getFacilityId().toString().compareTo(flight.getOriginCode()) == 0) {
			double delay = event.getTime() - flight.getDepartureTime();
			DgFlightDelay d = new DgFlightDelay(flight);
			d.setDepartureDelay(delay);
			d.setActualDepartureTime(event.getTime());
			this.flightDesignatorDelayMap.put(flightDesignator, d);
		}
	}

	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		TransitDriverStartsEvent e = this.vehicleIdTransitDriverStartsEventMap.get(event.getVehicleId());
		String flightDesignator = e.getTransitRouteId().toString().split("_")[2];
		DgOagFlight flight = this.oagFlights.getFlightDesignatorFlightMap().get(flightDesignator);
		if (event.getFacilityId().toString().compareTo(flight.getDestinationCode()) == 0){
			double delay = event.getTime() - flight.getDepartureTime() - flight.getScheduledDuration();
			DgFlightDelay d = this.flightDesignatorDelayMap.get(flightDesignator); //if this is not in the map, there is something wrong
			d.setActualArrivalTime(event.getTime());
			d.setArrivalDelay(delay);
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehicleIdTransitDriverStartsEventMap.put(event.getVehicleId(), event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.vehicleIdTransitDriverStartsEventMap.remove(event.getVehicleId());
	}


	

}
