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

import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.carriers.consistency_checkers.CarrierConsistencyCheckers.CheckResult.CHECK_FAILED;
import static org.matsim.freight.carriers.consistency_checkers.CarrierConsistencyCheckers.CheckResult.CHECK_SUCCESSFUL;

/**
 *
 *  @author antonstock
 *	This class checks, if all shipments can be transported -> vehicle has to be large enough and in operation during pickup/delivery times.
 *
 */
public class VehicleScheduleTest {
	//Please specify wanted logger level here:
	//Level.WARN -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	private final Level lvl = Level.ERROR;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 */
	@Test
	void testVehicleScheduleShipment_passes() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersShipmentsPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, CarrierConsistencyCheckers.vehicleScheduleCheck(carriers,lvl), "At least one job can not be handled.");
	}

	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 * - carrier "ccCarrierWithShipments1":
	 * 		no vehicle is in operation when shipment "small_shipment_1" needs to be delivered
	 * 		shipment "large_shipment_1" is too big for all vehicles
	 * - carrier "ccCarrierWithShipment2":
	 * 		no vehicle is not in operation when shipment "small_shipment_2" needs to be picked up
	 * 		shipment "large_shipment_2" is too big for all vehicles
	 */
	@Test
	void testVehicleScheduleShipment_fails() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersShipmentsFAIL.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_FAILED, CarrierConsistencyCheckers.vehicleScheduleCheck(carriers,lvl), "All jobs can be handled.");
	}


	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 */
	@Test
	void testVehicleScheduleService_passes() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersServicesPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, CarrierConsistencyCheckers.vehicleScheduleCheck(carriers,lvl), "At least one job can not be handled.");
	}

	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 * - carrier "ccCarrierWithShipments1":
	 * 		small_service_1: no vehicle in operation
	 * 		medium_service_1: vehicle in operation is too small
	 *		extra_large_service_1: vehicle in operation is too small
	 * - carrier "ccCarrierWithShipment2":
	 *		extra_large_service_2: vehicles are too small
	 */
	@Test
	void testVehicleScheduleService_fails() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersServicesFAIL.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_FAILED, CarrierConsistencyCheckers.vehicleScheduleCheck(carriers,lvl), "All jobs can be handled.");
	}

}
