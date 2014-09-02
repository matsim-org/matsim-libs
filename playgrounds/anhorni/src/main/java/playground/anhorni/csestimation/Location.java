/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.csestimation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Location {
	private Id<Location> id;
	private Coord coord;	 
	private String city;
	
	public Location(Id<Location> id) {
		this.id = id;
	}
	
	public Location(Id<Location> id, Coord coord) {
		this.id = id;
		this.coord = coord;
	}
	
	public Id<Location> getId() {
		return id;
	}
	public Coord getCoord() {
		return coord;
	}
	public void setId(Id<Location> id) {
		this.id = id;
	}
	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
}
