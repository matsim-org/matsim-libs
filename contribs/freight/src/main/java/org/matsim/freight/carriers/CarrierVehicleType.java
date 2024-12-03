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

package org.matsim.freight.carriers;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * The carrier vehicle type.
 * <p>
 * I decided to put vehicle cost information into the type (which is indeed not a physical attribute of the type). Thus, physical and
 * non-physical attributes are used. This is likely to be changed in the future.
 *
 * @author sschroeder
 *
 */
public class CarrierVehicleType {
	// this is now really only a name space for the builder method.  There are two options where this could go:
	// (1) into CarriersUtils; then it could keep its freight specific syntax
	// (2) into VehicleUtils; then it would need to lose its freight specific syntax
	// However, note that when moving it, the class here still needs to be available since otherwise a lot of outside code will break.
	// kai, sep'19

	private CarrierVehicleType(){} // do not instantiate

	/**
	 * A builder building the type.
	 *
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		final VehicleType delegate ;

		/**
		 * Returns a new instance of builder initialized with the typeId.
		 * <p>
		 * The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 *
		 * @param typeId the type id
		 * @return a type builder
		 */
		public static Builder newInstance(Id<VehicleType> typeId){
			return new Builder(typeId);
		}

		private Builder(Id<VehicleType> typeId){
			this.delegate = VehicleUtils.getFactory().createVehicleType( typeId ) ;
		}

		/**
		 * Sets fixed costs of vehicle.
		 *
		 * <p>By default, it is 0.
		 * @param fix 	fixed costs
		 * @return 		this builder
		 */
		public Builder setFixCost(double fix){
			this.delegate.getCostInformation().setFixedCost( fix ) ;
			return this;
		}

		/**
		 * Sets costs per distance-unit.
		 *
		 * <p>By default, it is 1.
		 *
		 * @param perDistanceUnit 	costs per distance-unit
		 * @return 					this builder
		 */
		public Builder setCostPerDistanceUnit(double perDistanceUnit){
			this.delegate.getCostInformation().setCostsPerMeter( perDistanceUnit ) ;
			return this;
		}

		/**
		 * Sets costs per time-unit.
		 *
		 * <p>By default, it is 0.
		 *
		 * @param perTimeUnit 	costs per time-unit
		 * @return 				this builder
		 */
		public Builder setCostPerTimeUnit(double perTimeUnit){
			this.delegate.getCostInformation().setCostsPerSecond( perTimeUnit ) ;
			return this;
		}

		/**
		 * Sets description.
		 *
		 * @param description 	the description
		 * @return 				this builder
		 */
		public Builder setDescription(String description){
			this.delegate.setDescription( description ) ;
			return this;
		}

		/**
		 * Sets the capacity of vehicle-type.
		 *
		 * <p>By default, the capacity is 0.
		 *
		 * @param capacity 		the capacity of the vehicle-type
		 * @return 				this builder
		 */
		public Builder setCapacity(int capacity){
			this.delegate.getCapacity().setOther( capacity );
			return this;
		}

		/**
		 * Builds the type.
		 *
		 * @return {@link CarrierVehicleType}
		 */
		public VehicleType build(){
			return delegate ;
		}

		public Builder setMaxVelocity(double veloInMeterPerSeconds) {
			this.delegate.setMaximumVelocity( veloInMeterPerSeconds ) ;
			return this;
		}
	}


}
