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


/**
 * @author dgrether
 */
public interface VehicleCapacity {
	
	public Integer getSeats();
	
	public Integer getStandingRoom();
	
	public FreightCapacity getFreightCapacity();
	
	public void setSeats(Integer seats);
	
	public void setStandingRoom(Integer standingRoom);
	
	public void setFreightCapacity(FreightCapacity freightCap);
}
