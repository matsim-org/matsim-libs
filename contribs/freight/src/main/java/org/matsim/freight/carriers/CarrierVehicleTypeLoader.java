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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @deprecated Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle. kai/kai jan'22
 * <p>
 * Loader that loads/assigns vehicleTypes to their vehicles and carriers respectively.
 *
 * @author sschroeder
 *
 */
@Deprecated
public class CarrierVehicleTypeLoader {

	@SuppressWarnings("unused")
	private static final  Logger logger = LogManager.getLogger(CarrierVehicleTypeLoader.class);

	/**
	 * Constructs the loader with the carriers the types should be assigned to.
	 *
	 * @param carriers
	 *
	 *  * @deprecated Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle. kai/kai jan'22
	 */
	@Deprecated
	public CarrierVehicleTypeLoader(Carriers carriers) {
		super();
	}

	/**
	 * Assigns types to carriers and their vehicles.
	 *
	 * @param types
	 *
	 * @deprecated Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle. kai/kai jan'22
	 */
	@Deprecated
	public void loadVehicleTypes(CarrierVehicleTypes types){
		logger.error("Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle.");
	}

}
