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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public abstract class X2GraphImpl implements X2Graph {
	protected Network network;
	protected CoordinateReferenceSystem crs;
	protected GeometryFactory geofac;

	protected Collection<SimpleFeature> features;

	protected List<Map<Id<Link>, ?>> parameters = new ArrayList<>();
	protected List<Tuple<String, Class<?>>> attrTypes = new ArrayList<Tuple<String, Class<?>>>();

	public void addParameter(String paramName, Class<?> clazz, Map<Id<Link>, ?> params) {
		attrTypes.add(new Tuple<String, Class<?>>(paramName, clazz));
		parameters.add(params);
	}

	protected LinearRing getLinearRing(Link link) {
		// //////////////////////////////////////////////////////////////
		double width = getLinkWidth(link);
		// //////////////////////////////////////////////////////////////
		Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());

		Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());

		double xdiff = to.x - from.x;
		double ydiff = to.y - from.y;
		double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
		double xwidth = width * ydiff / denominator;
		double ywidth = -width * xdiff / denominator;

		Coordinate fromB = new Coordinate(from.x + xwidth, from.y + ywidth, 0);
		Coordinate toB = new Coordinate(to.x + xwidth, to.y + ywidth, 0);
		// ////////////////////////////////////////////////////////////////////////
		return new LinearRing(new CoordinateArraySequence(new Coordinate[] {
				from, to, toB, fromB, from }), geofac);
	}

	protected double getLinkWidth(Link link) {
		return 50;
	}
}
