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
package playground.vsp.analysis.modules.ptAccessibility.activity;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;

/**
 * @author droeder
 *
 */
public class ActivityLocation {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(ActivityLocation.class);
	private String type;
	private Coordinate coord;

	public ActivityLocation(Coord coord, String type) {
		this.coord = new Coordinate(coord.getX(), coord.getY(), 0.);
		this.type = type;
	}
	
	@Override
	public boolean equals(Object o){
		if(! (o instanceof ActivityLocation)) return false;
		ActivityLocation delegate = (ActivityLocation) o;
		if(delegate.getCoord().equals(this.getCoord()) && delegate.getType().equals(this.getType())) return true;
		return false;
		
	}

	/**
	 * @return
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @return
	 */
	public Coordinate getCoord() {
		return this.coord;
	}
	
	
}

