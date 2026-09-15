/*
 *   *********************************************************************** *
 *   project: org.matsim.*													 *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2025 by the members listed in the COPYING, 		 *
 *                          LICENSE and WARRANTY file.  					 *
 *   email           : info at matsim dot org   							 *
 *                                                                         	 *
 *   *********************************************************************** *
 *                                                                        	 *
 *     This program is free software; you can redistribute it and/or modify  *
 *      it under the terms of the GNU General Public License as published by *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.								     *
 *     See also COPYING, LICENSE and WARRANTY file						 	 *
 *                                                                           *
 *   *********************************************************************** *
 */

package org.matsim.freight.carriers.consistency_checkers;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.carriers.consistency_checkers.CarrierConsistencyCheckers.CheckResult.CHECK_FAILED;
import static org.matsim.freight.carriers.consistency_checkers.CarrierConsistencyCheckers.CheckResult.CHECK_SUCCESSFUL;

/**
 * @author antonstock
 * this method will check if all given jobs, here shipments, are scheduled properly, i.e. all jobs occur exactly once.
 */

public class ShipmentsTourTest {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	private final Level lvl = Level.ERROR;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * This test is supposed to return CHECK_SUCCESSFUL
	 */
	@Test
	void testTour_shipment_passes() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

        Assertions.assertEquals(CHECK_SUCCESSFUL, CarrierConsistencyCheckers.allJobsInToursCheck(carriers,lvl), "There is at least one inconsistency within the selected plan!");
	}

	/**
	 * This test is supposed to return CHECK_FAILED, because job "parcel_2" is scheduled twice.
	 */
	@Test
	void testTour_shipment_fails_1() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_1.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

        Assertions.assertEquals(CHECK_FAILED, CarrierConsistencyCheckers.allJobsInToursCheck(carriers,lvl), "There is no inconsistency within the selected plan!");
	}

	/**
	 * This test is supposed to return CHECK_FAILED, because both pickup and delivery of "parcel_5" are not scheduled.
	 * NOTE: If pickup of a shipment is missing but the delivery is part of a tour, CarrierPlanXmlReader will throw a IllegalStateException. These scenarios are not part of this test.
	 */
	@Test
	void testTour_shipment_fails_2() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_2.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

        Assertions.assertEquals(CHECK_FAILED, CarrierConsistencyCheckers.allJobsInToursCheck(carriers,lvl), "There is no inconsistency within the selected plan!");
	}

	/**
	 * This test is supposed to return CHECK_FAILED, because both pickup and delivery of "parcel_5" are not scheduled and pickup of "parcel_1" is scheduled twice.
	 * NOTE: If the delivery of a shipment is scheduled n times but its pickup n-1 times, CarrierPlanXmlReader will throw a IllegalStateException. These scenarios are not part of this test.
	 */
	@Test
	void testTour_shipment_fails_3() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_3.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

        Assertions.assertEquals(CHECK_FAILED, CarrierConsistencyCheckers.allJobsInToursCheck(carriers,lvl), "There is no inconsistency within the selected plan!");
	}

	/**
	 * This test is supposed to catch a NullPointerException, because the tour of carrier1 has "parcel_ab"'s pickup and delivery scheduled, but not listed as a shipment.
	 */
	@Test
	void testTour_shipment_fails_4() {
		//@KMT: Diese Lösung ist nicht besonders elegant, gibt es da vielleicht eine bessere Option?

		String pathToInput = utils.getPackageInputDirectory();
		boolean exceptionCaught = false;
		CarrierConsistencyCheckers.CheckResult checkResult;

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithShipmentsFAIL_4.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		try {
			CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		} catch (NullPointerException e) {
			exceptionCaught = true;
		}

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		if (!exceptionCaught) {
			checkResult = CarrierConsistencyCheckers.allJobsInToursCheck(carriers,lvl);
		} else {
			checkResult = CHECK_FAILED;
		}
		Assertions.assertEquals(CHECK_FAILED, checkResult, "There is no inconsistency within the selected plan!");
	}
}
