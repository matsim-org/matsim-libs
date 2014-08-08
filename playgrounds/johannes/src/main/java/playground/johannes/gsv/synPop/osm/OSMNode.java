/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.osm;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class OSMNode {

	private String id;
	
	private double longitude;
	
	private double latitude;
	
	private Map<String, String> tags;
	
	private boolean nodeOfWay = false; //TODO can a node be associated to multiple ways?
	
	public OSMNode(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void addTag(String key, String value) {
		if(tags == null)
			tags = new HashMap<String, String>();
		
		tags.put(key, value);
	}
	
	public Map<String, String> tags() {
		return tags;
	}
	
	public void setNodeOfWay(boolean flag) {
		nodeOfWay = flag;
	}
	
	public boolean isNodeOfWay() {
		return nodeOfWay;
	}
}
