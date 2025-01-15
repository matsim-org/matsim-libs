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

package org.matsim.freight.carriers.jsprit;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 *
 *  @author rewert, kturner
 * <p>
 * 	Test for the distance constraint. 4 different setups are used to control the
 * 	correct working of the constraint
 *
 * 	In this test, the vehicleTypes are read in from .xml-File.
 *
 */
public class DistanceConstraintFromVehiclesFileTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	static final Logger log = LogManager.getLogger(DistanceConstraintFromVehiclesFileTest.class);

	final static URL SCENARIO_URL = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");

	/**
	 * Option 1: Tour is possible with the vehicle with the small battery and the
	 * vehicle with the small battery is cheaper
	 *
	 */
	@Test
	final void CarrierSmallBatteryTest_Version1() throws ExecutionException, InterruptedException {

		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes allVehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		new CarrierVehicleTypeReader(allVehicleTypes).readFile(ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class).getCarriersVehicleTypesFile());

		//Filter for V1 vehicleTypes.
		for (VehicleType vehicleType : allVehicleTypes.getVehicleTypes().values()) {
			if (vehicleType.getId().toString().equals("SmallBattery_V1") || vehicleType.getId().toString().equals("LargeBattery_V1")) {
				vehicleTypes.getVehicleTypes().put(vehicleType.getId(), vehicleType);
			}
		}

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV1 = CarriersUtils.createCarrier(Id.create("Carrier_Version1", Carrier.class));

		carriers.addCarrier(addTwoServicesToCarrier(carrierV1));
		createCarriers(carriers, fleetSize, carrierV1, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV1, 25);

		CarriersUtils.runJsprit(scenario);

		Assertions.assertEquals(1,
				carrierV1.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

		VehicleType vehicleType_SmallV1 = vehicleTypes.getVehicleTypes().get(Id.create("SmallBattery_V1", VehicleType.class));
		VehicleType vehicleType_LargeV1 = vehicleTypes.getVehicleTypes().get(Id.create("LargeBattery_V1", VehicleType.class));

		Assertions.assertEquals(vehicleType_SmallV1.getId(), carrierV1.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle().getType().getId());
		double maxDistance_vehicleType_LargeV1 = VehicleUtils.getEnergyCapacity(vehicleType_LargeV1.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV1.getEngineInformation());
		double maxDistance_vehicleType_SmallV1 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV1.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV1.getEngineInformation());

		Assertions.assertEquals(30000, maxDistance_vehicleType_LargeV1,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");
		Assertions.assertEquals(30000, maxDistance_vehicleType_SmallV1,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");

		double distanceTour = 0.0;
		List<Tour.TourElement> elements = carrierV1.getSelectedPlan().getScheduledTours().iterator().next().getTour()
				.getTourElements();
		for (Tour.TourElement element : elements) {
			if (element instanceof Tour.Leg legElement) {
				if (legElement.getRoute().getDistance() != 0)
					distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0,
							scenario.getNetwork());
			}
		}
		Assertions.assertEquals(24000, distanceTour,
				MatsimTestUtils.EPSILON,
				"The scheduled tour has a non expected distance");
	}

	/**
	 * Option 2: Tour is not possible with the vehicle with the small battery.
	 * That's why one vehicle with a large battery is used.
	 *
	 */
	@Test
	final void CarrierLargeBatteryTest_Version2() throws ExecutionException, InterruptedException {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes allVehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		new CarrierVehicleTypeReader(allVehicleTypes).readFile(ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class).getCarriersVehicleTypesFile());

		//Filter for V1 vehicleTypes.
		for (VehicleType vehicleType : allVehicleTypes.getVehicleTypes().values()) {
			if (vehicleType.getId().toString().equals("SmallBattery_V2") || vehicleType.getId().toString().equals("LargeBattery_V2")) {
				vehicleTypes.getVehicleTypes().put(vehicleType.getId(), vehicleType);
			}
		}

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV2 = CarriersUtils.createCarrier(Id.create("Carrier_Version2", Carrier.class));

		carriers.addCarrier(addTwoServicesToCarrier(carrierV2));
		createCarriers(carriers, fleetSize, carrierV2, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV2, 10);

		CarriersUtils.runJsprit(scenario);


		Assertions.assertEquals(1,
				carrierV2.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

		VehicleType vehicleType_SmallV2 = vehicleTypes.getVehicleTypes().get(Id.create("SmallBattery_V2", VehicleType.class));
		VehicleType vehicleType_LargeV2 = vehicleTypes.getVehicleTypes().get(Id.create("LargeBattery_V2", VehicleType.class));

		Assertions.assertEquals(vehicleType_LargeV2.getId(), carrierV2.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle().getType().getId());
		double maxDistance_vehicleType_LargeV2 = VehicleUtils.getEnergyCapacity(vehicleType_LargeV2.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV2.getEngineInformation());
		double maxDistance_vehicleType_SmallV2 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV2.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV2.getEngineInformation());

		Assertions.assertEquals(30000, maxDistance_vehicleType_LargeV2,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");
		Assertions.assertEquals(15000, maxDistance_vehicleType_SmallV2,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");

		double distanceTour = 0.0;
		List<Tour.TourElement> elements = carrierV2.getSelectedPlan().getScheduledTours().iterator().next().getTour()
				.getTourElements();
		for (Tour.TourElement element : elements) {
			if (element instanceof Tour.Leg legElement) {
				if (legElement.getRoute().getDistance() != 0)
					distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0,
							scenario.getNetwork());
			}
		}
		Assertions.assertEquals(24000, distanceTour,
				MatsimTestUtils.EPSILON,
				"The scheduled tour has a non expected distance");

	}

	/**
	 * Option 3: costs for using one long range vehicle are higher than the costs of
	 * using two short range truck
	 *
	 */

	@Test
	final void Carrier2SmallBatteryTest_Version3() throws ExecutionException, InterruptedException {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes allVehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		new CarrierVehicleTypeReader(allVehicleTypes).readFile(ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class).getCarriersVehicleTypesFile());

		//Filter for V3 vehicleTypes.
		for (VehicleType vehicleType : allVehicleTypes.getVehicleTypes().values()) {
			if (vehicleType.getId().toString().equals("SmallBattery_V3") || vehicleType.getId().toString().equals("LargeBattery_V3")) {
				vehicleTypes.getVehicleTypes().put(vehicleType.getId(), vehicleType);
			}
		}

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV3 = CarriersUtils.createCarrier(Id.create("Carrier_Version3", Carrier.class));

		carriers.addCarrier(addTwoServicesToCarrier(carrierV3));
		createCarriers(carriers, fleetSize, carrierV3, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV3, 10);

		CarriersUtils.runJsprit(scenario);

		Assertions.assertEquals(2,
				carrierV3.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

		VehicleType vehicleType_SmallV3 = vehicleTypes.getVehicleTypes().get(Id.create("SmallBattery_V3", VehicleType.class));
		VehicleType vehicleType_LargeV3 = vehicleTypes.getVehicleTypes().get(Id.create("LargeBattery_V3", VehicleType.class));

		double maxDistance_vehicleType_LargeV3 =  VehicleUtils.getEnergyCapacity(vehicleType_LargeV3.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV3.getEngineInformation());
		double maxDistance_vehicleType_SmallV3 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV3.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV3.getEngineInformation());

		Assertions.assertEquals(30000, maxDistance_vehicleType_LargeV3,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");

		Assertions.assertEquals(30000, maxDistance_vehicleType_SmallV3,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");


		for (ScheduledTour scheduledTour : carrierV3.getSelectedPlan().getScheduledTours()) {

			double distanceTour = 0.0;
			List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
			for (Tour.TourElement element : elements) {
				if (element instanceof Tour.Leg legElement) {
					if (legElement.getRoute().getDistance() != 0)
						distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0,
								0, scenario.getNetwork());
				}
			}
			Assertions.assertEquals(vehicleType_SmallV3.getId(), scheduledTour.getVehicle().getType().getId());
			if (distanceTour == 12000)
				Assertions.assertEquals(12000, distanceTour,
						MatsimTestUtils.EPSILON,
						"The scheduled tour has a non expected distance");
			else
				Assertions.assertEquals(20000, distanceTour,
						MatsimTestUtils.EPSILON,
						"The scheduled tour has a non expected distance");
		}
	}

	/**
	 * Option 4: An additional shipment outside the range of both BEV types.
	 * Therefore, one diesel vehicle must be used and one vehicle with a small
	 * battery.
	 *
	 */

	@Test
	final void CarrierWithAdditionalDieselVehicleTest_Version4() throws ExecutionException, InterruptedException {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes allVehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		new CarrierVehicleTypeReader(allVehicleTypes).readFile(ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class).getCarriersVehicleTypesFile());

		//Filter for V4 vehicleTypes including diesel.
		for (VehicleType vehicleType : allVehicleTypes.getVehicleTypes().values()) {
			if (vehicleType.getId().toString().equals("SmallBattery_V4") || vehicleType.getId().toString().equals("LargeBattery_V4")
					|| vehicleType.getId().toString().equals("DieselVehicle") ) {
				vehicleTypes.getVehicleTypes().put(vehicleType.getId(), vehicleType);
			}
		}

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV4 = CarriersUtils.createCarrier(Id.create("Carrier_Version4", Carrier.class));

		carriers.addCarrier(addThreeServicesToCarrier(carrierV4));
		createCarriers(carriers, fleetSize, carrierV4, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV4, 10);

		CarriersUtils.runJsprit(scenario);

		Assertions.assertEquals(2,
				carrierV4.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

		VehicleType vehicleType_SmallV4 = vehicleTypes.getVehicleTypes().get(Id.create("SmallBattery_V4", VehicleType.class));
		VehicleType vehicleType_LargeV4 = vehicleTypes.getVehicleTypes().get(Id.create("LargeBattery_V4", VehicleType.class));

		double maxDistance_vehicleType_Large4 = VehicleUtils.getEnergyCapacity(vehicleType_LargeV4.getEngineInformation())
				/ VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV4.getEngineInformation());
		double maxDistance_vehicleType_SmallV4 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV4.getEngineInformation())
				/ VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV4.getEngineInformation());

		Assertions.assertEquals(30000, maxDistance_vehicleType_Large4,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");

		Assertions.assertEquals(30000, maxDistance_vehicleType_SmallV4,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");

		for (ScheduledTour scheduledTour : carrierV4.getSelectedPlan().getScheduledTours()) {

			String thisTypeId = scheduledTour.getVehicle().getType().getId().toString();
			double distanceTour = 0.0;
			List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
			for (Tour.TourElement element : elements) {
				if (element instanceof Tour.Leg legElement) {
					if (legElement.getRoute().getDistance() != 0)
						distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0,
								0, scenario.getNetwork());
				}
			}
			if (thisTypeId.equals("SmallBattery_V4"))
				Assertions.assertEquals(24000, distanceTour,
						MatsimTestUtils.EPSILON,
						"The scheduled tour has a non expected distance");
			else if (thisTypeId.equals("DieselVehicle"))
				Assertions.assertEquals(36000, distanceTour,
						MatsimTestUtils.EPSILON,
						"The scheduled tour has a non expected distance");
			else
				Assertions.fail("Wrong vehicleType used");
		}
	}

	/**
	 * Deletes the existing output file and sets the number of the last MATSim iteration to 0.
	 *
	 * @param config the config
	 */
	private void prepareConfig(Config config) {
		config.network().setInputFile(IOUtils.extendUrl(SCENARIO_URL, "grid9x9.xml").toString());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(),
				config.controller().getOverwriteFileSetting(), CompressionType.gzip);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controller().setLastIteration(0);
		config.global().setRandomSeed(4177);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setUseDistanceConstraintForTourPlanning(UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(testUtils.getPackageInputDirectory()+"/vehicleTypesForDCTest.xml");
	}

	private static Carrier addTwoServicesToCarrier(Carrier carrier) {
		// Service 1
		CarrierService.Builder builder1 = CarrierService.Builder
				.newInstance(Id.create("Service1", CarrierService.class), Id.createLinkId("j(3,8)"))
				.setServiceDuration(20);
		CarrierService service1 = builder1.setServiceStartingTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarriersUtils.addService(carrier, service1);

		// Service 2
		CarrierService.Builder builder = CarrierService.Builder
				.newInstance(Id.create("Service2", CarrierService.class), Id.createLinkId("j(0,3)R"))
				.setServiceDuration(20);
		CarrierService service2 = builder.setServiceStartingTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarriersUtils.addService(carrier, service2);

		return carrier;
	}

	private static Carrier addThreeServicesToCarrier(Carrier carrier) {
		// Services 1 and 2
		addTwoServicesToCarrier(carrier);

		// Service 3
		CarrierService.Builder builder = CarrierService.Builder
				.newInstance(Id.create("Service3", CarrierService.class), Id.createLinkId("j(9,2)"))
				.setServiceDuration(20);
		CarrierService service3 = builder.setServiceStartingTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarriersUtils.addService(carrier, service3);

		return carrier;
	}


	/**
	 * Creates the vehicle at the depot, ads this vehicle to the carriers and sets
	 * the capabilities. Sets TimeWindow for the carriers.
	 */
	private static void createCarriers(Carriers carriers, FleetSize fleetSize, Carrier singleCarrier,
									   CarrierVehicleTypes vehicleTypes) {
		double earliestStartingTime = 8 * 3600;
		double latestFinishingTime = 10 * 3600;
		List<CarrierVehicle> vehicles = new ArrayList<>();
		for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
			if (singleCarrier.getId().toString().equals(singleVehicleType.getDescription()))
				vehicles.add(createGarbageTruck(singleVehicleType.getId().toString(), earliestStartingTime,
						latestFinishingTime, singleVehicleType));
		}

		// define Carriers
		defineCarriers(carriers, fleetSize, singleCarrier, vehicles, vehicleTypes);
	}

	/**
	 * Method for creating a new carrierVehicle
	 *
	 * @return new carrierVehicle at the depot
	 */
	static CarrierVehicle createGarbageTruck(String vehicleName, double earliestStartingTime,
											 double latestFinishingTime, VehicleType singleVehicleType) {

		return CarrierVehicle.Builder.newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId("i(1,8)"), singleVehicleType )
				.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime).build();
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers
	 */
	private static void defineCarriers(Carriers carriers, FleetSize fleetSize, Carrier singleCarrier,
									   List<CarrierVehicle> vehicles, CarrierVehicleTypes vehicleTypes) {

		singleCarrier.setCarrierCapabilities(CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build());
		for (CarrierVehicle carrierVehicle : vehicles) {
			CarriersUtils.addCarrierVehicle(singleCarrier, carrierVehicle);
		}
		singleCarrier.getCarrierCapabilities().getVehicleTypes().addAll(vehicleTypes.getVehicleTypes().values());

	}
}
