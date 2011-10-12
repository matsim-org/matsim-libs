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
public abstract class AbstractMovingObject implements MovingObject{
	
	private Vector2D currentPosition;
	private Vector2D newPosition;
	private Vector2D speed;
	private Id id;
	private Geometry geometry;

	public AbstractMovingObject(Vector2D position, Id id, Geometry g){
		this.id = id;
		this.currentPosition = position;
		this.geometry = g;
	}
	
	public Geometry getGeometry(){
		return this.geometry;
	}
	
	public Id getId(){
		return this.id;
	}

	public Vector2D getSpeed(){
		return this.speed;
	}

	public Vector2D getCurrentPosition(){
		return this.currentPosition;
	}
	
	protected void setCurrentPosition(Vector2D c){
		this.currentPosition = c;
	}
	
	protected void setNewPosition(Vector2D c){
		this.newPosition = c;
	}
	
	public boolean processNextStep(){
		if(currentPosition.equals(newPosition)){
			return false;
		}else{
			currentPosition = newPosition;
			return true;
		}
	}
	
	protected void setSpeed(Vector2D v){
		this.speed = v;
	}

	public abstract void calculateNextStep(double stepSize, VelocityObstacle obstacle);
	
}
