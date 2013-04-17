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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @authors fuerbas, droeder
 *
 */

public class travelStatsHandler implements LinkEnterEventHandler, TransitDriverStartsEventHandler {
	
	private static final Logger log = Logger.getLogger(travelStatsHandler.class);
	private Map<Id, String> vehId2mode;
	private HashMap<String, Counts> mode2CountsVolume;
	private HashMap<String, Counts> mode2CountsCapacity;
	private HashMap<String, Counts> mode2CountsCapacity_m;
	private HashMap<String, Counts> mode2CountsPax;
	private HashMap<String, Counts> mode2CountsPax_m;
	
	private Integer maxSlice = 0;

	private Double interval;

	private Scenario scenario;
	private ScenarioImpl scen;
	private TransitSchedule sched;
	
	
	public travelStatsHandler(Scenario scenario, Double interval) {
		this.scenario = scenario;
		this.scen= (ScenarioImpl) this.scenario;
		this.interval = interval;
		this.sched = this.scenario.getTransitSchedule();
		this.vehId2mode = new HashMap<Id, String>();
		this.mode2CountsVolume = new HashMap<String, Counts>();
		this.mode2CountsCapacity = new HashMap<String, Counts>();
		this.mode2CountsCapacity_m = new HashMap<String, Counts>();
	}
	
	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		//handle only pt-Vehicles!
		if(this.vehId2mode.containsKey(event.getVehicleId())){
			//create the counts if none exist
			Count countVolume = this.mode2CountsVolume.get(vehId2mode.get(event.getVehicleId())).
					createCount(event.getLinkId(), event.getLinkId().toString());
			Count countCapacity = this.mode2CountsCapacity.get(vehId2mode.get(event.getVehicleId())).
					createCount(event.getLinkId(), event.getLinkId().toString());
			Count countCapacity_m = this.mode2CountsCapacity.get(vehId2mode.get(event.getVehicleId())).
					createCount(event.getLinkId(), event.getLinkId().toString());
			if(countVolume == null){
				//get existing counts
				countVolume = this.mode2CountsVolume.get(vehId2mode.get(event.getVehicleId())).getCount(event.getLinkId());
				countCapacity = this.mode2CountsCapacity.get(vehId2mode.get(event.getVehicleId())).getCount(event.getLinkId());
				countCapacity_m = this.mode2CountsCapacity_m.get(vehId2mode.get(event.getVehicleId())).getCount(event.getLinkId());
			}else{
				//we always want to start with hour zero
				countVolume.createVolume(0, 0.);
				countCapacity.createVolume(0, 0.);
				countCapacity_m.createVolume(0, 0.);
			}
			this.increaseVolume(countVolume, event.getTime());
			double vehCapacity = calcCapacity(event);
			double vehCapacity_m = vehCapacity * scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			this.increaseCapacity(countCapacity, event.getTime(), vehCapacity);
			this.increaseCapacity_m(countCapacity, event.getTime(), vehCapacity_m);
		}
	}
	


	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(!this.sched.getTransitLines().containsKey(event.getTransitLineId())) return;
		TransitLine line = this.sched.getTransitLines().get(event.getTransitLineId());
		if(line == null )log.debug(event.getTransitLineId());
		TransitRoute route = line.getRoutes().get(event.getTransitRouteId());
		if(route == null) {
			log.debug("route " + event.getTransitRouteId() + " is null on TransitLine " + event.getTransitLineId()); 
			return;
		}
		String mode = route.getTransportMode();
		this.vehId2mode.put(event.getVehicleId(), mode);
		if(!this.mode2CountsVolume.containsKey(mode)){
			this.mode2CountsVolume.put(mode, new Counts());
		}		
		if(!this.mode2CountsCapacity.containsKey(mode)){
			this.mode2CountsCapacity.put(mode, new Counts());
		}
		if(!this.mode2CountsCapacity_m.containsKey(mode)){
			this.mode2CountsCapacity_m.put(mode, new Counts());
		}
	}
	
	private void increaseVolume(Count count, Double time){
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
	
	private void increaseCapacity(Count count, double time, double capIncrease) {
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
		v.setValue(v.getValue() + capIncrease);
	}
	
	private void increaseCapacity_m(Count count, double time, double capIncrease) {
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
		v.setValue(v.getValue() + capIncrease);
	}
	
	public int getMaxTimeSlice() {
		return this.maxSlice;
	}
	
	private double calcCapacity(LinkEnterEvent event) {
		int vehSeats = scen.getVehicles().getVehicles().get(event.getVehicleId()).getType().getCapacity().getSeats();
		int vehStand = scen.getVehicles().getVehicles().get(event.getVehicleId()).getType().getCapacity().getStandingRoom();
		int vehCap = vehSeats + vehStand;
		return vehCap;
	}
	
	
}
