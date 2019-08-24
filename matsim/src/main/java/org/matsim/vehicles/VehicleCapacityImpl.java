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


import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author dgrether
 */
public class VehicleCapacityImpl implements VehicleCapacity {

	private Integer seats = 1; // one seat for the driver
	private Integer standingRoom = 0 ;
	private Double volumeInCubicMeters = Double.POSITIVE_INFINITY ; // not an active constraint
	private Double weightInTons = Double.POSITIVE_INFINITY ; // not an active constraint
	private FreightCapacity freightCap = null;
	private Attributes attributes = new Attributes() ;

	@Deprecated // I am rather unsure if it makes sense to both have an interface, and to instantiate the implementation directly.  kai, aug'19
	public VehicleCapacityImpl(){

	}
	
	@Override
	public FreightCapacity getFreightCapacity() {
		return freightCap;
	}

	@Override
	public Integer getSeats() {
		return seats;
	}

	@Override
	public Integer getStandingRoom() {
		return standingRoom;
	}


	@Override
	public void setFreightCapacity(FreightCapacity freightCapacity) {
		this.freightCap = freightCapacity;
	}

	@Override
	public void setSeats(Integer seats) {
		this.seats = seats;
	}

	@Override
	public void setStandingRoom(Integer standingRoom) {
		this.standingRoom = standingRoom;
	}

	@Override
	public Double getVolumeInCubicMeters() {
		return volumeInCubicMeters;
	}

	@Override
	public void setVolumeInCubicMeters(double volumeInCubicMeters) {
		this.volumeInCubicMeters = volumeInCubicMeters;
	}

	@Override
	public Double getWeightInTons() {
		return weightInTons;
	}

	@Override
	public void setWeightInTons(double weightInTons) {
		this.weightInTons = weightInTons;
	}

	@Override public Attributes getAttributes(){
		return this.attributes ;
	}
}
