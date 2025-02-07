package org.matsim.freight.carriers.consistency_checkers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
 *
 *  @author antonstock
 *	This class checks, if all shipments can be transported -> vehicle has to be large enough and in operation during pickup/delivery times.
 *
 */
public class VehicleScheduleTest {

	private static final Logger log = LogManager.getLogger(VehicleScheduleTest.class);

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testVehicleScheduleShipment_passes() {
	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 * This test should return true.
	 */
		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();
		//names of xml-files
		String carriersXML = "CCTestCarriersShipmentsPASS.xml";
		String vehicleXML = "CCTestVeh.xml";

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + carriersXML);
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + vehicleXML);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assert.isTrue(areCarriersCapable, "All jobs can be handled.");
	}
	@Test
	void testVehicleScheduleShipment_failes() {
		/**
		 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
		 * This test should return false, because:
		 * - carrier "ccCarrierWithShipments1"'s vehicle "cargo_bike" is not in operation when shipment "small_shipment" needs to be picked up.
		 * - carrier "ccCarrierWithShipment2"'s vehicle "medium_truck" is not in operation when shipment "large_shipment" needs to be delivered.
		 */
		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();
		//names of xml-files
		String carriersXML = "CCTestCarriersShipmentsFAIL.xml";
		String vehicleXML = "CCTestVeh.xml";

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + carriersXML);
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + vehicleXML);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assertions.assertFalse(areCarriersCapable, "At least one job can not be handled.");
	}

	@Test
	void testVehicleScheduleService_passes() {
		/**
		 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
		 * This test should return true.
		 */
		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();
		//names of xml-files
		String carriersXML = "CCTestCarriersServicesPASS.xml";
		String vehicleXML = "CCTestVeh.xml";

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + carriersXML);
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + vehicleXML);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assert.isTrue(areCarriersCapable, "All jobs can be handled.");
	}
	@Test
	void testVehicleScheduleService_failes() {
		/**
		 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
		 * This test should return false, because:
		 * - carrier "ccCarrierWithShipments1"'s vehicle "cargo_bike" is not in operation when shipment "small_shipment" needs to be picked up.
		 * - carrier "ccCarrierWithShipment2"'s vehicle "medium_truck" is not in operation when shipment "large_shipment" needs to be delivered.
		 */
		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();
		//names of xml-files
		String carriersXML = "CCTestCarriersServicesFAIL.xml";
		String vehicleXML = "CCTestVeh.xml";

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + carriersXML);
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + vehicleXML);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assertions.assertFalse(areCarriersCapable, "At least one job can not be handled.");
	}

}
