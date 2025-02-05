/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import java.net.URL;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

public class CarrierControllerUtilsTest{

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private static final Logger log = LogManager.getLogger( CarrierControllerUtilsTest.class );

	private final Id<Carrier> CARRIER_SERVICES_ID = Id.create("CarrierWServices", Carrier.class);
	private final Id<Carrier> CARRIER_SHIPMENTS_ID = Id.create("CarrierWShipments", Carrier.class);

	private Carrier carrierWServices;
	private Carrier carrierWShipments;

	private Carrier carrierWShipmentsOnlyFromCarrierWServices;
	private Carrier carrierWShipmentsOnlyFromCarrierWShipments;

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@BeforeEach
	public void setUp() {

		//Create carrier with services and shipments
		Carriers carriersWithServicesAndShipments = new Carriers();
		carrierWServices = CarriersUtils.createCarrier(CARRIER_SERVICES_ID );
		CarrierService service1 = createMatsimService("Service1", "i(3,9)", 2);
		CarriersUtils.addService(carrierWServices, service1);
		CarrierService service2 = createMatsimService("Service2", "i(4,9)", 2);
		CarriersUtils.addService(carrierWServices, service2);

		//Create carrier with shipments
		carrierWShipments = CarriersUtils.createCarrier(CARRIER_SHIPMENTS_ID );
		CarrierShipment shipment1 = createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1);
		CarriersUtils.addShipment(carrierWShipments, shipment1);
		CarrierShipment shipment2 = createMatsimShipment("shipment2", "i(3,0)", "i(3,7)", 2);
		CarriersUtils.addShipment(carrierWShipments, shipment2);

		//Create vehicle for Carriers
		final Id<VehicleType> vehicleTypeId = Id.create( "gridType", VehicleType.class );
		VehicleType carrierVehType = VehicleUtils.getFactory().createVehicleType( vehicleTypeId );
		VehicleUtils.setHbefaTechnology(carrierVehType.getEngineInformation(), "diesel");
		VehicleUtils.setFuelConsumptionLitersPerMeter(carrierVehType.getEngineInformation(), 0.015);
		VehicleCapacity vehicleCapacity = carrierVehType.getCapacity();
		vehicleCapacity.setOther( 3 );
		CostInformation costInfo = carrierVehType.getCostInformation();
		costInfo.setCostsPerMeter( 0.0001 ) ;
		costInfo.setCostsPerSecond( 0.001 ) ;
		costInfo.setFixedCost( 130. ) ;
//		VehicleType carrierVehType = CarriersUtils.CarrierVehicleTypeBuilder.newInstance( vehicleTypeId )
		carrierVehType.setMaximumVelocity( 10. ) ;
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);

		CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)"),
				carrierVehType ).setEarliestStart(0.0 ).setLatestEnd(36000.0 ).build();
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance()
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE);
		carrierWServices.setCarrierCapabilities(ccBuilder.build());
		carrierWShipments.setCarrierCapabilities(ccBuilder.build());

		// Add both carriers
		carriersWithServicesAndShipments.addCarrier(carrierWServices);
		carriersWithServicesAndShipments.addCarrier(carrierWShipments);

		//load Network and build netbasedCosts for jsprit
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(testUtils.getPackageInputDirectory() + "grid-network.xml");
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.


		//Build jsprit, solve and route VRP for carrierService only -> need solution to convert Services to Shipments
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrierWServices, network);
		vrpBuilder.setRoutingCost(netBasedCosts) ;
		VehicleRoutingProblem problem = vrpBuilder.build();

			// get the algorithm out-of-the-box, search solution and get the best one.
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

			//Routing bestPlan to Network
		CarrierPlan carrierPlanServicesAndShipments = MatsimJspritFactory.createPlan(carrierWServices, bestSolution) ;
		NetworkRouter.routePlan(carrierPlanServicesAndShipments,netBasedCosts) ;
		carrierWServices.addPlan(carrierPlanServicesAndShipments) ;

		/*
		 * Now convert it to an only shipment-based VRP.
		 */

		//Convert to jsprit VRP
		Carriers carriersWithShipmentsOnly = CarriersUtils.createShipmentVRPCarrierFromServiceVRPSolution(carriersWithServicesAndShipments);
		carrierWShipmentsOnlyFromCarrierWServices = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SERVICES_ID);		//with converted Service
		carrierWShipmentsOnlyFromCarrierWShipments = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SHIPMENTS_ID);		//with copied Shipments
	}


	//Should only have Services
	@Test
	void numberOfInitialServicesIsCorrect() {
		Assertions.assertEquals(2, carrierWServices.getServices().size());

		int demandServices = 0;
		for (CarrierService carrierService : carrierWServices.getServices().values()) {
			demandServices += carrierService.getCapacityDemand();
		}
		Assertions.assertEquals(4, demandServices);

		Assertions.assertEquals(0, carrierWServices.getShipments().size());
	}

	//Should only have Shipments
	@Test
	void numberOfInitialShipmentsIsCorrect() {
		Assertions.assertEquals(0, carrierWShipments.getServices().size());

		Assertions.assertEquals(2, carrierWShipments.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipments.getShipments().values()) {
            demandShipments += carrierShipment.getCapacityDemand();
		}
		Assertions.assertEquals(3, demandShipments);
	}

	@Test
	void numberOfShipmentsFromCopiedShipmentsIsCorrect() {
		Assertions.assertEquals(0, carrierWShipmentsOnlyFromCarrierWShipments.getServices().size());

		Assertions.assertEquals(2, carrierWShipmentsOnlyFromCarrierWShipments.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipmentsOnlyFromCarrierWServices.getShipments().values()) {
            demandShipments += carrierShipment.getCapacityDemand();
		}
		Assertions.assertEquals(4, demandShipments);
	}

	@Test
	void numberOfShipmentsFromConvertedServicesIsCorrect() {
		Assertions.assertEquals(0, carrierWShipmentsOnlyFromCarrierWServices.getServices().size());

		Assertions.assertEquals(2, carrierWShipmentsOnlyFromCarrierWServices.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipmentsOnlyFromCarrierWServices.getShipments().values()) {
            demandShipments += carrierShipment.getCapacityDemand();
		}
		Assertions.assertEquals(4, demandShipments);
	}

	@Test
	void fleetAvailableAfterConvertingIsCorrect() {
		Assertions.assertEquals(FleetSize.INFINITE, carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getFleetSize());
		Assertions.assertEquals(1, carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getVehicleTypes().size());
		for ( VehicleType carrierVehicleType : carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getVehicleTypes()){
			Assertions.assertEquals(3., carrierVehicleType.getCapacity().getOther(), Double.MIN_VALUE );
			Assertions.assertEquals(130, carrierVehicleType.getCostInformation().getFixedCosts(), 0.0 );
			Assertions.assertEquals(0.0001, carrierVehicleType.getCostInformation().getCostsPerMeter(), 0.0 );
			Assertions.assertEquals(0.001, carrierVehicleType.getCostInformation().getCostsPerSecond(), 0.0 );
			Assertions.assertEquals(10, carrierVehicleType.getMaximumVelocity(), 0.0);
			Assertions.assertEquals("diesel", VehicleUtils.getHbefaTechnology(carrierVehicleType.getEngineInformation()));
			Assertions.assertEquals(0.015, VehicleUtils.getFuelConsumptionLitersPerMeter(carrierVehicleType.getEngineInformation()), 0.0);
		}

		Assertions.assertEquals(FleetSize.INFINITE, carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getFleetSize());
		Assertions.assertEquals(1, carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getVehicleTypes().size());
		for ( VehicleType carrierVehicleType : carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getVehicleTypes()){
			Assertions.assertEquals(3., carrierVehicleType.getCapacity().getOther(), Double.MIN_VALUE );
			Assertions.assertEquals(130, carrierVehicleType.getCostInformation().getFixedCosts(), 0.0 );
			Assertions.assertEquals(0.0001, carrierVehicleType.getCostInformation().getCostsPerMeter(), 0.0 );
			Assertions.assertEquals(0.001, carrierVehicleType.getCostInformation().getCostsPerSecond(), 0.0 );
			Assertions.assertEquals(10, carrierVehicleType.getMaximumVelocity(), 0.0);
			Assertions.assertEquals("diesel", VehicleUtils.getHbefaTechnology(carrierVehicleType.getEngineInformation()));
			Assertions.assertEquals(0.015, VehicleUtils.getFuelConsumptionLitersPerMeter(carrierVehicleType.getEngineInformation()), 0.0);		}
	}

	@Test
	void copyingOfShipmentsIsDoneCorrectly() {
		boolean foundShipment1 = false;
		boolean foundShipment2 = false;
		CarrierShipment carrierShipment1 = CarriersUtils.getShipment(carrierWShipmentsOnlyFromCarrierWShipments, Id.create("shipment1", CarrierShipment.class));
		assert carrierShipment1 != null;
		if (carrierShipment1.getId() == Id.create("shipment1", CarrierShipment.class)) {
				System.out.println("Found Shipment1");
				foundShipment1 = true;
				Assertions.assertEquals(Id.createLinkId("i(1,0)"), carrierShipment1.getPickupLinkId());
				Assertions.assertEquals(Id.createLinkId("i(7,6)R"), carrierShipment1.getDeliveryLinkId());
            Assertions.assertEquals(1, carrierShipment1.getCapacityDemand());
				Assertions.assertEquals(30.0, carrierShipment1.getDeliveryDuration(), 0);
			Assertions.assertEquals(3600.0, carrierShipment1.getDeliveryStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(36000.0, carrierShipment1.getDeliveryStartingTimeWindow().getEnd(), 0);
				Assertions.assertEquals(5.0, carrierShipment1.getPickupDuration(), 0);
			Assertions.assertEquals(0.0, carrierShipment1.getPickupStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(7200.0, carrierShipment1.getPickupStartingTimeWindow().getEnd(), 0);
			}
		CarrierShipment carrierShipment2 = CarriersUtils.getShipment(carrierWShipmentsOnlyFromCarrierWShipments, Id.create("shipment2", CarrierShipment.class));
		assert carrierShipment2 != null;
		if (carrierShipment2.getId() == Id.create("shipment2", CarrierShipment.class)) {
				System.out.println("Found Shipment2");
				foundShipment2 = true;
				Assertions.assertEquals(Id.createLinkId("i(3,0)"), carrierShipment2.getPickupLinkId());
				Assertions.assertEquals(Id.createLinkId("i(3,7)"), carrierShipment2.getDeliveryLinkId());
            Assertions.assertEquals(2, carrierShipment2.getCapacityDemand());
				Assertions.assertEquals(30.0, carrierShipment2.getDeliveryDuration(), 0);
			Assertions.assertEquals(3600.0, carrierShipment2.getDeliveryStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(36000.0, carrierShipment2.getDeliveryStartingTimeWindow().getEnd(), 0);
				Assertions.assertEquals(5.0, carrierShipment2.getPickupDuration(), 0);
			Assertions.assertEquals(0.0, carrierShipment2.getPickupStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(7200.0, carrierShipment2.getPickupStartingTimeWindow().getEnd(), 0);
			}
		Assertions.assertTrue(foundShipment1, "Not found Shipment1 after copying");
		Assertions.assertTrue(foundShipment2, "Not found Shipment2 after copying");
	}


	@Test
	void convertionOfServicesIsDoneCorrectly() {
		boolean foundService1 = false;
		boolean foundService2 = false;
		CarrierShipment carrierShipment1 = CarriersUtils.getShipment(carrierWShipmentsOnlyFromCarrierWServices, Id.create("Service1", CarrierShipment.class));
		assert carrierShipment1 != null;
		if (carrierShipment1.getId() == Id.create("Service1", CarrierShipment.class)) {
				foundService1 = true;
				Assertions.assertEquals(Id.createLinkId("i(6,0)"), carrierShipment1.getPickupLinkId());
				Assertions.assertEquals(Id.createLinkId("i(3,9)"), carrierShipment1.getDeliveryLinkId());
            Assertions.assertEquals(2, carrierShipment1.getCapacityDemand());
				Assertions.assertEquals(31.0, carrierShipment1.getDeliveryDuration(), 0);
			Assertions.assertEquals(3601.0, carrierShipment1.getDeliveryStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(36001.0, carrierShipment1.getDeliveryStartingTimeWindow().getEnd(), 0);
				Assertions.assertEquals(0.0, carrierShipment1.getPickupDuration(), 0);
			Assertions.assertEquals(0.0, carrierShipment1.getPickupStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(36001.0, carrierShipment1.getPickupStartingTimeWindow().getEnd(), 0);
			}
		CarrierShipment carrierShipment2 = CarriersUtils.getShipment(carrierWShipmentsOnlyFromCarrierWServices, Id.create("Service2", CarrierShipment.class));
		assert carrierShipment2 != null;
		if (carrierShipment2.getId() == Id.create("Service2", CarrierShipment.class)) {
				foundService2 = true;
				Assertions.assertEquals(Id.createLinkId("i(6,0)"), carrierShipment2.getPickupLinkId());
				Assertions.assertEquals(Id.createLinkId("i(4,9)"), carrierShipment2.getDeliveryLinkId());
            Assertions.assertEquals(2, carrierShipment2.getCapacityDemand());
				Assertions.assertEquals(31.0, carrierShipment2.getDeliveryDuration(), 0);
			Assertions.assertEquals(3601.0, carrierShipment2.getDeliveryStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(36001.0, carrierShipment2.getDeliveryStartingTimeWindow().getEnd(), 0);
				Assertions.assertEquals(0.0, carrierShipment2.getPickupDuration(), 0);
			Assertions.assertEquals(0.0, carrierShipment2.getPickupStartingTimeWindow().getStart(), 0);
			Assertions.assertEquals(36001.0, carrierShipment2.getPickupStartingTimeWindow().getEnd(), 0);
			}
		Assertions.assertTrue(foundService1, "Not found converted Service1 after converting");
		Assertions.assertTrue(foundService2, "Not found converted Service2 after converting");
	}

	/*Note: This test can be removed / modified when jsprit works properly with a combined Service and Shipment VRP.
	* Currently, the capacity of the vehicle seems to be "ignored" in a way that the load within the tour is larger than the capacity;
	* Maybe it is because of the misunderstanding, that a Service is modeled as "Pickup" and not as thought before as "Delivery". KMT sep18
	*/
	@Test
	void exceptionIsThrownWhenUsingMixedShipmentsAndServices() {
		assertThrows(UnsupportedOperationException.class, () -> {
			Carrier carrierMixedWServicesAndShipments = CarriersUtils.createCarrier(Id.create("CarrierMixed", Carrier.class));
			CarrierService service1 = createMatsimService("Service1", "i(3,9)", 2);
			CarriersUtils.addService(carrierMixedWServicesAndShipments, service1);
			CarrierShipment shipment1 = createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1);
			CarriersUtils.addShipment(carrierMixedWServicesAndShipments, shipment1);

			Network network = NetworkUtils.createNetwork();
			new MatsimNetworkReader(network).readFile(testUtils.getPackageInputDirectory() + "grid-network.xml");

			MatsimJspritFactory.createRoutingProblemBuilder(carrierMixedWServicesAndShipments, network);
		});
	}

	private static CarrierShipment createMatsimShipment(String id, String from, String to, int size) {
		Id<CarrierShipment> shipmentId = Id.create(id, CarrierShipment.class);
		Id<Link> fromLinkId = null;
		Id<Link> toLinkId= null;

		if(from != null ) {
			fromLinkId = Id.create(from, Link.class);
		}
		if(to != null ) {
			toLinkId = Id.create(to, Link.class);
		}

		return CarrierShipment.Builder.newInstance(shipmentId, fromLinkId, toLinkId, size)
				.setDeliveryDuration(30.0)
				.setDeliveryStartingTimeWindow(TimeWindow.newInstance(3600.0, 36000.0))
				.setPickupDuration(5.0)
				.setPickupStartingTimeWindow(TimeWindow.newInstance(0.0, 7200.0))
				.build();
	}

	private static CarrierService createMatsimService(String id, String to, int size) {
		CarrierService.Builder builder = CarrierService.Builder.newInstance(Id.create(id, CarrierService.class), Id.create(to, Link.class))
				.setCapacityDemand(size)
				.setServiceDuration(31.0);
		return builder.setServiceStartingTimeWindow(TimeWindow.newInstance(3601.0, 36001.0))
				.build();
	}

	@Test
	void testAddVehicleTypeSkill(){
		VehiclesFactory factory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getVehicles().getFactory();
		VehicleType type = factory.createVehicleType(Id.create("test", VehicleType.class));
		Assertions.assertFalse(CarriersUtils.hasSkill(type, "testSkill"), "Should not have skill.");

		CarriersUtils.addSkill(type, "testSkillOne");
		Assertions.assertTrue(CarriersUtils.hasSkill(type, "testSkillOne"), "Should have skill 'testSkillOne'.");


		CarriersUtils.addSkill(type, "testSkillTwo");
		Assertions.assertTrue(CarriersUtils.hasSkill(type, "testSkillOne"), "Should have skill 'testSkillOne'.");
		Assertions.assertTrue(CarriersUtils.hasSkill(type, "testSkillTwo"), "Should have skill 'testSkillTwo'.");
	}

	@Test
	void testAddShipmentSkill(){
		CarrierShipment shipment = CarrierShipment.Builder.newInstance(
				Id.create("testShipment", CarrierShipment.class), Id.createLinkId("1"), Id.createLinkId("2"), 1)
				.build();
		Assertions.assertFalse(CarriersUtils.hasSkill(shipment, "testSkill"), "Should not have skill.");

		CarriersUtils.addSkill(shipment, "testSkillOne");
		Assertions.assertTrue(CarriersUtils.hasSkill(shipment, "testSkillOne"), "Should have skill 'testSkillOne'.");


		CarriersUtils.addSkill(shipment, "testSkillTwo");
		Assertions.assertTrue(CarriersUtils.hasSkill(shipment, "testSkillOne"), "Should have skill 'testSkillOne'.");
		Assertions.assertTrue(CarriersUtils.hasSkill(shipment, "testSkillTwo"), "Should have skill 'testSkillTwo'.");
	}

	@Test
	void testAddServiceSkill(){
		CarrierService service = CarrierService.Builder.newInstance(
				Id.create("testShipment", CarrierService.class), Id.createLinkId("2"))
				.build();
		Assertions.assertFalse(CarriersUtils.hasSkill(service, "testSkill"), "Should not have skill.");

		CarriersUtils.addSkill(service, "testSkillOne");
		Assertions.assertTrue(CarriersUtils.hasSkill(service, "testSkillOne"), "Should have skill 'testSkillOne'.");


		CarriersUtils.addSkill(service, "testSkillTwo");
		Assertions.assertTrue(CarriersUtils.hasSkill(service, "testSkillOne"), "Should have skill 'testSkillOne'.");
		Assertions.assertTrue(CarriersUtils.hasSkill(service, "testSkillTwo"), "Should have skill 'testSkillTwo'.");
	}

	@Test
	void testRunJsprit_allInformationGiven(){
		Config config = prepareConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
		String vraFile= IOUtils.extendUrl(scenarioUrl, "algorithm_v2.xml" ).toString();

		FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class ) ;
		freightConfig.setVehicleRoutingAlgorithmFileFile(vraFile);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario );
		Controller controller = ControllerUtils.createController(scenario);

		try {
			CarriersUtils.runJsprit(scenario);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail();
		}

		Assertions.assertEquals(vraFile, ConfigUtils.addOrGetModule( controller.getConfig(), FreightCarriersConfigGroup.class ).getVehicleRoutingAlgorithmFile());
	}

	/**
	 * This test should lead to an exception, because the NumberOfJspritIterations is not set for carriers.
	 */
	@Test
	void testRunJsprit_NoOfJspritIterationsMissing() {
		assertThrows(java.util.concurrent.ExecutionException.class, () -> {
			Config config = prepareConfig();
			config.controller().setOutputDirectory(utils.getOutputDirectory());
			Scenario scenario = ScenarioUtils.loadScenario(config);

			CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

			//remove all attributes --> remove the NumberOfJspritIterations attribute to trigger exception
			Carriers carriers = CarriersUtils.getCarriers(scenario);
			for (Carrier carrier : carriers.getCarriers().values()) {
				carrier.getAttributes().clear();
			}

			CarriersUtils.runJsprit(scenario);
		});
	}

	/**
	 * Don't crash even if there is no algorithm file specified.
	 */
	@Test
	void testRunJsprit_NoAlgorithmFileGiven(){
		Config config = prepareConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		try {
			CarriersUtils.runJsprit(scenario);
		} catch (Exception e) {
			Assertions.fail();
		}
		Assertions.assertNull(ConfigUtils.addOrGetModule(scenario.getConfig(), FreightCarriersConfigGroup.class).getVehicleRoutingAlgorithmFile());
	}

	private Config prepareConfig(){
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(scenarioUrl, "config.xml" ) );
		config.controller().setLastIteration(0);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		//freight config stuff
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(IOUtils.extendUrl(scenarioUrl, "singleCarrierFiveActivitiesWithoutRoutes.xml" ).toString() );
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(IOUtils.extendUrl(scenarioUrl, "vehicleTypes.xml" ).toString() );
		freightCarriersConfigGroup.setTravelTimeSliceWidth(24*3600);
		freightCarriersConfigGroup.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings);
		return config;
	}


}
