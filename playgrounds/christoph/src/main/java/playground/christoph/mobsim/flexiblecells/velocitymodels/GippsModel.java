/* *********************************************************************** *
 * project: org.matsim.*
 * GippsModel.java
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

public class GippsModel implements VelocityModel {

	/**
	 * Parameter set for car-following model described by Gipps (1981).
	 */
	
	public static double maxAcceleration = 2.0;	// a
	public static double maxDeceleration = -3.0;	// b
//	public static double vehicleCellSize = 8.0;	// vehicle + space afterwards
	public static double responseTime = 1.0;	// T [s]
	public static double minDesiredSpacing = 8.0; // S [m]
	
	private FlexibleCellLink nextLink = null;
	
	public void setNextLink(FlexibleCellLink nextLink) {
		this.nextLink = nextLink;
	}
	
	@Override
	public double calcSpeed(VehicleCell vehicleCell, double maxDesiredSpeed) {
		
		final double accelerating = this.accelerating(vehicleCell, maxDesiredSpeed);
		final double breaking = this.breaking(vehicleCell); 
		
		if (accelerating == Double.NaN || breaking == Double.NaN) {
			System.out.println("NaN!!");
		}
		
		return Math.min(accelerating, breaking);
	}
	
	private double accelerating(VehicleCell vehicleCell, double maxDesiredSpeed) {
		
		final double speed = vehicleCell.getSpeed();
		final double freeSpeed = maxDesiredSpeed;
		final double speedFactor = speed / freeSpeed; 
		return speed + 2.5 * maxAcceleration * responseTime * (1 - speedFactor)*Math.sqrt(0.025 + speedFactor); 
	}
	
	private double breaking(VehicleCell vehicleCell) {
		
		double distanceDifference;
		double nextVehicleSpeed;
		
		SpaceCell spaceCell = vehicleCell.getNextCell();
		if (spaceCell == null) {
			distanceDifference = 0.0;
			nextVehicleSpeed = 0.0;
		} else {
			VehicleCell nextVehicleCell = spaceCell.getNextCell();			
			if (nextVehicleCell != null) {
				distanceDifference = nextVehicleCell.getHeadPosition() - vehicleCell.getHeadPosition() - minDesiredSpacing;
				nextVehicleSpeed = nextVehicleCell.getSpeed();
			} else {
				// no more vehicles in front on this link - try checking the next link
				if (nextLink != null) {
					nextVehicleCell = nextLink.getFirstVehicle();
					if (nextVehicleCell != null) {
						// calc distance of next vehicle on next link
						double nextLinkDistance = nextVehicleCell.getHeadPosition() - minDesiredSpacing;
						double thisLinkDistance = spaceCell.getHeadPosition() - vehicleCell.getHeadPosition();
						distanceDifference = nextLinkDistance + thisLinkDistance;
						nextVehicleSpeed = nextVehicleCell.getSpeed();
					} else {
						distanceDifference = nextLink.getLength() + (spaceCell.getHeadPosition() - vehicleCell.getHeadPosition());
						nextVehicleSpeed = 0.0;
					}
				}
				// else: stop at the intersection
				else {
					distanceDifference = spaceCell.getHeadPosition() - vehicleCell.getHeadPosition();
					nextVehicleSpeed = 0.0;					
				}
			}
		}	
		
		final double factor = maxDeceleration * responseTime;
		final double root = factor * factor - maxDeceleration * (2 * distanceDifference - vehicleCell.getSpeed() * responseTime - nextVehicleSpeed * nextVehicleSpeed / maxDeceleration);
		
		return factor + Math.sqrt(root);
	}
}
