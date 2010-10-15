/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.vehDelayHandler;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;

/**
 * Collects planned and realized departures at one stop.
 * List of planned and realized departures is not guaranteed to be synchronized.
 * 
 * @author aneumann
 */
public class VehDelayAtStopContainer {
	
	private final Id stopId;
	
	// Not possible, due to missing line and route information in departure event
	// Information may be derived from transit schedule
	private final Id lineId;
	private final Id routeId;
	
	// Could be double[] ???
	private ArrayList<Double> plannedDepartures = new ArrayList<Double>();
	private ArrayList<Double> realizedDepartures = new ArrayList<Double>();
	
	public VehDelayAtStopContainer(Id stopId, Id lineId, Id routeId){
		this.stopId = stopId;
		this.lineId = lineId;
		this.routeId = routeId;
	}
	
	public void addDepartureEvent(VehicleDepartsAtFacilityEvent departureEvent){
		this.plannedDepartures.add(new Double(departureEvent.getTime() - departureEvent.getDelay()));
		this.realizedDepartures.add(new Double(departureEvent.getTime()));
	}
	
	protected ArrayList<Double> getPlannedDepartures() {
		return this.plannedDepartures;
	}

	protected ArrayList<Double> getRealizedDepartures() {
		return this.realizedDepartures;
	}	
	
	@Override
	public String toString() {
		return "Stop: " + this.stopId + ", Line: " + this.lineId + ", Route: " + this.routeId + ", # planned Departures: " + this.plannedDepartures.size() + ", # realized Departures: " + this.realizedDepartures.size();
	}

}
