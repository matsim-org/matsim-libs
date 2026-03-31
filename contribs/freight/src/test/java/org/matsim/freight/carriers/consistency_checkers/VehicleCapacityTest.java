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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;

import org.apache.logging.log4j.Level;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.carriers.consistency_checkers.CarrierConsistencyCheckers.CheckResult.CHECK_FAILED;
import static org.matsim.freight.carriers.consistency_checkers.CarrierConsistencyCheckers.CheckResult.CHECK_SUCCESSFUL;

/**
 *  @author antonstock
 *	VehicleCapacityTest checks, if all carriers have at least one vehicle with sufficient capacity for every job. If the capacity demand of a job
 * 	is higher than the highest vehicle capacity, capacityCheck will return false and a log warning with details about the affected carrier(s) and job(s).
 */

public class VehicleCapacityTest {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	private final Level lvl = Level.ERROR;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This test will check if the vehicles of carriers c1 and c2 have enough capacity to handle the given SHIPMENTS.
	 */
	@Test
	void testVehicleCapacityShipments_passes() {

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCTestCarriersShipmentsPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, CarrierConsistencyCheckers.vehicleCapacityCheck(carriers, lvl), "At least one carrier has no sufficient vehicle!");
	}

	/**
	 * This test will check if the vehicles of carriers c1 and c2 have enough capacity to handle the given jobs.
	 * ccCarrierWithShipments1: shipment "large_shipment" capacity demand of 33 is too high
	 * ccCarrierWithShipments2: shipment "large_shipment" capacity demand of 16 is too high
	 */
	@Test
	void testVehicleCapacityShipments_fails() {

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCTestCarriersShipmentsFAIL.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_FAILED, CarrierConsistencyCheckers.vehicleCapacityCheck(carriers, lvl), "At least one vehicle of every carrier has enough capacity for the largest job!");
	}

	/**
	 * This test will check if the vehicles of carriers c1 and c2 have enough capacity to handle the given SERVICES.
	 */
	@Test
	void testVehicleCapacityServices_passes() {

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCTestCarriersServicesPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, CarrierConsistencyCheckers.vehicleCapacityCheck(carriers, lvl), "At least one carrier has no sufficient vehicle!");
	}

	/**
	 * This test will check if the vehicles of carriers c1 and c2 have enough capacity to handle the given jobs.
	 * ccCarrierWithServices1: service "extra_large_service" capacity demand of 31 is too high
	 * ccCarrierWithServices2: service "extra_large_service" capacity demand of 33 is too high
	 */
	@Test
	void testVehicleCapacityServices_fails() {

		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCTestCarriersServicesFAIL.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		Assertions.assertEquals(CHECK_FAILED, CarrierConsistencyCheckers.vehicleCapacityCheck(carriers,lvl), "At least one vehicle of every carrier has enough capacity for the largest job.");
	}
}
