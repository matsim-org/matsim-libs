/* *********************************************************************** *
 * project: org.matsim.*
 * LineStringBasedFeatureGenerator.java
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
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

public class LineStringBasedFeatureGenerator implements FeatureGenerator{

	private final WidthCalculator widthCalculator;
	private SimpleFeatureBuilder builder;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;


	public LineStringBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.geofac = new GeometryFactory();
		initFeatureType();
	}


	private void initFeatureType() {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("link");
		typeBuilder.setCRS(this.crs);
		typeBuilder.add("the_geom", LineString.class);
		typeBuilder.add("ID", String.class);
		typeBuilder.add("fromID", String.class);
		typeBuilder.add("toID", String.class);
		typeBuilder.add("length", Double.class);
		typeBuilder.add("freespeed", Double.class);
		typeBuilder.add("capacity", Double.class);
		typeBuilder.add("lanes", Double.class);
		typeBuilder.add("visWidth", Double.class);
		typeBuilder.add("type", String.class);

		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
	}


	@Override
	public SimpleFeature getFeature(final Link link) {
		double width = this.widthCalculator.getWidth(link);
		LineString ls = this.geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
				MGC.coord2Coordinate(link.getToNode().getCoord())});

		Object [] attribs = new Object[10];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		attribs[2] = link.getFromNode().getId().toString();
		attribs[3] = link.getToNode().getId().toString();
		attribs[4] = link.getLength();
		attribs[5] = link.getFreespeed();
		attribs[6] = link.getCapacity();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = width;
		attribs[9] = NetworkUtils.getType(link);

		try {
			return this.builder.buildFeature(null, attribs);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}

	}

}
