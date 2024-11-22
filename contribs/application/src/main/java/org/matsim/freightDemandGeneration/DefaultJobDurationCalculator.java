package org.matsim.freightDemandGeneration;

import org.matsim.freight.carriers.Carrier;

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
	public void recalculateServiceDurations(Carrier thisCarrier) {
		// Do nothing
	}

	@Override
	public void recalculateShipmentDurations(Carrier thisCarrier) {
		// Do nothing
	}

	/**
	 * @param serviceTimePerUnit		service time per unit
	 * @param demandForThisService		demand for this service
	 * @return							default calculation
	 */
	private int getDefaultCalculation(int serviceTimePerUnit, int demandForThisService) {
		if (demandForThisService == 0)
			return serviceTimePerUnit;
		else
			return serviceTimePerUnit * demandForThisService;
	}
}
