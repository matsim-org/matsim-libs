/* *********************************************************************** *
 * project: org.matsim.*
 * MyZone.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities.containers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MyZone extends MultiPolygon implements Identifiable<MyZone>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Id<MyZone> id;
	private ObjectAttributes attr;

	public MyZone(Polygon[] polygons, GeometryFactory factory, Id<MyZone> id) {
		super(polygons, factory);
		this.id = id;
		this.attr = new ObjectAttributes();
	}
	
	@Override
	public Id<MyZone> getId() {
		return this.id;
	}
	
	public Point sampleRandomInteriorPoint(){
		Point p = null;
		
		Geometry envelope = this.getEnvelope();
		double c1x = envelope.getCoordinates()[0].x;
		double c2x = envelope.getCoordinates()[2].x;
		double c1y = envelope.getCoordinates()[0].y;
		double c2y = envelope.getCoordinates()[2].y;
		double minX = Math.min(c1x, c2x);
		double maxX = Math.max(c1x, c2x);
		double minY = Math.min(c1y, c2y);
		double maxY = Math.max(c1y, c2y);
		
		while(p == null){
			double sampleX = minX + Math.random()*(maxX-minX);
			double sampleY = minY + Math.random()*(maxY-minY);
			Point pp = this.factory.createPoint(new Coordinate(sampleX, sampleY));
			if(this.covers(pp)){
				p = pp;
			}
		}
		
		return p;
	}
	
	public ObjectAttributes getObjectAttributes(){
		return this.attr;
	}
	
}

