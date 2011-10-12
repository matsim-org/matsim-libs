/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.realTimeNavigation.velocityObstacles;

import java.util.List;
import java.util.Set;

import playground.droeder.Vector2D;
import playground.droeder.realTimeNavigation.movingObjects.MovingObject;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author droeder
 *
 */
public class VelocityObstacleImpl extends AbstractVelocityObstacle {
	
	private GeometryCollection geometries;
	private GeometryFactory fac = new GeometryFactory();

	public VelocityObstacleImpl(MovingObject theOne, Set<MovingObject> opponents) {
		super(theOne, opponents);
	}

	@Override
	protected Geometry calcGeometry() {
		Geometry[] objects = new Geometry[super.opponents.size() - 1];

		Coordinate[] coords;

		int i = 0, ii;
		Vector2D translation;
		for(MovingObject o: this.opponents){
			if(o.equals(super.theOne)) continue;
			coords = new Coordinate[(o.getGeometry().getCoordinates().length * super.theOne.getGeometry().getCoordinates().length) + 1];
			coords[0] = vector2Coordinate(super.theOne.getCurrentPosition().add(o.getSpeed()));
			translation = o.getCurrentPosition().add(o.getSpeed());
			
			ii = 1;
			for(Coordinate c : o.getGeometry().getCoordinates()){
				for(Coordinate cc : super.theOne.getGeometry().getCoordinates()){
					coords[ii] = new Coordinate(c.x + cc.x + translation.getX(), c.y + cc.y + translation.getY());
					ii++;
				}
			}
			//TODO throws sometimes emptystack
			objects[i] = fac.createMultiPoint(coords).convexHull();
			i++;
		}
		
		
		this.geometries = fac.createGeometryCollection(objects);
		return this.geometries;
	}
	
	private Coordinate vector2Coordinate(Vector2D v){
		return new Coordinate(v.getX(), v.getY());
	}


}
