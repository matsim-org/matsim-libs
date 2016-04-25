/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicles.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.southafrica.freight.digicore.containers;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

/**
 * A container to hold multiple {@link DigicoreVehicle}s.
 * 
 * @author jwjoubert
 */
public class DigicoreVehicles {
	final private Logger log = Logger.getLogger(DigicoreVehicles.class);
	final private ObjectAttributes vehicleAttributes = new ObjectAttributes();
	private Map<Id<Vehicle>, DigicoreVehicle> vehicles = new HashMap<>();
	private String descr = null;
	private int counter = 0;
	private int nextMsg = 1;
	private boolean silent = false;
	private String crs = "Atlantis";
	
	public DigicoreVehicles() {
		/* No coordinate reference system set. */
	}
	
	public DigicoreVehicles(String crs){
		this.crs = crs;
	}
	
	public void setCoordinateReferenceSystem(String crs){
		this.crs = crs;
	}
	
	public String getCoordinateReferenceSystem(){
		return this.crs;
	}
	
	public void setDescription(String descr){
		this.descr = descr;
	}
	
	public String getDescription(){
		return this.descr;
	}
	
	public void setSilentLog(boolean silent){
		this.silent = silent;
	}
	
	public boolean isSilent(){
		return this.silent;
	}
	
	/**
	 * Adds the vehicle to the container.
	 * 
	 * @param vehicle
	 * @throws IllegalArgumentException if a vehicle already exists with the same {@link Id}.
	 */
	public void addDigicoreVehicle(DigicoreVehicle vehicle){
		/* Do not allow duplicate vehicle IDs. */
		if(vehicles.containsKey(vehicle.getId())){
			throw new IllegalArgumentException("There is already a vehicle with Id " + vehicle.getId().toString() + " in the population.");
		}
		
		/* Add the vehicle to the container. */
		this.vehicles.put(vehicle.getId(), vehicle);
		
		/* Update the counter. */
		counter++;
		if(this.counter % this.nextMsg == 0){
			nextMsg *= 2;
			printVehicleCount();
		}
	}
	
	/**
	 * Returns the container of {@link DigicoreVehicle}s. 
	 * @return
	 */
	public Map<Id<Vehicle>, DigicoreVehicle> getVehicles(){
		return this.vehicles;
	}
	
	/**
	 * Returns the attributes container of the vehicles.  
	 * @return
	 */
	public ObjectAttributes getVehicleAttributes(){
		return this.vehicleAttributes;
	}
	
	private void printVehicleCount(){
		if(!silent){
			log.info("  vehicles # " + this.counter);
		}
	}
	
	
}
