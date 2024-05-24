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
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;

/**
 * @author mrieser / senozon
 */
public class PolygonFeatureFactory {

	private final Map<String, Class<?>> attributes;
	private final SimpleFeatureType featureType;
	private final GeometryFactory fac = new GeometryFactory();
	private final SimpleFeatureBuilder builder;
	
	private PolygonFeatureFactory(Map<String, Class<?>> attributes, SimpleFeatureType featureType) {
		this.attributes = attributes;
		this.featureType = featureType;
		this.builder = new SimpleFeatureBuilder(this.featureType);
	}
	
	public SimpleFeatureType getFeatureType() {
		return featureType;
	}

	public SimpleFeature createPolygon(final Coordinate[] coordinates) {
		return this.createPolygon(coordinates, Collections.<String, Object>emptyMap(), null);
	}

	public SimpleFeature createPolygon(final Coord[] coordinates) {
		Coordinate[] coords = new Coordinate[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			coords[i] = new Coordinate(coordinates[i].getX(), coordinates[i].getY());
		}
		return this.createPolygon(coords, Collections.<String, Object>emptyMap(), null);
	}

	public SimpleFeature createPolygon(final Coordinate[] coordinates, final Map<String, Object> attributeValues, final String id) {
		LinearRing shell;
		if (coordinates[0] == coordinates[coordinates.length - 1]) {
			shell = this.fac.createLinearRing(coordinates);
		} else {
			Coordinate[] coordinates2 = new Coordinate[coordinates.length + 1];
			System.arraycopy(coordinates, 0, coordinates2, 0, coordinates.length);
			coordinates2[coordinates.length] = coordinates[0];
			shell = this.fac.createLinearRing(coordinates2);
		}
		Polygon p = this.fac.createPolygon(shell, null);
		MultiPolygon mp = this.fac.createMultiPolygon(new Polygon[] {p});
		return this.createPolygon(mp, attributeValues, id);
	}
	
	public SimpleFeature createPolygon(final Polygon polygon, final Map<String, Object> attributeValues, final String id) {
		MultiPolygon mp = this.fac.createMultiPolygon(new Polygon[] {polygon});
		return this.createPolygon(mp, attributeValues, id);
	}

	public SimpleFeature createPolygon(final MultiPolygon polygon, final Map<String, Object> attributeValues, final String id) {
		this.builder.add(polygon);
		for (String name : this.attributes.keySet()) {
			Object value = attributeValues.get(name);
			this.builder.add(value);
		}
		
		return this.builder.buildFeature(id);
	}

	public SimpleFeature createPolygon(final Coordinate[] coordinates, final Object[] attributeValues, final String id) {
		LinearRing shell;
		if (coordinates[0] == coordinates[coordinates.length - 1]) {
			shell = this.fac.createLinearRing(coordinates);
		} else {
			Coordinate[] coordinates2 = new Coordinate[coordinates.length + 1];
			System.arraycopy(coordinates, 0, coordinates2, 0, coordinates.length);
			coordinates2[coordinates.length] = coordinates[0];
			shell = this.fac.createLinearRing(coordinates2);
		}
		Polygon p = this.fac.createPolygon(shell, null);
		MultiPolygon mp = this.fac.createMultiPolygon(new Polygon[] {p});
		return this.createPolygon(mp, attributeValues, id);
	}
	
	public SimpleFeature createPolygon(final Polygon polygon, final Object[] attributeValues, final String id) {
		MultiPolygon mp = this.fac.createMultiPolygon(new Polygon[] {polygon});
		return this.createPolygon(mp, attributeValues, id);
	}
	
	public SimpleFeature createPolygon(final MultiPolygon polygon, Object[] attributeValues, final String id) {
		this.builder.add(polygon);
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

		public PolygonFeatureFactory create() {
			SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.setName(this.name);
			b.setCRS(this.crs);
			b.add("the_geom", MultiPolygon.class);
			for (Map.Entry<String, Class<?>> attr : this.attributes.entrySet()) {
				b.add(attr.getKey(), attr.getValue());
			}
			
			SimpleFeatureType featureType = b.buildFeatureType();
			return new PolygonFeatureFactory(this.attributes, featureType);
		}
	}

}
