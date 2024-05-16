/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonFeatureGenerator.java
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

package org.matsim.utils.gis.matsim2esri.network;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

public class PolygonFeatureGenerator implements FeatureGenerator{

	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;
	private SimpleFeatureBuilder builder;


	public PolygonFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.geofac = new GeometryFactory();
		initFeatureType();
	}

	private void initFeatureType() {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("link");
		typeBuilder.setCRS(this.crs);
		typeBuilder.add("the_geom", Polygon.class);
		typeBuilder.add("ID", String.class);
		typeBuilder.add("fromID", String.class);
		typeBuilder.add("toID", String.class);
		typeBuilder.add("length", Double.class);
		typeBuilder.add("freespeed", Double.class);
		typeBuilder.add("capacity", Double.class);
		typeBuilder.add("lanes", Double.class);
		typeBuilder.add("visWidth", Double.class);

		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
	}

	@Override
	public SimpleFeature getFeature(final Link link) {
		double width = this.widthCalculator.getWidth(link);

		Coordinate[] coords = createPolygonCoordsForLink(link, width);
		Polygon p =  this.geofac.createPolygon(this.geofac.createLinearRing(coords), null);
		Object [] attribs = new Object[9];
		attribs[0] = p;
		attribs[1] = link.getId().toString();
		attribs[2] = link.getFromNode().getId().toString();
		attribs[3] = link.getToNode().getId().toString();
		attribs[4] = link.getLength();
		attribs[5] = link.getFreespeed();
		attribs[6] = link.getCapacity();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = width;

		try {
			return this.builder.buildFeature(null, attribs);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	public static Coordinate[] createPolygonCoordsForLink(final Link link, double width) {
		Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
		double length = from.distance(to);

		final double dx = -from.x   + to.x;
		final double dy = -from.y   + to.y;

		double theta = 0.0;
		if (dx > 0) {
			theta = Math.atan(dy/dx);
		} else if (dx < 0) {
			theta = Math.PI + Math.atan(dy/dx);
		} else { // i.e. DX==0
			if (dy > 0) {
				theta = PI_HALF;
			} else {
				theta = -PI_HALF;
			}
		}
		if (theta < 0.0) theta += TWO_PI;
		double sinTheta = Math.sin(theta);
		double cosTheta = Math.cos(theta);
		double xfrom2 = from.x + sinTheta * width;
		double yfrom2 = from.y - cosTheta * width;
		double xto2 = from.x + cosTheta * length + sinTheta * width;
		double yto2 = from.y + sinTheta * length - cosTheta * width;
		Coordinate from2 = new Coordinate(xfrom2,yfrom2);
		Coordinate to2 = new Coordinate(xto2,yto2);

		return new Coordinate[] {from, to, to2, from2, from}; 
	}

}
