/* *********************************************************************** *
 * project: org.matsim.*
 * LeutzbachModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.flexiblecells.velocitymodels;

import playground.christoph.mobsim.flexiblecells.FlexibleCellLink;
import playground.christoph.mobsim.flexiblecells.SpaceCell;
import playground.christoph.mobsim.flexiblecells.VehicleCell;

public class LeutzbachModel implements VelocityModel {

	/**
	 * Parameter set for car-following model described by Leutzbach (1986).
	 */
	public static double maxAcceleration = 1.5;	// a [m/s^2]
	public static double maxDeceleration = 3.5;	// b [m/s^2]
	public static double responseTime = 1.0;	// T [s]
	public static double minDesiredSpacing = 20;	// S [m]
	public static double vehicleCellSize = 8.0;	// vehicle + space afterwards
	
	private final double timeStep;
	private FlexibleCellLink nextLink = null;
	
	public LeutzbachModel(double timeStep) {
		this.timeStep = timeStep;
	}
	
	@Override
	public void setNextLink(FlexibleCellLink nextLink) {
		this.nextLink = nextLink;
	}
	
	@Override
	public double calcSpeed(VehicleCell vehicleCell, double maxDesiredSpeed) {
		
		final double maxAcc = this.calcMaxAcceleration(vehicleCell);
		double acc = Math.min(maxAcc, maxAcceleration);
		
		if (acc < 0.0) acc = Math.max(-maxDeceleration, acc);
		
		final double speed = vehicleCell.getSpeed();
		double newSpeed = speed + acc * timeStep;
		
		if (newSpeed <= maxDesiredSpeed) {
			vehicleCell.setAcceleration(acc);
//			vehicleCell.setSpeed(newSpeed);
			return newSpeed;
		} else {
			double deltaSpeed = maxDesiredSpeed - speed;
			acc = deltaSpeed / this.timeStep;
			vehicleCell.setAcceleration(acc);
//			vehicleCell.setSpeed(maxDesiredSpeed);
			return maxDesiredSpeed;
		}		
	}
	
	private double calcMaxAcceleration(VehicleCell vehicleCell) {
		
		boolean stopAtIntersection = this.nextLink == null;
		
		SpaceCell spaceCell = vehicleCell.getNextCell();
		if (spaceCell == null && !stopAtIntersection) return maxAcceleration;
		
		VehicleCell nextVehicleCell = spaceCell.getNextCell();
		if (nextVehicleCell == null) {
			if (!stopAtIntersection) return maxAcceleration;
			else {
				nextVehicleCell = new VehicleCell(null, spaceCell.getHeadPosition() + minDesiredSpacing + vehicleCellSize, 
						vehicleCellSize, 0.0);
				nextVehicleCell.setAcceleration(maxAcceleration);
			}
		}
		
		double speedDifference = nextVehicleCell.getSpeed() - vehicleCell.getSpeed();
		double distanceDifference = nextVehicleCell.getHeadPosition() - vehicleCell.getHeadPosition() - nextVehicleCell.getMinLength();

		// special case for stopped vehicles
		if (vehicleCell.getSpeed() == 0.0) {
			double stopDistance = responseTime * vehicleCell.getSpeed() + vehicleCell.getSpeed() * vehicleCell.getSpeed() / (2 * maxDeceleration);
			stopDistance += this.timeStep * vehicleCell.getSpeed();
			if (stopDistance < distanceDifference) return maxAcceleration;
			
		}
		
		double aNext = nextVehicleCell.getAcceleration();
		double acceleration = (speedDifference * speedDifference) / (2 * (minDesiredSpacing - distanceDifference)) + aNext;
		
		return acceleration;
	}
}
