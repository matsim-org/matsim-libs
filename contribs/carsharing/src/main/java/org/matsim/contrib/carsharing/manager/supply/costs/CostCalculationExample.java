package org.matsim.contrib.carsharing.manager.supply.costs;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;

/**
 * @author balac
 */
public class CostCalculationExample implements CostCalculation {

  private static final double betaTT = 1.0;
  private static final double betaRentalTIme = 1.0;
  private static final double scaleTOMatchCar = 4.0;

  @Override
  public double getCost(RentalInfo rentalInfo) {

    double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
    double inVehicleTime = rentalInfo.getInVehicleTime();

    return CostCalculationExample.scaleTOMatchCar
        * (inVehicleTime / 60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
  }
}
