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

import com.vividsolutions.jts.geom.Geometry;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class OSMObject {
	
	public static final String AREA = "area";
	
	public static final String BUILDING = "building";
	
	public static final String POI = "poi";
	
	private String id;

	private String type;
	
	private Geometry geometry;
	
	private Set<String> activityOptions = new HashSet<String>();

	public OSMObject(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the geometry
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * @param geometry the geometry to set
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	/**
	 * @return the activityOptions
	 */
	public Set<String> getActivityOptions() {
		return activityOptions;
	}

	public void addActivityOption(String option) {
		activityOptions.add(option);
	}
}
