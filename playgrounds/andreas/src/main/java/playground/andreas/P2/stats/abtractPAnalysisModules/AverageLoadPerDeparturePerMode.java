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

package playground.andreas.P2.stats.abtractPAnalysisModules;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.andreas.utils.stats.RecursiveStatsContainer;


/**
 * Calculates the average load per departure and ptModes specified.
 * 
 * @author aneumann
 *
 */
public class AverageLoadPerDeparturePerMode extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleDepartsAtFacilityEventHandler{
	
	private final static Logger log = Logger.getLogger(AverageLoadPerDeparturePerMode.class);
	
	private HashMap<Id, Double> vehId2VehicleCapacity = new HashMap<Id, Double>();
	
	private HashMap<Id, String> vehId2ptModeMap;
	private HashMap<Id, Integer> vehId2PaxMap = new HashMap<Id, Integer>();
	private HashMap<String, RecursiveStatsContainer> ptMode2Stats = new HashMap<String, RecursiveStatsContainer>();

	
	public AverageLoadPerDeparturePerMode(){
		super(AverageLoadPerDeparturePerMode.class.getSimpleName());
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + (this.ptMode2Stats.get(ptMode).getMean()));
		}
		return strB.toString();
	}
	
	@Override
	public void updateVehicles(Vehicles vehicles) {
		this.vehId2VehicleCapacity = new HashMap<Id, Double>();
		for (Vehicle veh : vehicles.getVehicles().values()) {
			Integer seats = veh.getType().getCapacity().getSeats();
			Integer standing = veh.getType().getCapacity().getStandingRoom();
			// setting these values is not mandatory. Thus, they maybe null \\DR, aug'13
			this.vehId2VehicleCapacity.put(veh.getId(), 
					new Double(
							((seats == null) ? 0 : seats) + 
							((standing == null) ? 0 : standing)
							- 1.0));
		}
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.vehId2ptModeMap = new HashMap<Id, String>();
		this.vehId2PaxMap = new HashMap<Id, Integer>();
		this.ptMode2Stats = new HashMap<String, RecursiveStatsContainer>();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		super.handleEvent(event);
		String ptMode = this.lineIds2ptModeMap.get(event.getTransitLineId());
		if (ptMode == null) {
			log.warn("Should not happen");
			ptMode = "no valid pt mode found";
		}
		this.vehId2ptModeMap.put(event.getVehicleId(), ptMode);
		
		if (this.vehId2PaxMap.get(event.getVehicleId()) != null) {
			if (this.vehId2PaxMap.get(event.getVehicleId()).intValue() != 0) {
				log.warn(event.getVehicleId() + " has still " + this.vehId2PaxMap.get(event.getVehicleId()) + " passengers onboard. Should be zero by now.");
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(!super.ptDriverIds.contains(event.getPersonId())){
			if (this.vehId2PaxMap.get(event.getVehicleId()) == null) {
				this.vehId2PaxMap.put(event.getVehicleId(), new Integer(0));
			}
			
			this.vehId2PaxMap.put(event.getVehicleId(), new Integer(this.vehId2PaxMap.get(event.getVehicleId()) + 1));
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(!super.ptDriverIds.contains(event.getPersonId())){
			this.vehId2PaxMap.put(event.getVehicleId(), new Integer(this.vehId2PaxMap.get(event.getVehicleId()) - 1));
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
		if (ptMode == null) {
			ptMode = "nonPtMode";
		}
		if (this.ptMode2Stats.get(ptMode) == null) {
			this.ptMode2Stats.put(ptMode, new RecursiveStatsContainer());
		}
		
		double currentLoad = 0.0;
		if (this.vehId2PaxMap.get(event.getVehicleId()) != null) {
			currentLoad = this.vehId2PaxMap.get(event.getVehicleId()).doubleValue();
		}
		double capacity = this.vehId2VehicleCapacity.get(event.getVehicleId()).doubleValue();
		this.ptMode2Stats.get(ptMode).handleNewEntry(currentLoad / capacity);
	}
}
