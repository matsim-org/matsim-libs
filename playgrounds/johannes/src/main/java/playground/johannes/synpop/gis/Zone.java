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

package playground.johannes.synpop.gis;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygonal;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class Zone {

	private static final Logger logger = Logger.getLogger(Zone.class);
	
	private final Geometry geometry;

	private Map<String, String> attributes;

	public Zone(Geometry geometry) {
		if(!(geometry instanceof Polygonal))
			logger.warn("Geometry is not instance of Polygonal. This is ok but may have effects on geometric operations.");
		
		this.geometry = geometry;
	}

	public Geometry getGeometry() {
		return geometry;
	}
	
	private void initAttributes() {
		if (attributes == null)
			attributes = new HashMap<String, String>();
	}

	public String getAttribute(String key) {
		if (attributes == null)
			return null;
		else
			return attributes.get(key);
	}

	public Map<String, String> attributes() {
		return Collections.unmodifiableMap(attributes);
	}
	
	public String setAttribute(String key, String value) {
		initAttributes();
		return attributes.put(key, value);
	}

	public String removeAttribute(String key) {
		if(attributes == null) return null;
		else return attributes.remove(key);
	}
}
