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
package playground.vsp.analysis.modules.boardingAlightingCount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.vsp.analysis.modules.boardingAlightingCount.utils.VehicleLocation;


/**
 * @author droeder
 *
 */
public class BoardAlightEventHandler implements 
									// we need to know the position of the vehicle
									VehicleArrivesAtFacilityEventHandler, 
									// we want to be notified when agents enter/leave vehicles
									PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
									// we don't want to count the transitDrivers, but we need to know which vehicles they are using
									TransitDriverStartsEventHandler{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BoardAlightEventHandler.class);
	private Map<Id, VehicleLocation> transitVehicles;
	private List<Id> drivers;
	private int interval;
	private Counts alight;
	private Counts board;
	private Integer maxSlice = 0;
	
	

	public BoardAlightEventHandler(int interval) {
		this.transitVehicles = new HashMap<Id, VehicleLocation>();
		this.drivers = new ArrayList<Id>();
		this.interval= interval;
		this.board = new Counts();
		this.alight = new Counts();
	}

	@Override
	public void reset(int iteration) {
		//do nothing
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// dont count the driver
		if(this.drivers.contains(event.getPersonId())) return;
		// only count boarding/alighting for transit
		if(!this.transitVehicles.keySet().contains(event.getVehicleId())) return;
		Id stopId = this.transitVehicles.get(event.getVehicleId()).getLocationId();
		//create a new count
		Count count = this.alight.createCount(stopId, stopId.toString());
		if(count == null){
			//or get the old one if there is one
			count = this.alight.getCount(stopId);
		}
		this.increase(count, event.getTime());		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// dont count the driver
		if(this.drivers.contains(event.getPersonId())) return;
		// only count boarding/alighting for transit
		if(!this.transitVehicles.keySet().contains(event.getVehicleId())) return;
		Id stopId = this.transitVehicles.get(event.getVehicleId()).getLocationId();
		//create a new count
		Count count = this.board.createCount(stopId, stopId.toString());
		if(count == null){
			//or get the old one if there is one
			count = this.board.getCount(stopId);
		}
		this.increase(count, event.getTime());
	}
	
	private void increase(Count count, Double time){
		Integer slice = (int) (time / this.interval);
		if(slice > this.maxSlice){
			this.maxSlice = slice;
		}
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + 1);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(this.transitVehicles.containsKey(event.getVehicleId())){
			this.transitVehicles.get(event.getVehicleId()).setLocationId(event.getFacilityId());
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.drivers.add(event.getDriverId());
		this.transitVehicles.put(event.getVehicleId(), new VehicleLocation(event.getVehicleId()));
	}
	
	public Counts getBoarding(){
		return this.board;
	}
	
	public Counts getAlight(){
		return this.alight;
	}
	
	public Integer getMaxTimeSlice(){
		return this.maxSlice;
	}
}

