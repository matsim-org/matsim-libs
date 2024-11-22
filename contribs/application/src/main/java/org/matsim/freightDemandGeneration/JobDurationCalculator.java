package org.matsim.freightDemandGeneration;

import org.matsim.api.core.v01.Scenario;

public interface JobDurationCalculator {
	/**
	 * Calculates the duration of a service in seconds.
	 *
	 * @param serviceDurationPerUnit in seconds
	 * @param demandForThisService   amount of demand for this service
	 * @return duration in seconds
	 */
	double calculateServiceDuration(Integer serviceDurationPerUnit, int demandForThisService);

	/**
	 * Calculates the duration of a pickup in seconds.
	 *
	 * @param pickupDurationPerUnit in seconds
	 * @param demandForThisShipment amount of demand for this shipment
	 * @return duration in seconds
	 */
	double calculatePickupDuration(Integer pickupDurationPerUnit, int demandForThisShipment);

	/**
	 * Calculates the duration of a delivery in seconds.
	 *
	 * @param deliveryDurationPerUnit in seconds
	 * @param demandForThisShipment   amount of demand for this shipment
	 * @return duration in seconds
	 */
	double calculateDeliveryDuration(Integer deliveryDurationPerUnit, int demandForThisShipment);

	/**
	 * Recalculates the job durations for all jobs in the scenario. The devault implementation does nothing.
	 *
	 * @param scenario scenario
	 */
	void recalculateJobDurations(Scenario scenario);
}
