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
import org.matsim.core.utils.geometry.CoordUtils;

import playground.droeder.Vector2D;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

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
		Vector2D bestNewSpeed = this.goal.subtract(super.getCurrentPosition()).getUnitVector().addFactor(this.maxSpeed);
		
		//TODO reduce speed if agent is next to its goal
		if(covers(obstacle.getGeometry(), super.getCurrentPosition().add(bestNewSpeed))){
			int numOfArcs = 10;
			int speedRate = 5;
			double angle = 2* Math.PI /numOfArcs;
			Vector2D newLocation, newSpeed;
			double dist = Double.POSITIVE_INFINITY;
			double temp;
			bestNewSpeed = new Vector2D(0, 0);
			//TODO very inefficient, need a better algorithm to exteriorPoints
			for(int i = 0 ; i < numOfArcs; i++){
				for(int j = 1; j < speedRate + 1; j++){
					newSpeed = new Vector2D(maxSpeed * j / speedRate * Math.cos(angle * i), maxSpeed * j / speedRate * Math.sin(angle * i));
					newLocation = super.getCurrentPosition().add(newSpeed);
					if(!covers(obstacle.getGeometry(), newLocation)){
						temp  = CoordUtils.calcDistance(newLocation.getCoord(), this.goal.getCoord());
						if(temp < dist){
							dist = temp;
							bestNewSpeed = newSpeed;
						}
					}
				}
			}
			
			this.setSpeed(bestNewSpeed);
		}else{
			this.setSpeed(bestNewSpeed);
		}
	}
	
	private boolean covers(Geometry g, Vector2D v){
		for(int i = 0; i < g.getNumGeometries(); i++){
			if(g.getGeometryN(i).covers(
					new GeometryFactory().createPoint(new Coordinate(v.getX(), v.getY())))){
				return true;
			}
		}
		return false;
	}

	/**
	 * @param stepSize
	 * @return boolean false if agent arrived at goal
	 */
	private void calcNewPosition(double stepSize) {
		if((this.goal.subtract(super.getCurrentPosition()).absolut()) < 0.01){
			//do nothing
			this.arrived = true;
		}else{
			Vector2D r0 = new Vector2D(this.getCurrentPosition().getX(), this.getCurrentPosition().getY());
			Vector2D newPosition = new Vector2D(stepSize, super.getSpeed());
			newPosition = newPosition.add(r0);
			super.setNewPosition(newPosition);
		}
	}
	
	public boolean arrived(){
		return this.arrived;
	}
}
