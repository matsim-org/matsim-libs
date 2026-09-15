/* *********************************************************************** *
 * project: org.matsim.*
 * BasicVehicles
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;


/**
 * Root class of the vehicles container.
 * @author dgrether
 * @author jwjoubert
 */
public interface Vehicles extends MatsimToplevelContainer {

	public Map<Id<VehicleType>, VehicleType> getVehicleTypes();

	public Map<Id<Vehicle>, Vehicle> getVehicles();

	@Override
	public VehiclesFactory getFactory();

	public void addVehicle(final Vehicle v);

	public void removeVehicle(final Id<Vehicle> vehicleId);

	public void addVehicleType(final VehicleType type);

	public void removeVehicleType(final Id<VehicleType> vehicleTypeId);

	/**
	 * convenience method
	 */
	default public VehicleType addModeVehicleType( final String mode ) {
		VehicleType vehicleType = this.getFactory().createVehicleType( Id.createVehicleTypeId( mode ) ).setNetworkMode( mode );
		this.addVehicleType( vehicleType );
		return vehicleType;
	}
}
