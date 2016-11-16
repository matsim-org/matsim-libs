package playground.balac.contribs.carsharing.coststructures;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;

public class CostStructure1 implements CostCalculation{

	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double scaleTOMatchCar = 4.0;
	
	private final static double start1 = 3600.0 * 8.0;
	private final static double end1 = 3600.0 * 8.0;
	
	private final static double start2 = 3600.0 * 14.0;
	private final static double end2 = 3600.0 * 14.0;
	@Override
	public double getCost(RentalInfo rentalInfo) {

		double startTime = rentalInfo.getStartTime();
		double rentalTIme = rentalInfo.getEndTime() - startTime;
		double inVehicleTime = rentalInfo.getInVehicleTime();
		
		double reduction = 1.00;
		if ((startTime < end1 && startTime >= start1) )
			reduction = 0.5;
		
		return reduction * CostStructure1.scaleTOMatchCar * 
				(inVehicleTime /60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
	}

}
