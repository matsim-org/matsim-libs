/* *********************************************************************** *
 * project: org.matsim.*
 * FeatureGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.referencing;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class FeatureGenerator {
	
	GeometryFactory geofac = new GeometryFactory();
	FeatureType ftPoint;
	private FeatureType ftLineString;
	
	

	
	public FeatureGenerator(final CoordinateReferenceSystem crs, final String [] header) {
		final AttributeType[] attribPoint = new AttributeType[header.length + 1];
		final AttributeType[] attribLineString = new AttributeType[header.length + 1];
		attribPoint[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, crs);
		attribLineString[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, crs);
		for (int i = 0; i < header.length; i++) {
			attribPoint[i+1] = AttributeTypeFactory.newAttributeType(header[i], String.class);
			attribLineString[i+1] = AttributeTypeFactory.newAttributeType(header[i], String.class);
		}
		
		try {
			this.ftPoint = FeatureTypeBuilder.newFeatureType(attribPoint, "pointShape");
			this.ftLineString = FeatureTypeBuilder.newFeatureType(attribLineString, "lineStringShape");
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		
	}
	
	public Feature getFeature(final Coordinate c, final String [] input) {
		final Object [] obj = new Object [input.length+1];
		obj[0] = this.geofac.createPoint(c);
		for (int i = 0; i < input.length; i++) {
			obj[i+1] = input[i];
		}
		try {
			return this.ftPoint.create(obj);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Feature getFeature(final Coordinate [] c, final String [] input) {
		final Object [] obj = new Object [input.length+1];
		obj[0] = this.geofac.createLineString(c);
		for (int i = 0; i < input.length; i++) {
			obj[i+1] = input[i];
		}
		try {
			return this.ftLineString.create(obj);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
	}

}
