package org.matsim.contrib.freight.vrp.utils;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

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
			total += r.getTour().getTotalCost();
		}
		return total;
	}
	
	
	public static double getTotalServiceTime(Collection<VehicleRoute> routes){
		double total = 0.0;
		for (VehicleRoute r : routes) {
			if(r.getTour().isEmpty()){
				continue;
			}
			for(org.matsim.contrib.freight.vrp.basics.TourActivity act : r.getTour().getActivities()){
				total += act.getOperationTime();
			}
		}
		return total;
	}
	
	
	public static double getTotalFixCost(Collection<VehicleRoute> routes){
		double total = 0.0;
		for (VehicleRoute r : routes) {
			if(r.getTour().isEmpty()){
				continue;
			}
			total += r.getVehicle().getType().vehicleCostParams.fix;
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
