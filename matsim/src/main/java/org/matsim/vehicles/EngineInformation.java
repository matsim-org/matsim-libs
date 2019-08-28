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
public class EngineInformation implements Attributable {

	private FuelType fuelType;
	private double gasConsumption;
	private Attributes attributes = new Attributes() ;

	/**
	 *
	 * @deprecated litersPerMeter were not longer set here.
	 * Please use EngineInformationImpl(FuelType fueltype) instead if you want to set the FuelType only (recommended)
	 * Please use VehicleUtils.setEngineInformation(...) instead if you also want to set FuelType and FuelConsumption (no longer recommended,
	 * because we want to have the FuelConsumption coming from the emissions contrib). This FuelConsumption here is only a very rough estimation
	 * without traffic condition.
	 */
	@Deprecated
	public EngineInformation( FuelType fueltype, double literPerMeter ) {
		this.setFuelType(fueltype);
		this.setFuelConsumption(literPerMeter);
	}

	public EngineInformation( FuelType fueltype ) {
		this.setFuelType(fueltype);
		this.setFuelConsumption(Double.NaN);
	}

    public EngineInformation() {
    }

	public FuelType getFuelType() {
		return this.fuelType;
	}

	public double getFuelConsumption() {
		return this.gasConsumption;
	}

	public void setFuelType(FuelType fueltype) {
		this.fuelType = fueltype;
	}

	public void setFuelConsumption(double literPerMeter) {
		this.gasConsumption = literPerMeter ;
	}

	public Attributes getAttributes(){
		return attributes ;
	}

	public static enum FuelType {diesel, gasoline, electricity, biodiesel}
}
