/* *********************************************************************** *
 * project: org.matsim.*
 * Network2GraphImpl.java
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

package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkLayer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public abstract class X2GraphImpl implements X2Graph {
	protected NetworkLayer network;
	protected CoordinateReferenceSystem crs;
	protected GeometryFactory geofac;

	protected Collection<Feature> features;
	protected DefaultFeatureTypeFactory defaultFeatureTypeFactory;

	protected List<Map<Id, ?>> parameters = new ArrayList<Map<Id, ?>>();
	protected List<AttributeType> attrTypes = new ArrayList<AttributeType>();

	public static Coordinate getCoordinate(Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}

	public void addParameter(String paramName, Class<?> clazz, Map<Id, ?> params) {
		attrTypes.add(AttributeTypeFactory.newAttributeType(paramName, clazz));
		this.parameters.add(params);
	}

}
