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

package playground.andreas.bvgAna.level2;

import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.andreas.bvgAna.level1.VehId2OccupancyHandler;

/**
 * Calculates the load of a vehicles depending on time and vehicle characteristics given.
 * 
 * @author aneumann
 *
 */
public class VehId2LoadMap implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{

	private final Logger log = Logger.getLogger(VehId2LoadMap.class);
	private final Level logLevel = Level.DEBUG;
	
	private VehId2OccupancyHandler vehId2OccupancyHandler;
	private Map<Id, Vehicle> vehiclesMap;
	
	public VehId2LoadMap(Vehicles vehicles){
		this.log.setLevel(this.logLevel);
		this.vehId2OccupancyHandler = new VehId2OccupancyHandler();
		this.vehiclesMap = vehicles.getVehicles();
	}
	
	/**
	 * @return Returns the load for a given vehicle id and time.
	 */
	public double getVehLoadByTime(Id vehId, double time){
		double occupancy = this.vehId2OccupancyHandler.getVehicleLoad(vehId, time);
		double capacity = this.vehiclesMap.get(vehId).getType().getCapacity().getSeats().intValue()
						+ this.vehiclesMap.get(vehId).getType().getCapacity().getStandingRoom().intValue();
		double load = occupancy / capacity;
		this.log.debug("Occupancy " + occupancy + ", Capacity " + capacity + ", Load " + load);
		
		if(load > 1.0){
			this.log.warn("Load for vehicle " + vehId + " at " + time + " > 1. Better check this");
		}
		return load;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehId2OccupancyHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.vehId2OccupancyHandler.handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
		this.vehId2OccupancyHandler.reset(iteration);		
	}
}
