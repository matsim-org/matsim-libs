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

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * A writer that writes carriers and their plans in a xml-file.
 *
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeWriter implements MatsimWriter {

	private final MatsimVehicleWriter delegate ;

	public CarrierVehicleTypeWriter( CarrierVehicleTypes types ) {
		// note: for reading, we do the automatic version handling.  for writing, we just always write the newest version; the older writer handlers are
		// left around if someone insists on writing the old version.  Since the carrier vehicle type format is just a subset of the vehicle definitions,
		// we can just use the normal vehicle writer.  kai, sep'19

		Vehicles vehicles = VehicleUtils.createVehiclesContainer() ;
		for( Map.Entry<Id<VehicleType>, VehicleType> entry : types.getVehicleTypes().entrySet() ){
			vehicles.addVehicleType( entry.getValue() );
		}
		delegate = new MatsimVehicleWriter( vehicles ) ;
	}

	@Override public void write( String filename ){
		delegate.writeFile( filename );
	}

}
