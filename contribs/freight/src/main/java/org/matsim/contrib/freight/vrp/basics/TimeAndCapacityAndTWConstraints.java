package org.matsim.contrib.freight.vrp.basics;

import org.apache.log4j.Logger;

public class TimeAndCapacityAndTWConstraints implements Constraints{

	private static Logger logger = Logger.getLogger(TimeAndCapacityAndTWConstraints.class);
	
	private double allowedTimeOnTheRoad;
	
	public TimeAndCapacityAndTWConstraints(double allowedTimeOnTheRoad) {
		super();
		this.allowedTimeOnTheRoad = allowedTimeOnTheRoad;
	}

	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		int currentLoad = 0;
		int maxCap = vehicle.getCapacity();
		if(tour.costs.time > allowedTimeOnTheRoad){
			logger.debug("time-conflict (allowedTimeOnTheRoad=" + allowedTimeOnTheRoad + ";actualTimeOnTheRoad=" + tour.costs.time + " on tour " + tour);
			return false;
		}
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct instanceof JobActivity){
				currentLoad += ((JobActivity) tourAct).getCapacityDemand();
			}
			if(currentLoad > vehicle.getCapacity()){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentLoad=" + currentLoad + " on tour " + tour);
				return false;
			}
			if(tourAct.getLatestArrTime() < tourAct.getEarliestArrTime()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
		}
		return true;
	}
	
}
