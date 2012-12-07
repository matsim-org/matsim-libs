/* *********************************************************************** *
 * project: org.matsim.*
 * OSMNode.java
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

package playground.gregor.sim2d_v4.io.osmparser;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class OSMNode {
	
	private final Id id;
	private final double lat;
	private final double lon;
	private final Map<String,String> tags = new HashMap<String,String>();
	
	
	public OSMNode(double lat, double lon, Id id) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
	}
	
	public double getLat() {
		return this.lat;
	}
	
	public double getLon() {
		return this.lon;
	}
	
	public Id getId() {
		return this.id;
	}
	
	public void addTad(String key, String val) {
		this.tags.put(key, val);
	}
	
	public Map<String,String> getTags() {
		return this.tags;
	}

}
