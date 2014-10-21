package org.matsim.contrib.freight.jsprit;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class FiFoTravelTime implements TravelTime {

	private TravelTime travelTime;
	
	private int binSize;
	
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
		double linkTravelTime = travelTime.getLinkTravelTime(link, time, null, vehicle);
		return linkTravelTime;
	}
		
	private double calculateCurrentSpeed(Link link, double time, Vehicle vehicle) {
		double speed = link.getLength()/getTravelTime(link, time, vehicle);
		if(speed > vehicle.getType().getMaximumVelocity()){
			speed = vehicle.getType().getMaximumVelocity();
		}
		return speed;
	}
	
	private double getTimeBin(double currentTime){
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