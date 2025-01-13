package org.matsim.freightDemandGeneration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controller;

import java.util.HashMap;

public class DefaultDemandGenerationSpecification implements DemandGenerationSpecification {
	private static double roundingError;

	@Override
	public int getDemandToDistribute(DemandReaderFromCSV.DemandInformationElement demandInformationElement,
									 HashMap<Id<Person>, Person> possiblePersonsFirstJobElement, HashMap<Id<Person>, Person> possiblePersonsSecondJobElement) {
		roundingError = 0;	//because this happens at the beginning of the demand generation
		return demandInformationElement.getDemandToDistribute();
	}

	@Override
	public double calculateServiceDuration(Integer serviceTimePerUnit, int demandForThisService) {
		return getDefaultCalculationForJobDuration(serviceTimePerUnit, demandForThisService);
	}

	@Override
	public double calculatePickupDuration(Integer pickupDurationPerUnit, int demandForThisShipment) {
		return getDefaultCalculationForJobDuration(pickupDurationPerUnit, demandForThisShipment);
	}

	@Override
	public double calculateDeliveryDuration(Integer deliveryDurationPerUnit, int demandForThisShipment) {
		return getDefaultCalculationForJobDuration(deliveryDurationPerUnit, demandForThisShipment);
	}

	@Override
	public void recalculateJobDurations(Scenario scenario) {
		// do nothing
	}

	@Override
	public int calculateDemandForThisLinkWithFixNumberOfJobs(int demandToDistribute, Integer numberOfJobs, int distributedDemand,
															 DemandReaderFromCSV.LinkPersonPair selectedNewLinkPersonPairForFirstJobElement, DemandReaderFromCSV.LinkPersonPair selectedNewLinkPersonPairForSecondJobElement, int i) {
		int demandForThisLink = (int) Math.ceil((double) demandToDistribute / (double) numberOfJobs);
		if (numberOfJobs == (i + 1)) {
			demandForThisLink = demandToDistribute - distributedDemand;
		} else {
			roundingError = roundingError
				+ ((double) demandForThisLink - ((double) demandToDistribute / (double) numberOfJobs));
			if (roundingError >= 1) {
				demandForThisLink = demandForThisLink - 1;
				roundingError = roundingError - 1;
			}
		}
		return demandForThisLink;
	}

	@Override
	public int calculateDemandBasedOnLinkLength(int countOfLinks, int distributedDemand, Integer demandToDistribute, int maxLinks,
												double sumOfPossibleLinkLength, Link link) {

		int demandForThisLink;
		if (countOfLinks == maxLinks) {
			demandForThisLink = demandToDistribute - distributedDemand;
		} else {
			demandForThisLink = (int) Math
				.ceil(link.getLength() / sumOfPossibleLinkLength * (double) demandToDistribute);
			roundingError = roundingError + ((double) demandForThisLink
				- (link.getLength() / sumOfPossibleLinkLength * (double) demandToDistribute));
			if (roundingError >= 1) {
				demandForThisLink = demandForThisLink - 1;
				roundingError = roundingError - 1;
			}
		}
		return demandForThisLink;
	}

	@Override
	public void writeAdditionalOutputFiles(Controller controller) {
		FreightDemandGenerationUtils.createDemandLocationsFile(controller);
	}

	/**
	 * Calculates the duration of a job based on the time per unit and the demand for this service.
	 *
	 * @param timePerUnit          time per unit
	 * @param demandForThisService demand for this service
	 * @return default calculation
	 */
	private int getDefaultCalculationForJobDuration(int timePerUnit, int demandForThisService) {
		if (demandForThisService == 0)
			return timePerUnit;
		else
			return timePerUnit * demandForThisService;
	}
}
