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

import java.util.ArrayList;


/**
 * A container class to store the basic location elements of a Geographical 
 * Positioning System (GPS) trace. This is one of the possible elements inside
 * an overall activity/trip chain.
 * 
 * @author jwjoubert
 */
public class DigicoreTrace extends ArrayList<DigicorePosition> implements DigicoreChainElement{
	private static final long serialVersionUID = 1L;
	private String crs;
	
	/**
	 * Container class for keeping track of the sequence of positions, i.e. the
	 * GPS trace of a device/person/vehicle.
	 * 
	 * @param crs
	 */
	public DigicoreTrace(String crs) {
		this.crs = crs;
	}
	
	/**
	 * Returns the coordinate reference system (CRS) for this GPS trace.
	 * 
	 * @return
	 */
	public String getCrs(){
		return this.crs;
	}
	
}
