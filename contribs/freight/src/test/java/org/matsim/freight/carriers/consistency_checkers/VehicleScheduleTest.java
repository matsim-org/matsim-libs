package org.matsim.freight.carriers.consistency_checkers;

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

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	//@ Anton: Habe hier auch noch nit im Detail rein geschaut. Was mir sofort auffällt: Bitte analog zum anderen Test:
	// -> bis auf 3. erledigt
	//
	// 3.) Assertions statt Assert -> TODO: Umbau mache ich zusammen mit der Umstellung auf Enum
	//
	//

	@Test
	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 * This test should return true.
	 */
	void testVehicleScheduleShipment_passes() {

		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersShipmentsPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assert.isTrue(areCarriersCapable, "At least one job can NOT be handled.");
	}
	@Test
	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 * This test should return false, because:
	 * - carrier "ccCarrierWithShipments1":
	 * 		no vehicle is in operation when shipment "small_shipment_1" needs to be delivered
	 * 		shipment "large_shipment_1" is too big for all vehicles
	 *
	 * - carrier "ccCarrierWithShipment2":
	 * 		no vehicle is not in operation when shipment "small_shipment_2" needs to be picked up
	 * 		shipment "large_shipment_2" is too big for all vehicles
	 */
	void testVehicleScheduleShipment_failes() {

		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersShipmentsFAIL.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assertions.assertFalse(areCarriersCapable, "All jobs can be handled.");
	}

	@Test
	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 * This test should return true.
	 */
	void testVehicleScheduleService_passes() {

		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersServicesPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assert.isTrue(areCarriersCapable, "At least one job can NOT be handled.");
	}
	@Test
	/**
	 * This test will check if the given jobs can be handled by the carriers -> A vehicle with sufficient capacity has to be in operation.
	 * This test should return false, because:
	 * - carrier "ccCarrierWithShipments1":
	 * 		small_service_1: no vehicle in operation
	 * 		medium_service_1: vehicle in operation is too small
	 *		extra_large_service_1: vehicle in operation is too small
	 * - carrier "ccCarrierWithShipment2":
	 *		extra_large_service_2: vehicles are too small
	 */
	void testVehicleScheduleService_failes() {

		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriersServicesFAIL.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		boolean areCarriersCapable = CarrierConsistencyCheckers.vehicleScheduleTest(carriers);
		Assertions.assertFalse(areCarriersCapable, "All jobs can be handled.");
	}

}
