/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.jsprit;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class FiFoTravelTime implements TravelTime {

	private final TravelTime travelTime;

	private final int binSize;

	public FiFoTravelTime(TravelTime travelTime, int binSize) {
		super();
		this.travelTime = travelTime;
		this.binSize = binSize;
	}

	public double getLinkTravelTime(Link link, double time, Vehicle vehicle) {
		double tt = getTravelTime(link, time, vehicle);
		if(getTimeBin(time) == getTimeBin(time+tt)){
			return tt;
		}
		else{
			double totalTravelTime = 0.0;
			double distanceToTravel = link.getLength();
			double currentTime = time;
			boolean distanceTraveled = false;
			while(!distanceTraveled){
				double nextTimeThreshold = getNextTimeBin(currentTime);
				if(currentTime < nextTimeThreshold){
					double speed = calculateCurrentSpeed(link,time, vehicle);
					double maxReachableDistanceInThisTimeBin = (nextTimeThreshold-currentTime)*speed;
					if(distanceToTravel > maxReachableDistanceInThisTimeBin){
						distanceToTravel = distanceToTravel - maxReachableDistanceInThisTimeBin;
						totalTravelTime += (nextTimeThreshold-currentTime);
						currentTime = nextTimeThreshold;
					}
					else{ //<= maxReachableDistance
						totalTravelTime += distanceToTravel/speed;
						distanceTraveled = true;
					}
				}
			}
			return totalTravelTime;
		}
	}

	private double getTravelTime(Link link, double time, Vehicle vehicle) {
		return travelTime.getLinkTravelTime(link, time, null, vehicle);
	}

	private double calculateCurrentSpeed(Link link, double time, Vehicle vehicle) {
		double speed = link.getLength()/getTravelTime(link, time, vehicle);
		if(speed > vehicle.getType().getMaximumVelocity()){
			speed = vehicle.getType().getMaximumVelocity();
		}
		return speed;
	}

	private int getTimeBin(double currentTime){
		return (int)currentTime/binSize;
	}

	private double getNextTimeBin(double currentTime) {
		double lastThreshold = Math.floor(currentTime/binSize) * binSize;
		return lastThreshold + binSize;
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return getLinkTravelTime(link, time, vehicle);
	}
}
