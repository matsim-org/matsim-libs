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

package org.matsim.basic.v01;

/**
 * @author dgrether
 */
public class BasicVehicleCapacityImpl implements BasicVehicleCapacity {

	private int seats;
	private int standingRoom;
	private BasicFreightCapacity freightCap;
	
	public BasicFreightCapacity getFreightCapacity() {
		return freightCap;
	}

	public int getSeats() {
		return seats;
	}

	public int getStandingRoom() {
		return standingRoom;
	}

	public void setFreightCapacity(BasicFreightCapacity freightCap) {
		this.freightCap = freightCap;
	}

	public void setSeats(int seats) {
		this.seats = seats;
	}

	public void setStandingRoom(int standingRoom) {
		this.standingRoom = standingRoom;
	}
	
}
