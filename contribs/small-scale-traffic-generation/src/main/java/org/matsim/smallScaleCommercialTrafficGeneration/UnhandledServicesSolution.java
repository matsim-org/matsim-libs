package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carrier;

import java.util.List;

/**
 * When generating service-durations for {@link Carrier}s it may happen service durations of their plans
 * are too long to be fully handled. This implementation solves this problem.
 */
public interface UnhandledServicesSolution {

	/**
	 *
	 * Checks and recalculates plans of carriers, which did not serve all services.
	 * This step may take a few minutes.
	 * @param scenario Scenario to handle the carriers for.
	 * @param nonCompleteSolvedCarriers List of carriers, that are not solved.
	 */
	void tryToSolveAllCarriersCompletely(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers);

	/**
	 * Change the service duration for a given carrier, because the service could not be handled in the last solution.
	 *
	 * @param carrier                                     The carrier for which we generate the serviceTime
	 * @param carrierAttributes                           attributes of the carrier to generate the service time for.
	 * @param key                                         key for the service duration
	 * @param additionalTravelBufferPerIterationInMinutes additional buffer for the travel time
	 * @return new service duration
	 */
	int changeServiceTimePerStop(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes, GenerateSmallScaleCommercialTrafficDemand.ServiceDurationPerCategoryKey key, int additionalTravelBufferPerIterationInMinutes);
}
