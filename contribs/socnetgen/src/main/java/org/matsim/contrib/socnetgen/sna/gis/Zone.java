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
package org.matsim.contrib.socnetgen.sna.gis;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * Representation of a spatial zone. This class is just a wrapper for feature of
 * type "Zone" providing some convenience methods.
 * 
 * @author illenberger
 * 
 */
public class Zone<T> {
	
//	public static final String FEATURE_TYPE_NAME = "Zone";
//	
//	public static final String GEOMETRY_ATTR_NAME = "geometry";
//	
//	public static final String INHABITANT_ATTR_NAME = "inhabitant";
//
//	private final Feature feature;

	private final Geometry geometry;
	
	private final PreparedGeometry preGeometry;
	
	private T attribute;
	
	/**
	 * Creates a new zone wrapping <tt>feature</tt>.
	 * 
	 * @param feature a feature of type {@link Zone#FEATURE_TYPE_NAME}
	 */
	public Zone(Geometry geometry) {
		this.geometry = geometry;
		this.preGeometry = PreparedGeometryFactory.prepare(geometry);
	}
	
//	/**
//	 * Returns the wrapped feature.
//	 * 
//	 * @return the wrapped feature.
//	 */
//	public Feature getFeature() {
//		return feature;
//	}
	
	/**
	 * Returns the default geometry of the wrapped feature.
	 * 
	 * @return the default geometry of the wrapped feature.
	 */
	public Geometry getGeometry() {
//		return feature.getDefaultGeometry();
		return geometry;
	}

	public PreparedGeometry getPreparedGeometry() {
		return preGeometry;
	}
	
	public T getAttribute() {
		return attribute;
	}
	
	public void setAttribute(T attribute) {
		this.attribute = attribute;
	}
	
//	/**
//	 * Returns the number of inhabitants in this zone. The number of inhabitants
//	 * is stored as the feature attribute named
//	 * {@link Zone#INHABITANT_ATTR_NAME}. If the wrapped feature does not
//	 * contain a such named attribute the corresponding exception will be
//	 * thrown.
//	 * 
//	 * @return the number of inhabitants in this zone.
//	 */
//	public int getInhabitants() {
//		return (Integer)feature.getAttribute(INHABITANT_ATTR_NAME); 
//	}
	
//	/**
//	 * Returns the population density in inhabitants per km^2.
//	 * 
//	 * @return the population density in inhabitants per km^2.
//	 */
//	public double getPopulationDensity() {
//		return getInhabitants()/getGeometry().getArea() * 1000 * 1000;
//	}
//	/**
//	 * Returns the ID of the wrapped feature.
//	 * 
//	 * @return the ID of the wrapped feature.
//	 */
//	public String getId() {
//		return feature.getID();
//	}
}
