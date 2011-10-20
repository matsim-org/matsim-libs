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

import java.util.Collection;

import playground.droeder.Vector2D;
import playground.droeder.realTimeNavigation.movingObjects.MovingObject;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class ReciprocalVelocityObstacleImpl extends AbstractVelocityObstacle {
	private Geometry geometry;
	private GeometryFactory fac = new GeometryFactory();

	/**
	 * @param theOne
	 * @param opponents
	 */
	public ReciprocalVelocityObstacleImpl(MovingObject theOne, Collection<MovingObject> opponents) {
		super(theOne, opponents);
	}

	/* (non-Javadoc)
	 * @see playground.droeder.realTimeNavigation.velocityObstacles.AbstractVelocityObstacle#calcGeometry()
	 */
	@Override
	protected Geometry calcGeometry() {
		if(super.opponents.size() <=1) return null;
		Geometry[] objects = new Geometry[super.opponents.size() - 1];

		Coordinate[] coords;

		int i = 0, ii;
		Vector2D translation;
		for(MovingObject o: this.opponents){
			if(o.equals(super.theOne)) continue;
			coords = new Coordinate[(o.getGeometry().getCoordinates().length * super.theOne.getGeometry().getCoordinates().length) + 1];
			// find the apex of the cone 
			coords[0] = vector2Coordinate(super.theOne.getCurrentPosition().add(o.getSpeed()));
			translation = o.getCurrentPosition().add(o.getSpeed());
			translation = translation.add(o.getSpeed().subtract(super.theOne.getSpeed()).addFactor(0.5));
			
			ii = 1;
			//addition of the coordinates of both agentGeometries -> MinkowskiSum
			for(Coordinate c : o.getGeometry().getCoordinates()){
				//TODO reflection of the origin Geometry in its referencePoint -> not implemented yet, because its not necessary for a circle...
				//TODO build the geometry as triangle, based on the speed of originObject...
				for(Coordinate cc : super.theOne.getGeometry().getCoordinates()){
					coords[ii] = new Coordinate(c.x + cc.x + translation.getX(), c.y + cc.y + translation.getY());
					ii++;
				}
			}
			objects[i] = fac.createMultiPoint(coords).convexHull();
			i++;
		}
		
		// we want only one, or at least the minimum number of, geometries
		this.geometry = fac.createGeometryCollection(objects).buffer(0);
		return this.geometry;
	}
	
	/**
	 * just for intern use
	 * @param v
	 * @return
	 */
	private Coordinate vector2Coordinate(Vector2D v){
		return new Coordinate(v.getX(), v.getY());
	}

}
