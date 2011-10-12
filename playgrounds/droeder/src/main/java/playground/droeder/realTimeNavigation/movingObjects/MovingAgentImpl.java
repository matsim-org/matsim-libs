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

import javax.vecmath.Vector2d;

import org.matsim.api.core.v01.Id;

import playground.droeder.Vector2D;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class MovingAgentImpl extends AbstractMovingObject {
	
	private Vector2D goal;
	private double maxSpeed;
	private boolean arrived = false;

	public MovingAgentImpl(Vector2D position, Vector2D goal, double maxSpeed, Id id, Geometry g) {
		super(position, id, g);
		this.goal = goal;
		this.maxSpeed = maxSpeed;
		this.setSpeed(this.goal.subtract(super.getCurrentPosition()).getUnitVector().addFactor(this.maxSpeed));
	}

	/* (non-Javadoc)
	 * @see playground.droeder.realTimeNavigation.movingObjects.AbstractMovingObject#processTimeStep(double, playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle)
	 */
	@Override
	public void calculateNextStep(double stepSize, VelocityObstacle obstacle) {
		//TODO better calc obstacle here
		if(this.arrived){
			return;
		}else{
			this.calcNewSpeed(obstacle);
			this.calcNewPosition(stepSize);
		}
	}

	/**
	 * @param obstacle
	 */
	private void calcNewSpeed(VelocityObstacle obstacle) {
		//TODO calculate the best possible speed to the goal outside of the velocityObstalce
		obstacle.getGeometry().getCoordinates();
		this.setSpeed(this.goal.subtract(super.getCurrentPosition()).getUnitVector().addFactor(this.maxSpeed));
		
	}

	/**
	 * @param stepSize
	 * @return boolean false if agent arrived at goal
	 */
	private void calcNewPosition(double stepSize) {
		if((this.goal.subtract(super.getCurrentPosition()).absolut()) < 0.01){
			this.arrived = true;
		}else{
			Vector2D r0 = new Vector2D(this.getCurrentPosition().getX(), this.getCurrentPosition().getY());
			Vector2D newPosition = new Vector2D(stepSize, super.getSpeed());
			newPosition = newPosition.add(r0);
			super.setNewPosition(newPosition);
		}
	}
}
