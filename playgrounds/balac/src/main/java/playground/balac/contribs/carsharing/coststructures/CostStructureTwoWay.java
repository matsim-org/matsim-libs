package playground.balac.contribs.carsharing.coststructures;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;

public class CostStructureTwoWay implements CostCalculation {

	private final static double scaleTOMatchCar = 1.0;

	@Override
	public double getCost(RentalInfo rentalInfo) {
		
		double startTime = rentalInfo.getStartTime();
		double rentalTime = rentalInfo.getEndTime() - startTime;
		double distance = rentalInfo.getDistance();
		double reduction = 0.75;
		
		return reduction * scaleTOMatchCar * (rentalTime / 3600.0 * 2.8 + distance / 1000.0 * 0.6); 				
			
	}
}
