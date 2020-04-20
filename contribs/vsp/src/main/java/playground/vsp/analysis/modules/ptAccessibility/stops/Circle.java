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
package playground.vsp.analysis.modules.ptAccessibility.stops;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;

/**
 * @author droeder
 *
 */
public class Circle {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Circle.class);

	private List<Coordinate> circle;
	
	public Circle(Double r, int quadrantSegments){
		this.circle = new ArrayList<Coordinate>();
		double pointsPerCirlce = quadrantSegments * 4.0;
		double x,y;
		for(int i = 0; i < pointsPerCirlce; i++){
			x = r * Math.cos(2. * Math.PI * i / pointsPerCirlce);
			y = r * Math.sin(2. * Math.PI * i / pointsPerCirlce);
			this.circle.add(new Coordinate(x, y,0.));
		}
	}
	
	public Polygon createPolygon(GeometryFactory f, Coord coord){
		Coordinate[] c = new Coordinate[this.circle.size() + 1];
		for(int i = 0; i < this.circle.size(); i++){
			c[i] = new Coordinate(circle.get(i).x + coord.getX(), circle.get(i).y + coord.getY(), 0.);
		}
		c[this.circle.size()] = c[0];
		
		Polygon p = f.createPolygon(f.createLinearRing(c), null);
		return p;
	}
	
}

