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

import org.matsim.api.core.v01.Coord;

import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;


/**
 * @author droeder
 *
 */
public abstract class AbstractMovingObject implements MovingObject{
	
	private Coord currentPosition;
	private double vX;
	private double vY;

	public AbstractMovingObject(Coord position, double vX, double vY){
		this.currentPosition = position;
		this.vX = vX;
		this.vY = vY;
	}

	public double getVx(){
		return this.vX;
	}
	
	public double getVy(){
		return this.vY;
	}
	
	public Coord getCurrentPosition(){
		return this.currentPosition;
	}
	
	protected void setCurrentPosition(Coord c){
		this.currentPosition = c;
	}
	
	protected void setVx(double vX){
		this.vX = vX;
	}
	
	protected void setVy(double vY){
		this.vY = vY;
	}
	
	public abstract boolean processTimeStep(double stepSize, VelocityObstacle obstacle);
	
}
