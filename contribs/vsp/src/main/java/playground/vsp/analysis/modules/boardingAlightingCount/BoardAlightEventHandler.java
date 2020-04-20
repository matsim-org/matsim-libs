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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.PtConstants;

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
									// we want to know if the trip starts or if it is a line-switch
									ActivityStartEventHandler, ActivityEndEventHandler,
									// we don't want to count the transitDrivers, but we need to know which vehicles they are using
									TransitDriverStartsEventHandler{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BoardAlightEventHandler.class);
	private Map<Id, VehicleLocation> transitVehicles;
	private List<Id> drivers;
	private int interval;
	private Counts alightUnclassified;
	private Counts boardUnclassified;
	private Integer maxSlice = 0;
	private Counts boardStart;
	private Counts boardSwitch;
	private Counts alightSwitch;
	private Counts alightEnd;
	
	private Map<Id, LinkedList<String>> endAct;
	private Map<Id, Tuple<Id, Double>> leaveVehicle;
	private Map<Id, Double> stops;
	
	

	public BoardAlightEventHandler(int interval) {
		this.transitVehicles = new HashMap<Id, VehicleLocation>();
		this.drivers = new ArrayList<Id>();
		this.interval= interval;
		this.boardUnclassified = new Counts();
		this.boardStart = new Counts();
		this.boardSwitch = new Counts();
		this.alightUnclassified = new Counts();
		this.alightSwitch = new Counts();
		this.alightEnd = new Counts();
		this.endAct = new HashMap<Id, LinkedList<String>>();
		this.leaveVehicle = new HashMap<Id, Tuple<Id,Double>>();
		this.stops = new HashMap<Id, Double>();
	}

	@Override
	public void reset(int iteration) {
		//do nothing
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		LinkedList<String> acts = this.endAct.get(event.getPersonId());
		if(acts == null){
			acts = new LinkedList<String>();
		}
		acts.addLast(event.getActType());
		this.endAct.put(event.getPersonId(), acts);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!(event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE))){
			// the trip is finished
			this.endAct.put(event.getPersonId(), null);
			if(this.leaveVehicle.containsKey(event.getPersonId())){
				Tuple<Id, Double> stopId2Time= this.leaveVehicle.remove(event.getPersonId());
				this.increase(this.alightEnd, stopId2Time.getFirst(), stopId2Time.getSecond());
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// dont count the driver
		if(this.drivers.contains(event.getPersonId())) return;
		// only count boarding/alighting for transit
		if(!this.transitVehicles.keySet().contains(event.getVehicleId())) return;
		Id stopId = this.transitVehicles.get(event.getVehicleId()).getLocationId();
		increase(this.alightUnclassified, stopId, event .getTime());
		// as the stopId of the vehicle might change before the person arrives at the next facility we store it here!
		this.leaveVehicle.put(event.getPersonId(), new Tuple<Id, Double>(stopId, event.getTime()));
	}


	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// dont count the driver
		if(this.drivers.contains(event.getPersonId())) return;
		// only count boarding/alighting for transit
		if(!this.transitVehicles.keySet().contains(event.getVehicleId())) return;
		Id stopId = this.transitVehicles.get(event.getVehicleId()).getLocationId();
		increase(this.boardUnclassified, stopId, event .getTime());
		//check if enter first time or if it is a line switch
		String act = this.endAct.get(event.getPersonId()).pollFirst();
		if(act.equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// agents switchs vehicles
			increase(this.boardSwitch, stopId, event.getTime());
		}else{
			// agent boards the first time on this trip!
			increase(this.boardStart, stopId, event.getTime());
		}
		//the persons switches vehicles/lines
		if(this.leaveVehicle.containsKey(event.getPersonId())){
			Tuple<Id, Double> stopId2Time= this.leaveVehicle.remove(event.getPersonId());
			this.increase(this.alightSwitch, stopId2Time.getFirst(), stopId2Time.getSecond());
		}
	}
	

	private void increase(Counts counts, Id stopId, Double time){
		//create a new count
		Count count = counts.createAndAddCount(stopId, stopId.toString());
		if(count == null){
			//or get the old one if there is one
			count = counts.getCount(stopId);
		}
		Integer slice = (int) (time / this.interval) + 1;
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
		Double count = this.stops.get(event.getFacilityId());
		if(count == null){
			count = 0.;
		}
		count++;
		this.stops.put(event.getFacilityId(), count);
	}
	
	public Map<Id, Double> getStopToDepartures(){
		return this.stops;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.drivers.add(event.getDriverId());
		this.transitVehicles.put(event.getVehicleId(), new VehicleLocation(event.getVehicleId()));
	}
	
	public SortedMap<String, Counts> getClassification2Counts(){
		SortedMap<String, Counts> counts = new TreeMap<String, Counts>();
		counts.put("boardUnclassified", this.boardUnclassified);
		counts.put("boardStart", this.boardStart);
		counts.put("boardSwitch", this.boardSwitch);
		counts.put("alightUnclassified", this.alightUnclassified);
		counts.put("alightEnd", this.alightEnd);
		counts.put("alightSwitch", this.alightSwitch);
		return counts;
	}
	
	public Integer getMaxTimeSlice(){
		return this.maxSlice;
	}

}

