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
 * 	correct working of the constraint for services
 * <p>
 * 	2 additional setups are defined when using shipments instead of service.
 * 	Shipments allow reloading of good during the tour.
 *
 */
public class DistanceConstraintTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	static final Logger log = LogManager.getLogger(DistanceConstraintTest.class);

	final static URL SCENARIO_URL = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");

	/**
	 * Option 1: Tour is possible with the vehicle with the small battery and the
	 * vehicle with the small battery is cheaper
	 *
	 * @throws ExecutionException, InterruptedException
	 */
	@Test
	final void CarrierSmallBatteryTest_Version1() throws ExecutionException, InterruptedException {

		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV1 = CarriersUtils.createCarrier(Id.create("Carrier_Version1", Carrier.class));
		VehicleType vehicleType_LargeV1 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V1", VehicleType.class));
		vehicleType_LargeV1.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		VehicleUtils.setHbefaTechnology(vehicleType_LargeV1.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_LargeV1.getEngineInformation(), 450.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_LargeV1.getEngineInformation(), 0.015);
		vehicleType_LargeV1.getCapacity().setOther(80.);
		vehicleType_LargeV1.setDescription("Carrier_Version1");

		VehicleType vehicleType_SmallV1 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V1", VehicleType.class));
		vehicleType_SmallV1.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		VehicleUtils.setHbefaTechnology(vehicleType_SmallV1.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_SmallV1.getEngineInformation(), 300.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_SmallV1.getEngineInformation(), 0.01);
		vehicleType_SmallV1.setDescription("Carrier_Version1");
		vehicleType_SmallV1.getCapacity().setOther(80.);

		vehicleTypes.getVehicleTypes().put(vehicleType_LargeV1.getId(), vehicleType_LargeV1);
		vehicleTypes.getVehicleTypes().put(vehicleType_SmallV1.getId(), vehicleType_SmallV1);

		carriers.addCarrier(addTwoServicesToCarrier(carrierV1));
		createCarriers(carriers, fleetSize, carrierV1, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV1, 25);

		CarriersUtils.runJsprit(scenario);

		Assertions.assertEquals(1,
				carrierV1.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

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

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV2 = CarriersUtils.createCarrier(Id.create("Carrier_Version2", Carrier.class));

		VehicleType vehicleType_LargeV2 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V2", VehicleType.class));
		vehicleType_LargeV2.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		VehicleUtils.setHbefaTechnology(vehicleType_LargeV2.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_LargeV2.getEngineInformation(), 450.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_LargeV2.getEngineInformation(), 0.015);
		vehicleType_LargeV2.setDescription("Carrier_Version2");
		vehicleType_LargeV2.getCapacity().setOther(80.);

		VehicleType vehicleType_SmallV2 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V2", VehicleType.class));
		vehicleType_SmallV2.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		VehicleUtils.setHbefaTechnology(vehicleType_SmallV2.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_SmallV2.getEngineInformation(), 150.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_SmallV2.getEngineInformation(), 0.01);
		vehicleType_SmallV2.setDescription("Carrier_Version2");
		vehicleType_SmallV2.getCapacity().setOther(80.);

		vehicleTypes.getVehicleTypes().put(vehicleType_LargeV2.getId(), vehicleType_LargeV2);
		vehicleTypes.getVehicleTypes().put(vehicleType_SmallV2.getId(), vehicleType_SmallV2);

		carriers.addCarrier(addTwoServicesToCarrier(carrierV2));
		createCarriers(carriers, fleetSize, carrierV2, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV2, 10);

		CarriersUtils.runJsprit(scenario);


		Assertions.assertEquals(1,
				carrierV2.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

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

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV3 = CarriersUtils.createCarrier(Id.create("Carrier_Version3", Carrier.class));

		VehicleType vehicleType_LargeV3 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V3", VehicleType.class));
		vehicleType_LargeV3.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		VehicleUtils.setHbefaTechnology(vehicleType_LargeV3.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_LargeV3.getEngineInformation(), 450.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_LargeV3.getEngineInformation(), 0.015);
		vehicleType_LargeV3.setDescription("Carrier_Version3");
		vehicleType_LargeV3.getCapacity().setOther(80.);

		VehicleType vehicleType_SmallV3 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V3", VehicleType.class));
		vehicleType_SmallV3.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(40.);
		VehicleUtils.setHbefaTechnology(vehicleType_SmallV3.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_SmallV3.getEngineInformation(), 300.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_SmallV3.getEngineInformation(), 0.010);
		vehicleType_SmallV3.setDescription("Carrier_Version3");
		vehicleType_SmallV3.getCapacity().setOther(40.);

		vehicleType_SmallV3.setDescription("Carrier_Version3");
		vehicleType_SmallV3.getCapacity().setOther(40.);

		vehicleTypes.getVehicleTypes().put(vehicleType_LargeV3.getId(), vehicleType_LargeV3);
		vehicleTypes.getVehicleTypes().put(vehicleType_SmallV3.getId(), vehicleType_SmallV3);

		carriers.addCarrier(addTwoServicesToCarrier(carrierV3));
		createCarriers(carriers, fleetSize, carrierV3, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV3, 10);

		CarriersUtils.runJsprit(scenario);

		Assertions.assertEquals(2,
				carrierV3.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

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
	 * Option 4: An additional service outside the range of both BEV types.
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

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV4 = CarriersUtils.createCarrier(Id.create("Carrier_Version4", Carrier.class));

		VehicleType vehicleType_LargeV4 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V4", VehicleType.class));
		vehicleType_LargeV4.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		VehicleUtils.setHbefaTechnology(vehicleType_LargeV4.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_LargeV4.getEngineInformation(), 450.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_LargeV4.getEngineInformation(), 0.015);
		vehicleType_LargeV4.setDescription("Carrier_Version4");
		vehicleType_LargeV4.getCapacity().setOther(120.);

		VehicleType vehicleType_SmallV4 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V4", VehicleType.class));
		vehicleType_SmallV4.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		VehicleUtils.setHbefaTechnology(vehicleType_SmallV4.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_SmallV4.getEngineInformation(), 300.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_SmallV4.getEngineInformation(), 0.01);
		vehicleType_SmallV4.setDescription("Carrier_Version4");
		vehicleType_SmallV4.getCapacity().setOther(120.);

		VehicleType vehicleType_Diesel = VehicleUtils.createVehicleType(Id.create("DieselVehicle", VehicleType.class));
		vehicleType_Diesel.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(400.);
		VehicleUtils.setHbefaTechnology(vehicleType_Diesel.getEngineInformation(), "diesel");
		VehicleUtils.setFuelConsumptionLitersPerMeter(vehicleType_Diesel.getEngineInformation(), 0.0001625);
		vehicleType_Diesel.setDescription("Carrier_Version4");
		vehicleType_Diesel.getCapacity().setOther(40.);

		vehicleTypes.getVehicleTypes().put(vehicleType_LargeV4.getId(), vehicleType_LargeV4);
		vehicleTypes.getVehicleTypes().put(vehicleType_SmallV4.getId(), vehicleType_SmallV4);
		vehicleTypes.getVehicleTypes().put(vehicleType_Diesel.getId(), vehicleType_Diesel);

		carriers.addCarrier(addThreeServicesToCarrier(carrierV4));
		createCarriers(carriers, fleetSize, carrierV4, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV4, 10);

		CarriersUtils.runJsprit(scenario);

		Assertions.assertEquals(2,
				carrierV4.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

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
	 * Option 5:
	 * This test uses shipments instead of service .
	 * As a consequence the vehicles can return to the depot, load more goods and run another subtour.
	 * Distance is set to a value that, due to distance restrictions, two tours are necessary.
	 * <p>
	 * This option (5) is designed similar to option 2
	 *
	 */

	@Test
	final void CarrierWithShipmentsMidSizeBatteryTest_Version5() throws ExecutionException, InterruptedException {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV5 = CarriersUtils.createCarrier(Id.create("Carrier_Version5", Carrier.class));

		VehicleType vehicleType_MidSizeV5 = VehicleUtils.createVehicleType(Id.create("MidSizeBattery_V5", VehicleType.class));
		vehicleType_MidSizeV5.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		VehicleUtils.setHbefaTechnology(vehicleType_MidSizeV5.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_MidSizeV5.getEngineInformation(), 300.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_MidSizeV5.getEngineInformation(), 0.015);
		vehicleType_MidSizeV5.setDescription("Carrier_Version5");
		vehicleType_MidSizeV5.getCapacity().setOther(80.);

		vehicleTypes.getVehicleTypes().put(vehicleType_MidSizeV5.getId(), vehicleType_MidSizeV5);

		carriers.addCarrier(addTwoShipmentsToCarrier(carrierV5));
		createCarriers(carriers, fleetSize, carrierV5, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV5, 10);

		CarriersUtils.runJsprit(scenario);

		//We need two tours, due to reloading both shipments must be transported one after the other
		Assertions.assertEquals(2,
				carrierV5.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

		Assertions.assertEquals(vehicleType_MidSizeV5.getId(), carrierV5.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle().getType().getId());
		double maxDistance_vehicleType_LargeV5 = VehicleUtils.getEnergyCapacity(vehicleType_MidSizeV5.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_MidSizeV5.getEngineInformation());

		Assertions.assertEquals(20000, maxDistance_vehicleType_LargeV5,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");

		ArrayList<Double> distancesOfTours = new ArrayList<>();
		for (ScheduledTour scheduledTour: carrierV5.getSelectedPlan().getScheduledTours()) {
			double distanceTour = 0.0;
			List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
			for (Tour.TourElement element : elements) {
				if (element instanceof Tour.Leg legElement) {
					if (legElement.getRoute().getDistance() != 0)
						distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0,
								scenario.getNetwork());
				}
			}
			distancesOfTours.add(distanceTour);
		}

		Assertions.assertEquals(2, distancesOfTours.size(), "There must be two entry for tour distances");
		//One tour has distance of 12000m
		Assertions.assertTrue(distancesOfTours.contains(12000.0), "The scheduled tour has a non expected distance");
		//The other tour has distance of 20000m
		Assertions.assertTrue(distancesOfTours.contains(20000.0), "The scheduled tour has a non expected distance");
	}

	/**
	 * Option 6:
	 * This test uses shipments instead of service .
	 * As a consequence the vehicles can return to the depot, load more goods and run another subtour.
	 * Distance is set to a value that one tour can be run with loading once.
	 * <p>
	 * This option (6) is designed similar to option 5
	 *
	 */

	@Test
	final void CarrierWithShipmentsLargeBatteryTest_Version6() throws ExecutionException, InterruptedException {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV5 = CarriersUtils.createCarrier(Id.create("Carrier_Version5", Carrier.class));

		VehicleType vehicleType_LargeV5 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V5", VehicleType.class));
		vehicleType_LargeV5.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		VehicleUtils.setHbefaTechnology(vehicleType_LargeV5.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(vehicleType_LargeV5.getEngineInformation(), 450.);
		VehicleUtils.setEnergyConsumptionKWhPerMeter(vehicleType_LargeV5.getEngineInformation(), 0.015);
		vehicleType_LargeV5.setDescription("Carrier_Version5");
		vehicleType_LargeV5.getCapacity().setOther(80.);

		vehicleTypes.getVehicleTypes().put(vehicleType_LargeV5.getId(), vehicleType_LargeV5);

		carriers.addCarrier(addTwoShipmentsToCarrier(carrierV5));
		createCarriers(carriers, fleetSize, carrierV5, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarriersUtils.setJspritIterations(carrierV5, 10);

		CarriersUtils.runJsprit(scenario);


		//We need two tours, due to reloading both shipments must be transported one after the other
		Assertions.assertEquals(1,
				carrierV5.getSelectedPlan().getScheduledTours().size(),
				"Not the correct amount of scheduled tours");

		Assertions.assertEquals(vehicleType_LargeV5.getId(), carrierV5.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle().getType().getId());
		double maxDistance_vehicleType_LargeV5 = VehicleUtils.getEnergyCapacity(vehicleType_LargeV5.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV5.getEngineInformation());

		Assertions.assertEquals(30000, maxDistance_vehicleType_LargeV5,
				MatsimTestUtils.EPSILON,
				"Wrong maximum distance of the tour of this vehicleType");

		ArrayList<Double> distancesOfTours = new ArrayList<>();
		for (ScheduledTour scheduledTour: carrierV5.getSelectedPlan().getScheduledTours()) {
			double distanceTour = 0.0;
			List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
			for (Tour.TourElement element : elements) {
				if (element instanceof Tour.Leg legElement) {
					if (legElement.getRoute().getDistance() != 0)
						distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0,
								scenario.getNetwork());
				}
			}
			distancesOfTours.add(distanceTour);
		}

		Assertions.assertEquals(1, distancesOfTours.size(), "There must be one entry for tour distances");
		//This tour has distance of 24000m
		Assertions.assertTrue(distancesOfTours.contains(24000.0), "The scheduled tour has a non expected distance");
	}

	/**
	 * Deletes the existing output file and sets the number of the last MATSim iteration to 0.
	 *
	 * @param config the config
	 */
	static void prepareConfig(Config config) {
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

	private static Carrier addTwoShipmentsToCarrier(Carrier carrier) {
		// Shipment 1
		CarrierShipment shipment1 = CarrierShipment.Builder
				.newInstance(Id.create("Shipment1", CarrierShipment.class), Id.createLinkId("i(1,8)"), Id.createLinkId("j(3,8)"), 40)
				.setDeliveryDuration(20).setDeliveryStartingTimeWindow(TimeWindow.newInstance(8 * 3600, 12 * 3600))
				.build();
		CarriersUtils.addShipment(carrier, shipment1);

		// Shipment 2
		CarrierShipment shipment2 = CarrierShipment.Builder
				.newInstance(Id.create("Shipment2", CarrierShipment.class),Id.createLinkId("i(1,8)"), Id.createLinkId("j(0,3)R"), 40)
				.setDeliveryDuration(20).setDeliveryStartingTimeWindow(TimeWindow.newInstance(8 * 3600, 12 * 3600))
				.build();
		CarriersUtils.addShipment(carrier, shipment2);

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
				vehicles.add(createCarrierVehicle(singleVehicleType.getId().toString(), earliestStartingTime,
						latestFinishingTime, singleVehicleType));
		}

		// define Carriers
		defineCarriers(carriers, fleetSize, singleCarrier, vehicles, vehicleTypes);
	}

	/**
	 * Method for creating a new carrierVehicle
	 * @return new carrierVehicle at the depot
	 */
	static CarrierVehicle createCarrierVehicle(String vehicleName, double earliestStartingTime,
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
