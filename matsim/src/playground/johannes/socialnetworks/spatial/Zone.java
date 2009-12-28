/* *********************************************************************** *
 * project: org.matsim.*
 * Zone.java
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
package playground.johannes.socialnetworks.spatial;

import org.matsim.api.core.v01.Id;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class Zone {

	private Id id;
	
	private Geometry border;
	
	public Zone(Geometry polygon, Id id) {
		this.border = polygon;
		this.id = id;
	}
	
	public Geometry getBorder() {
		return border;
	}
	
	public Id getId() {
		return id;
	}
}
