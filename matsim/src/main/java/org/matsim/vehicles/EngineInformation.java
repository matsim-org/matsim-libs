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

import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author dgrether
 */
public final class EngineInformation implements Attributable {
	// yyyy maybe these subtypes should be immutable?

	private Attributes attributes = new Attributes() ;

	/* package-private */ EngineInformation() {
		// yyyy leaving this public for the time being because otherwise the carrier vehicle type reading logic needs to change completely.  kai, sep'19
	}
	@Deprecated
	public FuelType getFuelType() {
		return VehicleUtils.getFuelType( this ) ;
	}
	@Deprecated
	public double getFuelConsumption() {
		return VehicleUtils.getFuelConsumption(this);
	}
	@Deprecated
	public void setFuelType( FuelType fueltype ) {
		VehicleUtils.setFuelType(this, fueltype);
	}
	@Deprecated
	public void setFuelConsumption( double literPerMeter ) {
		VehicleUtils.setFuelConsumption(this, literPerMeter);
	}
	public Attributes getAttributes(){
		return attributes ;
	}
	@Deprecated
	public enum FuelType {diesel, gasoline, electricity, biodiesel}
}
