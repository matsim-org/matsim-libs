/* *********************************************************************** *
 * project: org.matsim.*
 * TravelCost.java
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

package org.matsim.contrib.evacuation.control.helper.shapetostreetsnapper;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.Vehicle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class TravelCost implements TravelDisutility {
	
	private final Polygon p;

	private final GeometryFactory geofac = new GeometryFactory();
	
	public TravelCost(Polygon p) {
		this.p = p;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		Coordinate c0 = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate c1 = MGC.coord2Coordinate(link.getToNode().getCoord());
		LineString ls = this.geofac.createLineString(new Coordinate[]{c0,c1});
		if (ls.intersects(this.p) || this.p.covers(ls)) {
			return Double.POSITIVE_INFINITY;
		}
		
		return link.getLength();
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
}