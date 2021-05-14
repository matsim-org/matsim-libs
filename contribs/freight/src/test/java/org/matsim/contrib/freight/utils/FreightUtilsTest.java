/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.freight.utils;

import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

import org.junit.Assert;

public class FreightUtilsTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private static final Logger log = Logger.getLogger(FreightUtilsTest.class);

	private final Id<Carrier> CARRIER_SERVICES_ID = Id.create("CarrierWServices", Carrier.class);
	private final Id<Carrier> CARRIER_SHIPMENTS_ID = Id.create("CarrierWShipments", Carrier.class);

	private Carrier carrierWServices;
	private Carrier carrierWShipments;

	private Carrier carrierWShipmentsOnlyFromCarrierWServices;
	private Carrier carrierWShipmentsOnlyFromCarrierWShipments;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Before
	public void setUp() {

		//Create carrier with services and shipments
		Carriers carriersWithServicesAndShpiments = new Carriers();
		carrierWServices = CarrierUtils.createCarrier(CARRIER_SERVICES_ID );
		CarrierService service1 = createMatsimService("Service1", "i(3,9)", 2);
		CarrierUtils.addService(carrierWServices, service1);
		CarrierService service2 = createMatsimService("Service2", "i(4,9)", 2);
		CarrierUtils.addService(carrierWServices, service2);

		//Create carrier with shipments
		carrierWShipments = CarrierUtils.createCarrier(CARRIER_SHIPMENTS_ID );
		CarrierShipment shipment1 = createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1);
		CarrierUtils.addShipment(carrierWShipments, shipment1);
		CarrierShipment shipment2 = createMatsimShipment("shipment2", "i(3,0)", "i(3,7)", 2);
		CarrierUtils.addShipment(carrierWShipments, shipment2);

		//Create vehicle for Carriers
		final Id<VehicleType> vehicleTypeId = Id.create( "gridType", VehicleType.class );
		VehicleType carrierVehType = VehicleUtils.getFactory().createVehicleType( vehicleTypeId );
		VehicleUtils.setHbefaTechnology(carrierVehType.getEngineInformation(), "diesel");
		VehicleUtils.setFuelConsumption(carrierVehType, 0.015);
		VehicleCapacity vehicleCapacity = carrierVehType.getCapacity();
		vehicleCapacity.setOther( 3 );
		CostInformation costInfo = carrierVehType.getCostInformation();
		costInfo.setCostsPerMeter( 0.0001 ) ;
		costInfo.setCostsPerSecond( 0.001 ) ;
		costInfo.setFixedCost( 130. ) ;
//		VehicleType carrierVehType = CarrierUtils.CarrierVehicleTypeBuilder.newInstance( vehicleTypeId )
		carrierVehType.setMaximumVelocity( 10. ) ;
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);

		CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)")).setEarliestStart(0.0).setLatestEnd(36000.0).setTypeId(carrierVehType.getId()).build();
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance()
				.addType(carrierVehType)
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE);
		carrierWServices.setCarrierCapabilities(ccBuilder.build());
		carrierWShipments.setCarrierCapabilities(ccBuilder.build());

		// Add both carriers
		carriersWithServicesAndShpiments.addCarrier(carrierWServices);
		carriersWithServicesAndShpiments.addCarrier(carrierWShipments);

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriersWithServicesAndShpiments).loadVehicleTypes(vehicleTypes) ;

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
		carrierWServices.setSelectedPlan(carrierPlanServicesAndShipments) ;

		/*
		 * Now convert it to a only shipment-based VRP.
		 */

		//Convert to jsprit VRP
		Carriers carriersWithShipmentsOnly = FreightUtils.createShipmentVRPCarrierFromServiceVRPSolution(carriersWithServicesAndShpiments);
		carrierWShipmentsOnlyFromCarrierWServices = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SERVICES_ID);		//with converted Service
		carrierWShipmentsOnlyFromCarrierWShipments = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SHIPMENTS_ID);		//with copied Shipments

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriersWithShipmentsOnly).loadVehicleTypes(vehicleTypes) ;
	}


	@Test //Should only have Services
	public void numberOfInitalServicesIsCorrect() {
		Assert.assertEquals(2, carrierWServices.getServices().size());

		int demandServices = 0;
		for (CarrierService carrierService : carrierWServices.getServices().values()) {
			demandServices += carrierService.getCapacityDemand();
		}
		Assert.assertEquals(4, demandServices);

		Assert.assertEquals(0, carrierWServices.getShipments().size());
	}

	@Test //Should only have Shipments
	public void numberOfInitialShipmentsIsCorrect() {
		Assert.assertEquals(0, carrierWShipments.getServices().size());

		Assert.assertEquals(2, carrierWShipments.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipments.getShipments().values()) {
			demandShipments += carrierShipment.getSize();
		}
		Assert.assertEquals(3, demandShipments);
	}

	@Test
	public void numberOfShipmentsFromCopiedShipmentsIsCorrect() {
		Assert.assertEquals(0, carrierWShipmentsOnlyFromCarrierWShipments.getServices().size());

		Assert.assertEquals(2, carrierWShipmentsOnlyFromCarrierWShipments.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipmentsOnlyFromCarrierWServices.getShipments().values()) {
			demandShipments += carrierShipment.getSize();
		}
		Assert.assertEquals(4, demandShipments);
	}

	@Test
	public void numberOfShipmentsFromConvertedServicesIsCorrect() {
		Assert.assertEquals(0, carrierWShipmentsOnlyFromCarrierWServices.getServices().size());

		Assert.assertEquals(2, carrierWShipmentsOnlyFromCarrierWServices.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipmentsOnlyFromCarrierWServices.getShipments().values()) {
			demandShipments += carrierShipment.getSize();
		}
		Assert.assertEquals(4, demandShipments);
	}

	@Test
	public void fleetAvailableAfterConvertingIsCorrect() {
		Assert.assertEquals(FleetSize.INFINITE, carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(1, carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getVehicleTypes().size());
		for ( VehicleType carrierVehicleType : carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getVehicleTypes()){
			Assert.assertEquals(3., carrierVehicleType.getCapacity().getOther(), Double.MIN_VALUE );
			Assert.assertEquals(130, carrierVehicleType.getCostInformation().getFixedCosts(), 0.0 );
			Assert.assertEquals(0.0001, carrierVehicleType.getCostInformation().getCostsPerMeter(), 0.0 );
			Assert.assertEquals(0.001, carrierVehicleType.getCostInformation().getCostsPerSecond(), 0.0 );
			Assert.assertEquals(10, carrierVehicleType.getMaximumVelocity(), 0.0);
			Assert.assertEquals("diesel", VehicleUtils.getHbefaTechnology(carrierVehicleType.getEngineInformation()));
			Assert.assertEquals(0.015, VehicleUtils.getFuelConsumption(carrierVehicleType), 0.0);
		}

		Assert.assertEquals(FleetSize.INFINITE, carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(1, carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getVehicleTypes().size());
		for ( VehicleType carrierVehicleType : carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getVehicleTypes()){
			Assert.assertEquals(3., carrierVehicleType.getCapacity().getOther(), Double.MIN_VALUE );
			Assert.assertEquals(130, carrierVehicleType.getCostInformation().getFixedCosts(), 0.0 );
			Assert.assertEquals(0.0001, carrierVehicleType.getCostInformation().getCostsPerMeter(), 0.0 );
			Assert.assertEquals(0.001, carrierVehicleType.getCostInformation().getCostsPerSecond(), 0.0 );
			Assert.assertEquals(10, carrierVehicleType.getMaximumVelocity(), 0.0);
			Assert.assertEquals("diesel", VehicleUtils.getHbefaTechnology(carrierVehicleType.getEngineInformation()));
			Assert.assertEquals(0.015, VehicleUtils.getFuelConsumption(carrierVehicleType), 0.0);		}
	}

	@Test
	public void copiingOfShipmentsIsDoneCorrectly() {
		boolean foundShipment1 = false;
		boolean foundShipment2 = false;
		CarrierShipment carrierShipment1 = CarrierUtils.getShipment(carrierWShipmentsOnlyFromCarrierWShipments, Id.create("shipment1", CarrierShipment.class));
		assert carrierShipment1 != null;
		if (carrierShipment1.getId() == Id.create("shipment1", CarrierShipment.class)) {
				System.out.println("Found Shipment1");
				foundShipment1 = true;
				Assert.assertEquals(Id.createLinkId("i(1,0)"), carrierShipment1.getFrom());
				Assert.assertEquals(Id.createLinkId("i(7,6)R"), carrierShipment1.getTo());
				Assert.assertEquals(1, carrierShipment1.getSize());
				Assert.assertEquals(30.0, carrierShipment1.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3600.0, carrierShipment1.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36000.0, carrierShipment1.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(5.0, carrierShipment1.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment1.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(7200.0, carrierShipment1.getPickupTimeWindow().getEnd(), 0);
			}
		CarrierShipment carrierShipment2 = CarrierUtils.getShipment(carrierWShipmentsOnlyFromCarrierWShipments, Id.create("shipment2", CarrierShipment.class));
		assert carrierShipment2 != null;
		if (carrierShipment2.getId() == Id.create("shipment2", CarrierShipment.class)) {
				System.out.println("Found Shipment2");
				foundShipment2 = true;
				Assert.assertEquals(Id.createLinkId("i(3,0)"), carrierShipment2.getFrom());
				Assert.assertEquals(Id.createLinkId("i(3,7)"), carrierShipment2.getTo());
				Assert.assertEquals(2, carrierShipment2.getSize());
				Assert.assertEquals(30.0, carrierShipment2.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3600.0, carrierShipment2.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36000.0, carrierShipment2.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(5.0, carrierShipment2.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment2.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(7200.0, carrierShipment2.getPickupTimeWindow().getEnd(), 0);
			}
		Assert.assertTrue("Not found Shipment1 after copiing", foundShipment1);
		Assert.assertTrue("Not found Shipment2 after copiing", foundShipment2);
	}


	@Test
	public void convertionOfServicesIsDoneCorrectly() {
		boolean foundSercice1 = false;
		boolean foundService2 = false;
		CarrierShipment carrierShipment1 = CarrierUtils.getShipment(carrierWShipmentsOnlyFromCarrierWServices, Id.create("Service1", CarrierShipment.class));
		assert carrierShipment1 != null;
		if (carrierShipment1.getId() == Id.create("Service1", CarrierShipment.class)) {
				foundSercice1 = true;
				Assert.assertEquals(Id.createLinkId("i(6,0)"), carrierShipment1.getFrom());
				Assert.assertEquals(Id.createLinkId("i(3,9)"), carrierShipment1.getTo());
				Assert.assertEquals(2, carrierShipment1.getSize());
				Assert.assertEquals(31.0, carrierShipment1.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3601.0, carrierShipment1.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment1.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(0.0, carrierShipment1.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment1.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment1.getPickupTimeWindow().getEnd(), 0);
			}
		CarrierShipment carrierShipment2 = CarrierUtils.getShipment(carrierWShipmentsOnlyFromCarrierWServices, Id.create("Service2", CarrierShipment.class));
		assert carrierShipment2 != null;
		if (carrierShipment2.getId() == Id.create("Service2", CarrierShipment.class)) {
				foundService2 = true;
				Assert.assertEquals(Id.createLinkId("i(6,0)"), carrierShipment2.getFrom());
				Assert.assertEquals(Id.createLinkId("i(4,9)"), carrierShipment2.getTo());
				Assert.assertEquals(2, carrierShipment2.getSize());
				Assert.assertEquals(31.0, carrierShipment2.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3601.0, carrierShipment2.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment2.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(0.0, carrierShipment2.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment2.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment2.getPickupTimeWindow().getEnd(), 0);
			}
		Assert.assertTrue("Not found converted Service1 after converting", foundSercice1);
		Assert.assertTrue("Not found converted Service2 after converting", foundService2);
	}

	/* Note: This test can be removed / modified when jsprit works properly with a combined Service and Shipment VRP.
	* Currently the capacity of the vehicle seems to be "ignored" in a way that the load within the tour is larger than the capacity;
	* Maybe it is because of the misunderstanding, that a Service is modeled as "Pickup" and not as thought before as "Delivery". KMT sep18
	*/
	@Test(expected=UnsupportedOperationException.class)
	public void exceptionIsThrownWhenUsingMixedShipmentsAndServices() {
		Carrier carrierMixedWServicesAndShipments = CarrierUtils.createCarrier(Id.create("CarrierMixed", Carrier.class ) );
		CarrierService service1 = createMatsimService("Service1", "i(3,9)", 2);
		CarrierUtils.addService(carrierMixedWServicesAndShipments, service1);
		CarrierShipment shipment1 = createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1);
		CarrierUtils.addShipment(carrierMixedWServicesAndShipments, shipment1);

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(testUtils.getPackageInputDirectory() + "grid-network.xml");

		MatsimJspritFactory.createRoutingProblemBuilder(carrierMixedWServicesAndShipments, network);
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
				.setDeliveryServiceTime(30.0)
				.setDeliveryTimeWindow(TimeWindow.newInstance(3600.0, 36000.0))
				.setPickupServiceTime(5.0)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, 7200.0))
				.build();
	}

	private static CarrierService createMatsimService(String id, String to, int size) {
		return CarrierService.Builder.newInstance(Id.create(id, CarrierService.class), Id.create(to, Link.class))
				.setCapacityDemand(size)
				.setServiceDuration(31.0)
				.setServiceStartTimeWindow(TimeWindow.newInstance(3601.0, 36001.0))
				.build();
	}

	@Test
	public void testAddVehicleTypeSkill(){
		VehiclesFactory factory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getVehicles().getFactory();
		VehicleType type = factory.createVehicleType(Id.create("test", VehicleType.class));
		Assert.assertFalse("Should not have skill.", FreightUtils.hasSkill(type, "testSkill"));

		FreightUtils.addSkill(type, "testSkillOne");
		Assert.assertTrue("Should have skill 'testSkillOne'.", FreightUtils.hasSkill(type, "testSkillOne"));


		FreightUtils.addSkill(type, "testSkillTwo");
		Assert.assertTrue("Should have skill 'testSkillOne'.", FreightUtils.hasSkill(type, "testSkillOne"));
		Assert.assertTrue("Should have skill 'testSkillTwo'.", FreightUtils.hasSkill(type, "testSkillTwo"));
	}

	@Test
	public void testAddShipmentSkill(){
		CarrierShipment shipment = CarrierShipment.Builder.newInstance(
				Id.create("testShipment", CarrierShipment.class), Id.createLinkId("1"), Id.createLinkId("2"), 1)
				.build();
		Assert.assertFalse("Should not have skill.", FreightUtils.hasSkill(shipment, "testSkill"));

		FreightUtils.addSkill(shipment, "testSkillOne");
		Assert.assertTrue("Should have skill 'testSkillOne'.", FreightUtils.hasSkill(shipment, "testSkillOne"));


		FreightUtils.addSkill(shipment, "testSkillTwo");
		Assert.assertTrue("Should have skill 'testSkillOne'.", FreightUtils.hasSkill(shipment, "testSkillOne"));
		Assert.assertTrue("Should have skill 'testSkillTwo'.", FreightUtils.hasSkill(shipment, "testSkillTwo"));
	}

	@Test
	public void testAddServiceSkill(){
		CarrierService service = CarrierService.Builder.newInstance(
				Id.create("testShipment", CarrierService.class), Id.createLinkId("2"))
				.build();
		Assert.assertFalse("Should not have skill.", FreightUtils.hasSkill(service, "testSkill"));

		FreightUtils.addSkill(service, "testSkillOne");
		Assert.assertTrue("Should have skill 'testSkillOne'.", FreightUtils.hasSkill(service, "testSkillOne"));


		FreightUtils.addSkill(service, "testSkillTwo");
		Assert.assertTrue("Should have skill 'testSkillOne'.", FreightUtils.hasSkill(service, "testSkillOne"));
		Assert.assertTrue("Should have skill 'testSkillTwo'.", FreightUtils.hasSkill(service, "testSkillTwo"));
	}

	@Test
	public void testRunJsprit_allInformationGiven(){
		Config config = prepareConfig();
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
		String vraFile= IOUtils.extendUrl(scenarioUrl, "algorithm_v2.xml" ).toString();

		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class ) ;
		freightConfig.setVehicleRoutingAlgorithmFileFile(vraFile);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
		Controler controler = new Controler(scenario);

		try {
			FreightUtils.runJsprit(scenario);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertEquals(vraFile, ConfigUtils.addOrGetModule( controler.getConfig(), FreightConfigGroup.class ).getVehicleRoutingAlgorithmFile());
	}

	/**
	 * This test should lead to an exception, because the NumberOfJspritIterations is not set for carriers.
	 */
	@Test(expected = java.util.concurrent.ExecutionException.class)
	public void testRunJsprit_NoOfJspritIterationsMissing() throws ExecutionException, InterruptedException {
		Config config = prepareConfig();
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		Scenario scenario = ScenarioUtils.loadScenario(config);

		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		//remove all attributes --> remove the NumberOfJspritIterations attribute to trigger exception
		Carriers carriers = FreightUtils.getCarriers(scenario);
		for (Carrier carrier : carriers.getCarriers().values()) {
			carrier.getAttributes().clear();
		}

		FreightUtils.runJsprit(scenario);
	}

	/**
	 * Don't crash even if there is no algortihm file specified.
	 */
	@Test
	public void testRunJsprit_NoAlgortihmFileGiven(){
		Config config = prepareConfig();
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		try {
			FreightUtils.runJsprit(scenario);
		} catch (Exception e) {
			Assert.fail();
		}
		Assert.assertNull(ConfigUtils.addOrGetModule(scenario.getConfig(), FreightConfigGroup.class).getVehicleRoutingAlgorithmFile());
	}

	private Config prepareConfig(){
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(scenarioUrl, "config.xml" ) );
		config.controler().setLastIteration(0);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		//freight configstuff
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfigGroup.setCarriersFile(IOUtils.extendUrl(scenarioUrl, "singleCarrierFiveActivitiesWithoutRoutes.xml" ).toString() );
		freightConfigGroup.setCarriersVehicleTypesFile(IOUtils.extendUrl(scenarioUrl, "vehicleTypes.xml" ).toString() );
		freightConfigGroup.setTravelTimeSliceWidth(24*3600);
		freightConfigGroup.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.enforceBeginnings);
		return config;
	}


}
