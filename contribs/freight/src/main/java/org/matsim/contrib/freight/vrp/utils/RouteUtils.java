package org.matsim.contrib.freight.vrp.utils;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

public class RouteUtils {
	
	public static double getTransportCosts(Collection<VehicleRoute> routes){
		double cost = 0.0;
		for(VehicleRoute r : routes){
			cost += r.getTour().tourData.transportCosts;
		}
		return cost;
	}

	public static double getTransportTime(Collection<VehicleRoute> routes){
		double time = 0.0;
		for(VehicleRoute r : routes){
			time += r.getTour().tourData.transportTime;
		}
		return time;
	}
}
