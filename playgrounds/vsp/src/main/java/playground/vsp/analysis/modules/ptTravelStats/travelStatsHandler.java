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

package playground.vsp.analysis.modules.ptTravelStats;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;


/**
 * @authors aneumann, fuerbas, droeder
 *
 */

public class travelStatsHandler implements LinkEnterEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private static final Logger log = Logger.getLogger(travelStatsHandler.class);
	
	private final ScenarioImpl scenario;
	private final Double interval;
	
	private final Map<Id, String> transitVehicleId2transportMode;
	private final Set<Id> transitVehicleDrivers;
	private final Map<Id, Integer> transitVehicle2CurrentPaxCountMap;
	
	private final HashMap<String, Counts> mode2CountsVehicles;
	private final HashMap<String, Counts> mode2CountsCapacity;
	private final HashMap<String, Counts> mode2CountsCapacity_m;
	private final HashMap<String, Counts> mode2CountsPax;
	private final HashMap<String, Counts> mode2CountsPax_m;
	
	private Integer maxSlice = 0;
	
	public travelStatsHandler(Scenario scenario, Double interval) {
		this.scenario = (ScenarioImpl) scenario;
		this.interval = interval;

		this.transitVehicleId2transportMode = new HashMap<Id, String>();
		this.transitVehicleDrivers = new TreeSet<Id>();
		this.transitVehicle2CurrentPaxCountMap = new HashMap<Id, Integer>();
		
		this.mode2CountsVehicles = new HashMap<String, Counts>();
		this.mode2CountsCapacity = new HashMap<String, Counts>();
		this.mode2CountsCapacity_m = new HashMap<String, Counts>();
		this.mode2CountsPax = new HashMap<String, Counts>();
		this.mode2CountsPax_m = new HashMap<String, Counts>();
	}
	
	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitVehicleDrivers.add(event.getDriverId());
		this.transitVehicle2CurrentPaxCountMap.put(event.getVehicleId(), new Integer(0));
		
		if(!this.scenario.getTransitSchedule().getTransitLines().containsKey(event.getTransitLineId())) return;
		
		TransitLine line = this.scenario.getTransitSchedule().getTransitLines().get(event.getTransitLineId());
		if(line == null )log.debug(event.getTransitLineId());
		
		TransitRoute route = line.getRoutes().get(event.getTransitRouteId());
		if(route == null) {
			log.debug("route " + event.getTransitRouteId() + " is null on TransitLine " + event.getTransitLineId()); 
			return;
		}
		
		String mode = route.getTransportMode();
		this.transitVehicleId2transportMode.put(event.getVehicleId(), mode);
		
		if(!this.mode2CountsVehicles.containsKey(mode)){
			this.mode2CountsVehicles.put(mode, new Counts());
		}		
		if(!this.mode2CountsCapacity.containsKey(mode)){
			this.mode2CountsCapacity.put(mode, new Counts());
		}
		if(!this.mode2CountsCapacity_m.containsKey(mode)){
			this.mode2CountsCapacity_m.put(mode, new Counts());
		}
		if(!this.mode2CountsPax.containsKey(mode)){
			this.mode2CountsPax.put(mode, new Counts());
		}
		if(!this.mode2CountsPax_m.containsKey(mode)){
			this.mode2CountsPax_m.put(mode, new Counts());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		//handle only pt-Vehicles!
		if(this.transitVehicle2CurrentPaxCountMap.containsKey(event.getVehicleId())){
			
			String mode = this.transitVehicleId2transportMode.get(event.getVehicleId());
			
			//create the counts if none exist
			Count countVehicles = this.mode2CountsVehicles.get(mode).createCount(event.getLinkId(), event.getLinkId().toString());
			Count countCapacity = this.mode2CountsCapacity.get(mode).createCount(event.getLinkId(), event.getLinkId().toString());
			Count countCapacity_m = this.mode2CountsCapacity_m.get(mode).createCount(event.getLinkId(), event.getLinkId().toString());
			Count countPax = this.mode2CountsPax.get(mode).createCount(event.getLinkId(), event.getLinkId().toString());
			Count countPax_m = this.mode2CountsPax_m.get(mode).createCount(event.getLinkId(), event.getLinkId().toString());
			
			if(countVehicles == null){
				//get existing counts
				countVehicles = this.mode2CountsVehicles.get(mode).getCount(event.getLinkId());
				countCapacity = this.mode2CountsCapacity.get(mode).getCount(event.getLinkId());
				countCapacity_m = this.mode2CountsCapacity_m.get(mode).getCount(event.getLinkId());
				countPax = this.mode2CountsPax.get(mode).getCount(event.getLinkId());
				countPax_m = this.mode2CountsPax_m.get(mode).getCount(event.getLinkId());
			}else{
				//we always want to start with hour zero
				countVehicles.createVolume(0, 0.);
				countCapacity.createVolume(0, 0.);
				countCapacity_m.createVolume(0, 0.);
				countPax.createVolume(0, 0.);
				countPax_m.createVolume(0, 0.);
			}

			this.increaseCount(countVehicles, event.getTime(), 1);
			
			double vehCapacity = getCapacityForVehicle(event.getVehicleId()) - 1; // As of Apr 2013, the dirver take one seat
			this.increaseCount(countCapacity, event.getTime(), vehCapacity);
			
			double vehCapacity_m = vehCapacity * this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			this.increaseCount(countCapacity_m, event.getTime(), vehCapacity_m);
			
			double pax = this.transitVehicle2CurrentPaxCountMap.get(event.getVehicleId());
			this.increaseCount(countPax, event.getTime(), pax);
			
			double pax_m = pax * this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			this.increaseCount(countPax_m, event.getTime(), pax_m);
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// add a passenger to the vehicle counts data, but ignore every non pt-vehicle and every driver
		if(this.transitVehicle2CurrentPaxCountMap.keySet().contains(event.getVehicleId())){
			if(!this.transitVehicleDrivers.contains(event.getPersonId())){
				// transit vehicle, but not the driver - increase by one
				this.transitVehicle2CurrentPaxCountMap.put(event.getVehicleId(), this.transitVehicle2CurrentPaxCountMap.get(event.getVehicleId()) + 1);
			}
		}	
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// substract a passenger to the vehicle counts data, but ignore every non pt-vehicle and every driver
		if(this.transitVehicle2CurrentPaxCountMap.keySet().contains(event.getVehicleId())){
			if(!this.transitVehicleDrivers.contains(event.getPersonId())){
				// transit vehicle, but not the driver - decrease by one
				this.transitVehicle2CurrentPaxCountMap.put(event.getVehicleId(), this.transitVehicle2CurrentPaxCountMap.get(event.getVehicleId()) - 1);
			}
		}	
	}

	private void increaseCount(Count count, double time, double amount) {
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
		v.setValue(v.getValue() + amount);
	}
	
	private double getCapacityForVehicle(Id vehicleId) {
		int vehSeats = this.scenario.getVehicles().getVehicles().get(vehicleId).getType().getCapacity().getSeats();
		int vehStand = this.scenario.getVehicles().getVehicles().get(vehicleId).getType().getCapacity().getStandingRoom();
		return vehSeats + vehStand;
	}

	public int getMaxTimeSlice() {
		return this.maxSlice;
	}
	
	public HashMap<String, Counts> getMode2CountsVolume() {
		return this.mode2CountsVehicles;
	}

	public HashMap<String, Counts> getMode2CountsCapacity() {
		return this.mode2CountsCapacity;
	}

	public HashMap<String, Counts> getMode2CountsCapacity_m() {
		return this.mode2CountsCapacity_m;
	}

	public HashMap<String, Counts> getMode2CountsPax() {
		return this.mode2CountsPax;
	}

	public HashMap<String, Counts> getMode2CountsPax_m() {
		return this.mode2CountsPax_m;
	}
}
