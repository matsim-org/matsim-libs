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

	//Todo: @ Anton: Habe hier auch noch nit im Detail rein geschaut. Was mir sofort auffällt: Bitte analog zum anderen Test:
	// 1.) Beschreibung / JavaDoc vor dei Methode
	// 2.) Versuche die Inputs per Inlinign wie im anderen Test einzukürzen
	// 3.) Assertions statt Assert
	// 4.) Weg mit System.out.println
	// 5.) Schauen, dass die Textaussage bei den Assertions so ist, dass sie einem weiterhilft, wenn der Test **fehlschlägt**.

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
		System.out.println(areCarriersCapable);
		Assert.isTrue(areCarriersCapable, "All jobs can be handled.");
	}
	@Test
	void testVehicleScheduleShipment_failes() {
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
		 * - carrier "ccCarrierWithShipments1":
		 * 		small_service_1: no vehicle in operation
		 * 		medium_service_1: vehicle in operation is too small
		 *		extra_large_service_1: vehicle in operation is too small
		 * - carrier "ccCarrierWithShipment2":
		 *		extra_large_service_2: vehicles are too small
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
