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
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/**
 * @author mrieser / senozon
 */
public class PolylineFeatureFactory {
	
	private final Map<String, Class<?>> attributes;
	private final SimpleFeatureType featureType;
	private final GeometryFactory fac = new GeometryFactory();
	private final SimpleFeatureBuilder builder;

	private PolylineFeatureFactory(Map<String, Class<?>> attributes, SimpleFeatureType featureType) {
		this.attributes = attributes;
		this.featureType = featureType;
		this.builder = new SimpleFeatureBuilder(this.featureType);
	}

	public SimpleFeatureType getFeatureType() {
		return featureType;
	}
	
	public SimpleFeature createPolyline(final Coordinate[] coordinates) {
		return this.createPolyline(coordinates, Collections.<String, Object> emptyMap(), null);
	}

	public SimpleFeature createPolyline(final Coordinate[] coordinates, final Map<String, Object> attributeValues, final String id) {
		LineString ls = this.fac.createLineString(coordinates);
		MultiLineString mls = this.fac.createMultiLineString(new LineString[] {ls});
		
		this.builder.add(mls);
		for (String name : this.attributes.keySet()) {
			Object value = attributeValues.get(name);
			this.builder.add(value);
		}
		
		return this.builder.buildFeature(id);
	}

	public SimpleFeature createPolyline(final Coordinate[] coordinates, final Object[] attributeValues, final String id) {
		LineString ls = this.fac.createLineString(coordinates);
		return this.createPolyline(ls, attributeValues, id);
	}
	
	public SimpleFeature createPolyline(final LineString lineString, final Object[] attributeValues, final String id) {
		MultiLineString mls = this.fac.createMultiLineString(new LineString[] {lineString});
		return this.createPolyline(mls, attributeValues, id);
	}

	public SimpleFeature createPolyline(final MultiLineString multiLineString, final Object[] attributeValues, final String id) {
		this.builder.add(multiLineString);
		for (Object value : attributeValues) {
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
		
		public PolylineFeatureFactory create() {
			SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.setName(this.name);
			b.setCRS(this.crs);
			b.add("the_geom", MultiLineString.class);
			for (Map.Entry<String, Class<?>> attr : this.attributes.entrySet()) {
				b.add(attr.getKey(), attr.getValue());
			}
			SimpleFeatureType featureType = b.buildFeatureType();
			return new PolylineFeatureFactory(this.attributes, featureType);
		}
	}

}
