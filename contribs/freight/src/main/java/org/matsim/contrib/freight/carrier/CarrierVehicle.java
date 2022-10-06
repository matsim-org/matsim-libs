/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.carrier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * 
 * 
 * @author sschroeder
 *
 */
public final class CarrierVehicle implements Vehicle {

	private static final Logger log = LogManager.getLogger(CarrierVehicle.class);

	/**
	 * Returns a new instance of carrierVehicle.
	 * 
	 * The default values for other fields (being implicitly set) are [capacity=0][earliestStart=0.0][latestEnd=Integer.MaxValue()].
	 * 
	 * @param vehicleId
	 * @param locationId
	 * @return CarrierVehicle
	 * @see CarrierVehicle
	 */
	public static CarrierVehicle newInstance(Id<Vehicle> vehicleId, Id<Link> locationId, VehicleType carrierVehicleType ){
		return new Builder( vehicleId, locationId, carrierVehicleType ).build();
	}

	/**
	 * Builder to build vehicles.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a builder with vehicleId and locationId.
		 * 
		 * The default values for other fields (being implicitly set) are [capacity=0][earliestStart=0.0][latestEnd=Integer.MaxValue()].
		 * 
		 * @param vehicleId
		 * @param locationId
		 * @param vehicleType
		 * @return a new vehicle builder
		 */
		public static Builder newInstance( Id<Vehicle> vehicleId, Id<Link> locationId, VehicleType vehicleType ){
			return new Builder(vehicleId, locationId, vehicleType );
		}
		
		private final Id<Link> locationId;
		private final Id<Vehicle> vehicleId;
		private final VehicleType type;
//		private Id<org.matsim.vehicles.VehicleType> typeId;
		private double earliestStart = 0.0;
		private double latestEnd = Integer.MAX_VALUE;
		
		
		public Builder( Id<Vehicle> vehicleId, Id<Link> locationId, VehicleType vehicleType ){
			this.locationId = locationId;
			this.vehicleId = vehicleId;
			this.type = vehicleType;
		}

		/**
		 * @param type
		 * @deprecated The vehicleType need now to be set in the constructor kai/kai jan'22
		 */
		@Deprecated
		public Builder setType( VehicleType type ){
			log.warn(".setType has no functionality anymore and is deprecated");
//			this.type=type;
			return this;
		}

		/**
		 * @param typeId
		 * @deprecated The vehicleTypeId is no longer needed and was confusing -> Use getType().getId kai/kai jan'22
		 */
		@Deprecated
		public Builder setTypeId(Id<VehicleType> typeId ){
			log.warn(".setTypeId has no functionality anymore and is deprecated");
//			this.typeId = typeId;
			return this;
		}
		
		
		public Builder setEarliestStart(double earliestStart){
			this.earliestStart=earliestStart;
			return this;
		}
		
		public Builder setLatestEnd(double latestEnd){
			this.latestEnd = latestEnd;
			return this;
		}
		
		public CarrierVehicle build(){
			Gbl.assertNotNull( this.type );
			return new CarrierVehicle(this);
		}
	}
	
	private final Id<Link> locationId;
	private final Id<Vehicle> vehicleId;
	private final VehicleType vehicleType;
	private final Attributes attributes = new Attributes();
	private final double earliestStartTime;
	private final double latestEndTime;

	private CarrierVehicle(Builder builder){
		vehicleId = builder.vehicleId;
		locationId = builder.locationId;
		vehicleType = builder.type;
		earliestStartTime = builder.earliestStart;
		latestEndTime = builder.latestEnd;
	}

	/**
	 * Used to be getLocation.  Can't say if this is meant to contain only the starting position, or if it is meant to be changed over the day.  kai, jul'22
	 */
	public final Id<Link> getLinkId() {
		return locationId;
	}
	/**
	 * @deprecated -- please inline.  kai, jul'22
	 */
	public final Id<Link> getLocation() { return getLinkId(); }
	@Override
	public Id<Vehicle> getId() {
		return vehicleId;
	}
	
	@Override
	public String toString() {
		return vehicleId + " stationed at " + locationId;
	}
	@Override
	public VehicleType getType() {
		return vehicleType;
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}


	/**
	 * Returns the earliest time a vehicle can be deployed (and thus can depart from its origin).
	 * 
	 * The default value is 0.0;
	 * 
	 * @return the earliest start time
	 */
	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	/**
	 * Returns the latest time a vehicle has to be back in the depot (and thus has to arrive at its final destination).
	 * 
	 * The default value is Integer.MaxValue().
	 * 
	 * @return latest arrival time
	 */
	public double getLatestEndTime() {
		return latestEndTime;
	}

	
	public Id<VehicleType> getVehicleTypeId() {
//		return typeId;
		return vehicleType.getId();
	}

}
