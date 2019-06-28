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

	private Integer seats = null;
	private Integer standingRoom = null;
	private FreightCapacity freightCap = null;
	private Attributes attributes = new Attributes() ;

	public VehicleCapacityImpl(){}
	
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

	@Override public Attributes getAttributes(){
		return this.attributes ;
	}
}
