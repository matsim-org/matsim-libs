/* *********************************************************************** *
 * project: org.matsim.*
 * VehilceReaderV1
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.population;

import java.util.Map;

import org.matsim.basic.v01.BasicVehicleReaderV1;
import org.matsim.basic.v01.BasicVehicleType;
import org.matsim.interfaces.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class VehicleReaderV1 extends BasicVehicleReaderV1 {

	/**
	 * @param vehicleTypes
	 * @param vehicles
	 */
	public VehicleReaderV1(Map<String, BasicVehicleType> vehicleTypes,
			Map<Id, Vehicle> vehicles) {
		super(new VehicleBuilderImpl(vehicleTypes, vehicles));
	}

}
