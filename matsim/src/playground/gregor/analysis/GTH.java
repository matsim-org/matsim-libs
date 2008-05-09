/* *********************************************************************** *
 * project: org.matsim.*
 * GTH.java
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

package playground.gregor.analysis;

import org.opengis.spatialschema.geometry.geometry.Circle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class GTH {
	
	private GeometryFactory geofac;
	
	public GTH(GeometryFactory geofac) {
		this.geofac = geofac;
	}
	
	
	public Polygon getCircle(Coordinate center, double radius) {
		
		
		return null;
	}
	
	
	public Polygon getSquare(Coordinate center, double length) {
		
		Coordinate [] edges = new Coordinate [5];
		edges[0] = new Coordinate(center.x-length/2,center.y-length/2); 
		edges[1] = new Coordinate(center.x+length/2,center.y-length/2);
		edges[2] = new Coordinate(center.x+length/2,center.y+length/2);
		edges[3] = new Coordinate(center.x-length/2,center.y+length/2);
		edges[4] = new Coordinate(center.x-length/2,center.y-length/2);
		
		LinearRing lr = this.geofac.createLinearRing(edges);
		return this.geofac.createPolygon(lr,null);
	}
	

}
