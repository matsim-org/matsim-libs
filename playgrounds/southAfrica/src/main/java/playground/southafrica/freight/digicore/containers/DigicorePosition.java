/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreTrace.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.freight.digicore.containers;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.southafrica.freight.digicore.utils.DigicoreUtils;

/**
 * A container class to store the basic element of a Geographical Positioning
 * System (GPS) record: a time stamp and the location.
 * 
 * @author jwjoubert
 */
public class DigicorePosition {
	private GregorianCalendar time;
	private Coord coord;
	
	/**
	 * Use this constructor if the location only has a longitude and latitude, 
	 * and no elevation.
	 * 
	 * @param time
	 * @param lon
	 * @param lat
	 */
	public DigicorePosition(long time, double lon, double lat) {
		this.time = DigicoreUtils.convertTimestampToGregorianCalendar(
				time*1000, TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		this.coord = CoordUtils.createCoord(lon, lat);
	}
	
	/**
	 * Use this constructor when the location has a known elevation. If not,
	 * rather use the alternative {@link #DigicorePosition(long, double, double)}
	 * constructor that only takes longitude and latitude.
	 *  
	 * @param time
	 * @param lon
	 * @param lat
	 * @param ele
	 */
	public DigicorePosition(long time, double lon, double lat, double ele){
		this.time = DigicoreUtils.convertTimestampToGregorianCalendar(
				time*1000, TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		this.coord = CoordUtils.createCoord(lon, lat, ele);
	}
	
	public GregorianCalendar getTimeAsGregorianCalendar(){
		return this.time;
	}
	
	public Coord getCoord(){
		return this.coord;
	}
	
}
