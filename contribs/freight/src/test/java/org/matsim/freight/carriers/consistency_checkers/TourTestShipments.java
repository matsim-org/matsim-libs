package org.matsim.freight.carriers.consistency_checkers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.CarrierConsistencyCheckers;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
 *
 * @author antonstock
 * doc: this method will check if all given jobs, here shipments, are scheduled properly, i.e. all jobs occur exactly once.
 * IF all jobs are scheduled once, this method will return enum ALL_JOBS_IN_TOURS
 * IF at least one job is missing, this method will return enum NOT_ALL_JOBS_IN_TOURS
 * IF at least one job is scheduled twice, it will return enum JOBS_SCHEDULED_MULTIPLE_TIMES
 * IF at least one job is missing AND at least one job is scheduled twice, it will return enum JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED
 *
 */

public class TourTestShipments {
	private static final Logger log = LogManager.getLogger(TourTestShipments.class);
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * This test is supposed to return ALL_JOBS_IN_TOURS.
	 */
	@Test
	void testTour_shipment_passes() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		CarrierConsistencyCheckers.allJobsInTourCheckResult testResult = CarrierConsistencyCheckers.allJobsInTours(carriers);
		//Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.ALL_JOBS_IN_TOURS, testResult, "At least one job is not scheduled correctly!");

		switch (testResult) {
			case ALL_JOBS_IN_TOURS:
				Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.ALL_JOBS_IN_TOURS, testResult);
				break;
			case JOBS_SCHEDULED_MULTIPLE_TIMES:
				Assertions.fail("At least one job is scheduled multiple times");
				break;
			case NOT_ALL_JOBS_IN_TOURS:
				Assertions.fail("At least one job is not scheduled.");
					break;
			case JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED:
				Assertions.fail("At least one job is missing and at least one job is scheduled more than once.");
					break;
			case JOBS_IN_TOUR_BUT_NOT_LISTED:
				Assertions.fail("At least one job is scheduled in a tour but not listed as a job.");
				break;
			default:
				Assertions.fail("Unexpected test result: " + testResult);
		}
	}

	/**
	 * This test is supposed to return JOBS_SCHEDULED_MULTIPLE_TIMES, because job "parcel_2" is scheduled twice.
	 */
	@Test
	void testTour_shipment_failes_1() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_1.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		CarrierConsistencyCheckers.allJobsInTourCheckResult testResult = CarrierConsistencyCheckers.allJobsInTours(carriers);
		//Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.ALL_JOBS_IN_TOURS, testResult, "At least one job is not scheduled correctly!");

		switch (testResult) {
			case ALL_JOBS_IN_TOURS:
				Assertions.fail("All jobs are scheduled once.");
				break;
			case JOBS_SCHEDULED_MULTIPLE_TIMES:
				Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.JOBS_SCHEDULED_MULTIPLE_TIMES, testResult);
				break;
			case NOT_ALL_JOBS_IN_TOURS:
				Assertions.fail("At least one job is not scheduled.");
				break;
			case JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED:
				Assertions.fail("At least one job is missing and at least one job is scheduled more than once.");
				break;
			case JOBS_IN_TOUR_BUT_NOT_LISTED:
				Assertions.fail("At least one job is scheduled in a tour but not listed as a job.");
				break;
			default:
				Assertions.fail("Unexpected test result: " + testResult);
		}

	}

	/**
	 * This test is supposed to return NOT_ALL_JOBS_IN_TOURS, because both pickup and delivery of "parcel_5" are not scheduled.
	 * NOTE: If pickup of a shipment is missing but the delivery is part of a tour, CarrierPlanXmlReader will throw a IllegalStateExeption. These scenarios are not part of this test.
	 */
	@Test
	void testTour_shipment_failes_2() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_2.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		CarrierConsistencyCheckers.allJobsInTourCheckResult testResult = CarrierConsistencyCheckers.allJobsInTours(carriers);
		//Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.ALL_JOBS_IN_TOURS, testResult, "At least one job is not scheduled correctly!");

		switch (testResult) {
			case ALL_JOBS_IN_TOURS:
				Assertions.fail("All jobs are scheduled once.");
				break;
			case JOBS_SCHEDULED_MULTIPLE_TIMES:
				Assertions.fail("At least one job is scheduled multiple times");
				break;
			case NOT_ALL_JOBS_IN_TOURS:
				Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.NOT_ALL_JOBS_IN_TOURS, testResult);
				break;
			case JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED:
				Assertions.fail("At least one job is missing and at least one job is scheduled more than once.");
				break;
			case JOBS_IN_TOUR_BUT_NOT_LISTED:
				Assertions.fail("At least one job is scheduled in a tour but not listed as a job.");
				break;
			default:
				Assertions.fail("Unexpected test result: " + testResult);
		}

	}

	/**
	 * This test is supposed to return JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED, because both pickup and delivery of "parcel_5" are not scheduled and pickup of "parcel_1" is scheduled twice.
	 * NOTE: If the delivery of a shipment is scheduled n times but its pickup n-1 times, CarrierPlanXmlReader will throw a IllegalStateExeption. These scenarios are not part of this test.
	 */
	@Test
	void testTour_shipment_failes_3() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_3.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		CarrierConsistencyCheckers.allJobsInTourCheckResult testResult = CarrierConsistencyCheckers.allJobsInTours(carriers);
		//Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.ALL_JOBS_IN_TOURS, testResult, "At least one job is not scheduled correctly!");

		switch (testResult) {
			case ALL_JOBS_IN_TOURS:
				Assertions.fail("All jobs are scheduled once.");
				break;
			case JOBS_SCHEDULED_MULTIPLE_TIMES:
				Assertions.fail("At least one job is scheduled multiple times");
				break;
			case NOT_ALL_JOBS_IN_TOURS:
				Assertions.fail("At least one job is not scheduled.");
				break;
			case JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED:
				Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED, testResult);
				break;
			case JOBS_IN_TOUR_BUT_NOT_LISTED:
				Assertions.fail("At least one job is scheduled in a tour but not listed as a job.");
				break;
			default:
				Assertions.fail("Unexpected test result: " + testResult);
		}
	}

	/**
	 * This test is supposed to catch a NullPointerException, because the tour of carrier1 has "parcel_ab"'s pickup and delivery scheduled, but not listed as a shipment.
	 */
	@Test
	void testTour_shipment_failes_4() {
		//@KMT: Diese Lösung ist nicht besonders elegant, gibt es da vielleicht eine bessere Option?

		String pathToInput = utils.getPackageInputDirectory();
		boolean exceptionCatched = false;
		CarrierConsistencyCheckers.allJobsInTourCheckResult testResult = null;
		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_4.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		try {
			CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		} catch (NullPointerException e) {
			testResult = CarrierConsistencyCheckers.allJobsInTourCheckResult.JOBS_IN_TOUR_BUT_NOT_LISTED;
			exceptionCatched = true;
			log.warn("Please check carrier input file - there might be a job scheduled but not listed!");
		}

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		if (!exceptionCatched) {
			testResult = CarrierConsistencyCheckers.allJobsInTours(carriers);
		}
		switch (testResult) {
			case ALL_JOBS_IN_TOURS:
				Assertions.fail("All jobs are scheduled once.");
				break;
			case JOBS_SCHEDULED_MULTIPLE_TIMES:
				Assertions.fail("At least one job is scheduled multiple times");
				break;
			case NOT_ALL_JOBS_IN_TOURS:
				Assertions.fail("At least one job is not scheduled.");
				break;
			case JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED:
				Assertions.fail("At least one job is missing and at least one job is scheduled more than once.");
				break;
			case JOBS_IN_TOUR_BUT_NOT_LISTED:
				Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.JOBS_IN_TOUR_BUT_NOT_LISTED, testResult);
				break;
			default:
				Assertions.fail("Unexpected test result: " + testResult);
		}
	}
}
