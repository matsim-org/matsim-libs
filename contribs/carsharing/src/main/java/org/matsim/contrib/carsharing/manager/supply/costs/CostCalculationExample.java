package org.matsim.contrib.carsharing.manager.supply.costs;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;

public class CostCalculationExample implements CostCalculation {

	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		
		
		
		return rentalTIme /3600.0 * 2.0;
	}

}
