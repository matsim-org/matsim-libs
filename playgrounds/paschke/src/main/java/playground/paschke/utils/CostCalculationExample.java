package playground.paschke.utils;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;

public class CostCalculationExample implements CostCalculation {
	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double scaleTOMatchCar = 1.0;

	private final static double start = 3600.0 * 14.0;
	private final static double end = 3600.0 * 18.0;

	@Override
	public double getCost(RentalInfo rentalInfo) {
		double startTime = rentalInfo.getStartTime();
		double rentalTIme = rentalInfo.getEndTime() - startTime;
		double inVehicleTime = rentalInfo.getInVehicleTime();

		double reduction = 1.0;
		if (startTime < end && startTime >= start)
			reduction = 0.5;
		
		return reduction * CostCalculationExample.scaleTOMatchCar * 
				(inVehicleTime /60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
	}
}
