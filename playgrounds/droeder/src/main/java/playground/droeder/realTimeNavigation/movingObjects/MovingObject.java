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
package playground.droeder.realTimeNavigation.movingObjects;

import org.matsim.api.core.v01.Id;

import playground.droeder.Vector2D;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public interface MovingObject {
	
	public Id getId();
	
	public Vector2D getCurrentPosition();
	
	public boolean processNextStep();
	
	public void calculateNextStep(double stepSize, VelocityObstacle obstacle);
	
	public Geometry getGeometry();
	
	public Vector2D getSpeed();
}
