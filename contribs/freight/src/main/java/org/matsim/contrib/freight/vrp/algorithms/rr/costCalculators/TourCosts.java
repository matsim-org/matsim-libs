package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class TourCosts {
	
	public static TourCost createTransportCostOnly(){
		return new TourCost(){

			@Override
			public double getTourCost(TourImpl tour, Driver driver,Vehicle vehicle) {
				return tour.tourData.transportCosts;
			}
			
		};
	}
	
	public static TourCost createTotalVariableCosts(){
		return new TourCost(){

			@Override
			public double getTourCost(TourImpl tour, Driver driver,Vehicle vehicle) {
				return tour.tourData.totalCost;
			}
			
		};
	}

	public static TourCost createTotalCosts(){
		return new TourCost(){

			@Override
			public double getTourCost(TourImpl tour, Driver driver,Vehicle vehicle) {
				return vehicle.getType().vehicleCostParams.fix + tour.tourData.totalCost;
			}
			
		};
	}
}
