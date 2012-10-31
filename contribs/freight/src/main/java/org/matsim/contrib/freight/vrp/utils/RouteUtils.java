package org.matsim.contrib.freight.vrp.utils;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

public class RouteUtils {

	public static double getTransportCosts(Collection<VehicleRoute> routes) {
		double cost = 0.0;
		for (VehicleRoute r : routes) {
			if(r.getTour().isEmpty()){
				continue;
			}
			cost += r.getTour().tourData.transportCosts;
		}
		return cost;
	}

	public static double getTransportTime(Collection<VehicleRoute> routes) {
		double time = 0.0;
		for (VehicleRoute r : routes) {
			if(r.getTour().isEmpty()){
				continue;
			}
			time += r.getTour().tourData.transportTime;
		}
		return time;
	}
	
	public static double getTotalCost(Collection<VehicleRoute> routes){
		double total = 0.0;
		for (VehicleRoute r : routes) {
			if(r.getTour().isEmpty()){
				continue;
			}
			total += r.getTour().tourData.totalCost;
		}
		return total;
	}
	
	public static int getNuOfActiveRoutes(Collection<VehicleRoute> routes){
		int count = 0;
		for (VehicleRoute r : routes) {
			if(r.getTour().isEmpty()){
				continue;
			}
			count++;
		}
		return count;
	}
}
