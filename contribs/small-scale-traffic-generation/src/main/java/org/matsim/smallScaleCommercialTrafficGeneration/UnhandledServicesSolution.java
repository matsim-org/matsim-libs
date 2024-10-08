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
	 * @param scenario Scenario to search for carriers with unhandled jobs
	 * @return List with the found carriers
	 */
	List<Carrier> createListOfCarrierWithUnhandledJobs(Scenario scenario);

	/**
	 * Give a service duration based on the purpose and the trafficType under a given probability
	 *
	 * @param carrier                                     The carrier for which we generate the serviceTime
	 * @param carrierAttributes                           attributes of the carrier to generate the service time for.
	 *                                                    selectedStartCategory: the category of the employee
	 * @param additionalTravelBufferPerIterationInMinutes additional buffer for the travel time
	 * @return the service duration
	 */
	int getServiceTimePerStop(Carrier carrier, GenerateSmallScaleCommercialTrafficDemand.CarrierAttributes carrierAttributes, int additionalTravelBufferPerIterationInMinutes);

	/**
	 *
	 * Checks and recalculates plans of carriers, which did not serve all services.
	 * This step may take a few minutes.
	 * @param scenario Scenario to handle the carriers for. Needed to execute {@link org.matsim.freight.carriers.CarriersUtils#runJsprit(Scenario)} and {@link UnhandledServicesSolution#createListOfCarrierWithUnhandledJobs(Scenario)}
	 * @param nonCompleteSolvedCarriers List of carriers, that are not solved. Can be obtained by {@link UnhandledServicesSolution#createListOfCarrierWithUnhandledJobs(Scenario)}
	 */
	void tryToSolveAllCarriersCompletely(Scenario scenario, List<Carrier> nonCompleteSolvedCarriers);
}
