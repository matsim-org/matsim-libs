package org.matsim.freightDemandGeneration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controller;

import java.util.HashMap;

public interface DemandGenerationSpecification {

	/**
	 * Gets the demand to distribute for this demand information element.
	 *
	 * @param demandInformationElement 	demand information element
	 * @param possiblePersonsFirstJobElement		possible persons for pickup
	 * @param possiblePersonsSecondJobElement	possible persons for delivery
	 * @return demand to distribute
	 */
	int getDemandToDistribute(DemandReaderFromCSV.DemandInformationElement demandInformationElement,
							  HashMap<Id<Person>, Person> possiblePersonsFirstJobElement, HashMap<Id<Person>, Person> possiblePersonsSecondJobElement);
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

	int calculateDemandForThisLinkWithFixNumberOfJobs(int demandToDistribute, Integer numberOfJobs, int distributedDemand,
													  DemandReaderFromCSV.LinkPersonPair selectedNewLinkPersonPairForFirstJobElement, DemandReaderFromCSV.LinkPersonPair selectedNewLinkPersonPairForSecondJobElement, int i);
	/**
	 * @param countOfLinks				counter
	 * @param distributedDemand 		Already distributed demand
	 * @param demandToDistribute 		Demand to distribute
	 * @param maxLinks					Maximum of possible links for demand
	 * @param sumOfPossibleLinkLength	Sum of all lengths of the links
	 * @param link 						this link
	 * @return							Demand for this link
	 */
	int calculateDemandBasedOnLinkLength(int countOfLinks, int distributedDemand, Integer demandToDistribute,
														int maxLinks, double sumOfPossibleLinkLength, Link link);

	void writeAdditionalOutputFiles(Controller controller);
}
