package org.matsim.freightDemandGeneration;

import org.matsim.freight.carriers.Carrier;

public interface JobDurationCalculator {
	double calculateServiceDuration(Integer serviceDurationPerUnit, int demandForThisService);

	double calculatePickupDuration(Integer pickupDurationPerUnit, int demandForThisShipment);

	double calculateDeliveryDuration(Integer deliveryDurationPerUnit, int demandForThisShipment);

	void recalculateServiceDurations(Carrier thisCarrier);

	void recalculateShipmentDurations(Carrier thisCarrier);
}
