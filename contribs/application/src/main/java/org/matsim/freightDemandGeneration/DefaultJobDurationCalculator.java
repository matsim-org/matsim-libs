package org.matsim.freightDemandGeneration;

import org.matsim.api.core.v01.Scenario;

public class DefaultJobDurationCalculator implements JobDurationCalculator {
	@Override
	public double calculateServiceDuration(Integer serviceTimePerUnit, int demandForThisService) {
		return getDefaultCalculation(serviceTimePerUnit, demandForThisService);
	}

	@Override
	public double calculatePickupDuration(Integer pickupDurationPerUnit, int demandForThisShipment) {
		return getDefaultCalculation(pickupDurationPerUnit, demandForThisShipment);
	}

	@Override
	public double calculateDeliveryDuration(Integer deliveryDurationPerUnit, int demandForThisShipment) {
		return getDefaultCalculation(deliveryDurationPerUnit, demandForThisShipment);
	}

	@Override
	public void recalculateJobDurations(Scenario scenario) {
		// do nothing
	}

	/**
	 * @param timePerUnit				time per unit
	 * @param demandForThisService		demand for this service
	 * @return							default calculation
	 */
	private int getDefaultCalculation(int timePerUnit, int demandForThisService) {
		if (demandForThisService == 0)
			return timePerUnit;
		else
			return timePerUnit * demandForThisService;
	}
}
