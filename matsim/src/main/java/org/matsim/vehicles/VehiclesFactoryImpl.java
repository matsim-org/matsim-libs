/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;

/**
 * deliberately non-public since there is an interface.  kai, nov'11
 * 
 * @author dgrether
 */
final class VehiclesFactoryImpl implements VehiclesFactory {
	// The design is roughly as follows:
	// * VehicleType and its sub-types VehicleCapacity and CostInformation are no longer behind interfaces.  They are so "small" that we will assume that
	// we will never optimize them.  Which means that they can also be instantiated directly; the methods here are there for historical reasons and for
	// convenience.
	// * Hierarchical sub-types are gone.  E.g. there is no FreighCapacity within VehicleCapacity any more.
	// * EngineInformation is deprecated and should go away soon.  In practice, the hbefa entries are used, and they are used via Attributable.
	// kai/kai, aug'19


	/**
	 * deliberately non-public since there is a factory.  kai, nov'11
	 */
	VehiclesFactoryImpl() {
	}

	@Override
	public Vehicle createVehicle(Id<Vehicle> id, VehicleType type) {
		return VehicleUtils.createVehicle(id, type );
	}
	
	@Override
	public VehicleType createVehicleType(Id<VehicleType> typeId) {
		return VehicleUtils.createVehicleType(typeId );
	}


}
