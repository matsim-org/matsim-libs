/* *********************************************************************** *
 * project: org.matsim.*
 * BasicFreightCapacity
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

/**
 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
 * */
@Deprecated
public interface FreightCapacity extends Attributable {

	/**
	 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public double getVolume();

	/**
	 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public void setVolume(double cubicMeters);

	/**
	 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public double getWeight();

	/**
	 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public void setWeight(double tons);

	/**
	 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public int getUnits();

	/**
	 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public void setUnits(int units);

	double UNDEFINED_VOLUME =  Double.POSITIVE_INFINITY ;
	double UNDEFINED_WEIGHT = Double.POSITIVE_INFINITY ;
	double UNDEFINED_UNITS = Integer.MAX_VALUE ;
	
}
