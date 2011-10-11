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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.droeder.Vector2D;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;

/**
 * @author droeder
 *
 */
public class MovingAgentImpl extends AbstractMovingObject {
	
	private Coord goal;
	private double maxSpeed;
	private boolean arrived = false;

	/**
	 * @param position 
	 * @param goal
	 * @param vX
	 * @param vY
	 * @param maxSpeed
	 * @param maxAcc
	 * @param maxDec 
	 * 
	 */
	public MovingAgentImpl(Coord position, Coord goal, double vX, double vY, double maxSpeed) {
		super(position, vX, vY);
		this.goal = goal;
		this.maxSpeed = maxSpeed;
	}

	/* (non-Javadoc)
	 * @see playground.droeder.realTimeNavigation.movingObjects.AbstractMovingObject#processTimeStep(double, playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle)
	 */
	@Override
	public boolean processTimeStep(double stepSize, VelocityObstacle obstacle) {
		if(this.arrived){
			return false;
		}else{
			this.calcNewSpeed(obstacle);
			this.calcNewPosition(stepSize);
			return true;
		}
	}

	/**
	 * @param obstacle
	 */
	private void calcNewSpeed(VelocityObstacle obstacle) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param stepSize
	 * @return boolean false if agent arrived at goal
	 */
	private void calcNewPosition(double stepSize) {
		if(CoordUtils.calcDistance(this.getCurrentPosition(), this.goal) < 0.01){
			this.arrived = true;
		}else{
			Vector2D r0 = new Vector2D(this.getCurrentPosition().getX(), this.getCurrentPosition().getY());
			Vector2D v0 = new Vector2D(this.getVx(), this.getVy());
			if(v0.absolut() > this.maxSpeed){
				v0 = new Vector2D(maxSpeed, v0.addFactor(1/v0.absolut()));
			}
			Vector2D newPosition = new Vector2D(stepSize, v0);
			newPosition = newPosition.add(r0);
			this.setCurrentPosition(new CoordImpl(newPosition.getX(), newPosition.getY()));
		}
	}
}
