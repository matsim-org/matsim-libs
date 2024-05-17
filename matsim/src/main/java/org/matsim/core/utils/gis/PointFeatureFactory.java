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

package org.matsim.core.utils.gis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;

/**
 * @author mrieser / senozon
 */
public class PointFeatureFactory {

	private final Map<String, Class<?>> attributes;
	private final SimpleFeatureType featureType;
	private final GeometryFactory fac = new GeometryFactory();
	private final SimpleFeatureBuilder builder;

	private PointFeatureFactory(Map<String, Class<?>> attributes, SimpleFeatureType featureType) {
		this.attributes = attributes;
		this.featureType = featureType;
		this.builder = new SimpleFeatureBuilder(this.featureType);
	}

	public SimpleFeatureType getFeatureType() {
		return featureType;
	}
	
	public SimpleFeature createPoint(final Coordinate coordinate) {
		return this.createPoint(coordinate, Collections.<String, Object> emptyMap(), null);
	}

	public SimpleFeature createPoint(final Coordinate coordinate, final Map<String, Object> attributeValues, final String id) {
		Point p = this.fac.createPoint(coordinate);
		
		this.builder.add(p);
		for (String name : this.attributes.keySet()) {
			Object value = attributeValues.get(name);
			this.builder.add(value);
		}
		
		return this.builder.buildFeature(id);
	}
	
	public SimpleFeature createPoint(final Coordinate coordinate, final Object[] attributeValues, final String id) {
		Point p = this.fac.createPoint(coordinate);
		return this.createPoint(p, attributeValues, id);
	}

	public SimpleFeature createPoint(final Point point, final Object[] attributeValues, final String id) {
		this.builder.add(point);
		for (int i = 0; i < attributeValues.length; i++) {
			Object value = attributeValues[i];
			this.builder.add(value);
		}
		return this.builder.buildFeature(id);
	}
	
	public SimpleFeature createPoint(final Coord coordinate, final Object[] attributeValues, final String id) {
		Point p = this.fac.createPoint(new Coordinate(coordinate.getX(), coordinate.getY()));
		
		this.builder.add(p);
		for (int i = 0; i < attributeValues.length; i++) {
			Object value = attributeValues[i];
			this.builder.add(value);
		}
		return this.builder.buildFeature(id);
	}

	public static class Builder {
		private CoordinateReferenceSystem crs = null;
		private String name = "";
		private Map<String, Class<?>> attributes = new LinkedHashMap<String, Class<?>>();

		public Builder() {
		}

		public Builder setCrs(CoordinateReferenceSystem crs) {
			this.crs = crs;
			return this;
		}

		public Builder addAttribute(final String name, final Class<?> type) {
			this.attributes.put(name, type);
			return this;
		}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public PointFeatureFactory create() {
			SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.setName(this.name);
			b.setCRS(this.crs);
			b.add("the_geom", Point.class);
			for (Map.Entry<String, Class<?>> attr : this.attributes.entrySet()) {
				b.add(attr.getKey(), attr.getValue());
			}
			SimpleFeatureType featureType = b.buildFeatureType();
			return new PointFeatureFactory(this.attributes, featureType);
		}
	}

}
