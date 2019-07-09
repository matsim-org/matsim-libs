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

/**
 * @author dgrether
 */
public interface VehicleCapacity extends Attributable {

	double UNDEFINED_VOLUME =  Double.POSITIVE_INFINITY ;
	double UNDEFINED_WEIGHT = Double.POSITIVE_INFINITY ;
	double UNDEFINED_UNITS = Integer.MAX_VALUE ;
	
	public Integer getSeats();
	
	public Integer getStandingRoom();

	/**
	 * @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public FreightCapacity getFreightCapacity();
	
	public void setSeats(Integer seats);
	
	public void setStandingRoom(Integer standingRoom);
	
	/**
	* @deprecated FreightCapacity functionality is now part of VehicleCapacity, kmt Jul19
	 * */
	@Deprecated
	public void setFreightCapacity(FreightCapacity freightCapacity);

	public double getVolumeInCubicMeters();

	public void setVolumeInCubicMeters(double volumeInCubicMeters);

	public double getWeightInTons();

	public void setWeightInTons(double weightInTons);

}
