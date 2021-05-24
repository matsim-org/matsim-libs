/* *********************************************************************** *
 * project: org.matsim.*
 * Zone.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility.gis;

import org.locationtech.jts.geom.Geometry;

/**
 * Representation of a spatial zone. This class is just a wrapper for feature of
 * type "Zone" providing some convenience methods.
 * 
 * @author illenberger
 * 
 */
public final class Zone<T> {

	private final Geometry geometry;
	
	private T attribute;
	
	/**
	 * Creates a new zone wrapping <tt>feature</tt>.
	 * 
	 * @param feature a feature of type {@link Zone#FEATURE_TYPE_NAME}
	 */
	public Zone(Geometry geometry) {
		this.geometry = geometry;
	}
	
	/**
	 * Returns the default geometry of the wrapped feature.
	 * 
	 * @return the default geometry of the wrapped feature.
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	public T getAttribute() {
		return attribute;
	}
	
	public void setAttribute(T attribute) {
		this.attribute = attribute;
	}
}
