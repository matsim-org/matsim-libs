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

package org.matsim.contrib.minibus.stats.abtractPAnalysisModules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;


/**
 * Count the number of offered capacity-meters per ptModes specified.
 * 
 * @author aneumann
 *
 */
final class CountCapacityMeterPerMode extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, LinkEnterEventHandler, Wait2LinkEventHandler, VehicleLeavesTrafficEventHandler{
	
	private final static Logger log = Logger.getLogger(CountCapacityMeterPerMode.class);
	
	private final Network network;
	private HashMap<Id<Vehicle>, Double> vehId2VehicleCapacity = new HashMap<>();
	
	private HashMap<Id<Vehicle>, String> vehId2ptModeMap;
	private HashMap<String, Double> ptMode2CountMap;
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	
	public CountCapacityMeterPerMode(Network network){
		super(CountCapacityMeterPerMode.class.getSimpleName());
		this.network = network;
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + this.ptMode2CountMap.get(ptMode));
		}
		return strB.toString();
	}
	
	@Override
	public void updateVehicles(Vehicles vehicles) {
		this.vehId2VehicleCapacity = new HashMap<>();
		for (Vehicle veh : vehicles.getVehicles().values()) {
			Integer seats = veh.getType().getCapacity().getSeats();
			Integer standing = veh.getType().getCapacity().getStandingRoom();
			// setting these values is not mandatory. Thus, they maybe null \\DR, aug'13
			this.vehId2VehicleCapacity.put(veh.getId(),
                    ((seats == null) ? 0 : seats) +
                            ((standing == null) ? 0 : standing)
                            - 1.0);
		}
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.vehId2ptModeMap = new HashMap<>();
		this.ptMode2CountMap = new HashMap<>();
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		super.handleEvent(event);
		String ptMode = this.lineIds2ptModeMap.get(event.getTransitLineId());
		if (ptMode == null) {
			log.warn("Could not find a valid pt mode for transit line " + event.getTransitLineId());
			ptMode = "no valid pt mode found";
		}
		this.vehId2ptModeMap.put(event.getVehicleId(), ptMode);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
		if (ptMode == null) {
			ptMode = "nonPtMode";
		}
		if (ptMode2CountMap.get(ptMode) == null) {
			ptMode2CountMap.put(ptMode, 0.0);
		}
		
		double capacity;
		if(super.ptDriverIds.contains(delegate.getDriverOfVehicle(event.getVehicleId()))){
			capacity = this.vehId2VehicleCapacity.get(event.getVehicleId());
		}else{
			// it's a car, which will not appear in the vehicles-list, called in updateVehicles \dr
			// TODO [AN] nonPtMode is not fully implemented - check that again
			capacity = 1;
		}
		double capacityMeterForThatLink = capacity * this.network.getLinks().get(event.getLinkId()).getLength();

		ptMode2CountMap.put(ptMode, ptMode2CountMap.get(ptMode) + capacityMeterForThatLink);
	}
	
	public HashMap<String, Double> getResults(){
		return this.ptMode2CountMap;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		delegate.handleEvent(event);
	}
}
