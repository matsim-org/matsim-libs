/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.jsprit;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.management.InvalidAttributeValueException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.FreightConfigGroup.UseDistanceConstraintForTourPlanning;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 *
 *  @author rewert
 *
 * 	Test for the distance constraint. 4 different setups are used to control the
 * 	correct working of the constraint
 *
 */
public class DistanceConstraintTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	static final Logger log = Logger.getLogger(DistanceConstraintTest.class);

	final static URL SCENARIO_URL = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");

	/**
	 * Option 1: Tour is possible with the vehicle with the small battery and the
	 * vehicle with the small battery is cheaper
	 *
	 * @throws InvalidAttributeValueException
	 */
	@Test
	public final void CarrierSmallBatteryTest_Version1() throws InvalidAttributeValueException {

		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV1 = CarrierUtils.createCarrier(Id.create("Carrier_Version1", Carrier.class));
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
		CarrierUtils.setJspritIterations(carrierV1, 25);

		FreightUtils.runJsprit(scenario, ConfigUtils.addOrGetModule(config, FreightConfigGroup.class));

		Assert.assertEquals("Not the correct amout of scheduled tours", 1,
				carrierV1.getSelectedPlan().getScheduledTours().size());

		Assert.assertEquals(vehicleType_SmallV1.getId(), ((Vehicle) carrierV1.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle()).getType().getId());
		double maxDistance_vehicleType_LargeV1 = VehicleUtils.getEnergyCapacity(vehicleType_LargeV1.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV1.getEngineInformation());
		double maxDistance_vehicleType_SmallV1 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV1.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV1.getEngineInformation());

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30000, maxDistance_vehicleType_LargeV1,
				MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30000, maxDistance_vehicleType_SmallV1,
				MatsimTestUtils.EPSILON);
		
		double distanceTour = 0.0;
		List<Tour.TourElement> elements = carrierV1.getSelectedPlan().getScheduledTours().iterator().next().getTour()
				.getTourElements();
		for (Tour.TourElement element : elements) {
			if (element instanceof Tour.Leg) {
				Tour.Leg legElement = (Tour.Leg) element;
				if (legElement.getRoute().getDistance() != 0)
					distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0,
							scenario.getNetwork());
			}
		}
		Assert.assertEquals("The schedulded tour has a non expected distance", 24000, distanceTour,
				MatsimTestUtils.EPSILON);
	}

	/**
	 * Option 2: Tour is not possible with the vehicle with the small battery. Thats
	 * why one vehicle with a large battery is used.
	 *
	 * @throws InvalidAttributeValueException
	 */
	@Test
	public final void CarrierLargeBatteryTest_Version2() throws InvalidAttributeValueException {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV2 = CarrierUtils.createCarrier(Id.create("Carrier_Version2", Carrier.class));

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
		CarrierUtils.setJspritIterations(carrierV2, 10);

		FreightUtils.runJsprit(scenario, ConfigUtils.addOrGetModule(config, FreightConfigGroup.class));
		
		
		Assert.assertEquals("Not the correct amout of scheduled tours", 1,
				carrierV2.getSelectedPlan().getScheduledTours().size());

		Assert.assertEquals(vehicleType_LargeV2.getId(), carrierV2.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle().getType().getId());
		double maxDistance_vehicleType_LargeV2 = VehicleUtils.getEnergyCapacity(vehicleType_LargeV2.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV2.getEngineInformation());
		double maxDistance_vehicleType_SmallV2 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV2.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV2.getEngineInformation());

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30000, maxDistance_vehicleType_LargeV2,
				MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 15000, maxDistance_vehicleType_SmallV2,
				MatsimTestUtils.EPSILON);

		double distanceTour = 0.0;
		List<Tour.TourElement> elements = carrierV2.getSelectedPlan().getScheduledTours().iterator().next().getTour()
				.getTourElements();
		for (Tour.TourElement element : elements) {
			if (element instanceof Tour.Leg) {
				Tour.Leg legElement = (Tour.Leg) element;
				if (legElement.getRoute().getDistance() != 0)
					distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0,
							scenario.getNetwork());
			}
		}
		Assert.assertEquals("The schedulded tour has a non expected distance", 24000, distanceTour,
				MatsimTestUtils.EPSILON);

	}

	/**
	 * Option 3: costs for using one long range vehicle are higher than the costs of
	 * using two short range truck
	 *
	 * @throws InvalidAttributeValueException
	 */

	@Test
	public final void Carrier2SmallBatteryTest_Version3() throws InvalidAttributeValueException {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV3 = CarrierUtils.createCarrier(Id.create("Carrier_Version3", Carrier.class));

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
		CarrierUtils.setJspritIterations(carrierV3, 10);

		FreightUtils.runJsprit(scenario, ConfigUtils.addOrGetModule(config, FreightConfigGroup.class));

		Assert.assertEquals("Not the correct amout of scheduled tours", 2,
				carrierV3.getSelectedPlan().getScheduledTours().size());

		double maxDistance_vehicleType_LargeV3 =  VehicleUtils.getEnergyCapacity(vehicleType_LargeV3.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV3.getEngineInformation());
		double maxDistance_vehicleType_SmallV3 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV3.getEngineInformation())
				/  VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV3.getEngineInformation());

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30000, maxDistance_vehicleType_LargeV3,
				MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30000, maxDistance_vehicleType_SmallV3,
				MatsimTestUtils.EPSILON);


		for (ScheduledTour scheduledTour : carrierV3.getSelectedPlan().getScheduledTours()) {

			double distanceTour = 0.0;
			List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
			for (Tour.TourElement element : elements) {
				if (element instanceof Tour.Leg) {
					Tour.Leg legElement = (Tour.Leg) element;
					if (legElement.getRoute().getDistance() != 0)
						distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0,
								0, scenario.getNetwork());
				}
			}
			Assert.assertEquals(vehicleType_SmallV3.getId(), scheduledTour.getVehicle().getType().getId());
			if (distanceTour == 12000)
				Assert.assertEquals("The schedulded tour has a non expected distance", 12000, distanceTour,
						MatsimTestUtils.EPSILON);
			else
				Assert.assertEquals("The schedulded tour has a non expected distance", 20000, distanceTour,
						MatsimTestUtils.EPSILON);
		}
	}

	/**
	 * Option 4: An additional shipment outside the range of both BEVtypes.
	 * Therefore one diesel vehicle must be used and one vehicle with a small
	 * battery.
	 *
	 * @throws InvalidAttributeValueException
	 */

	@Test
	public final void CarrierWithAddiotionalDieselVehicleTest_Version4() throws InvalidAttributeValueException {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV4 = CarrierUtils.createCarrier(Id.create("Carrier_Version4", Carrier.class));

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
		VehicleUtils.setFuelConsumption(vehicleType_Diesel, 0.0001625);
		vehicleType_Diesel.setDescription("Carrier_Version4");
		vehicleType_Diesel.getCapacity().setOther(40.);

		vehicleTypes.getVehicleTypes().put(vehicleType_LargeV4.getId(), vehicleType_LargeV4);
		vehicleTypes.getVehicleTypes().put(vehicleType_SmallV4.getId(), vehicleType_SmallV4);
		vehicleTypes.getVehicleTypes().put(vehicleType_Diesel.getId(), vehicleType_Diesel);

		carriers.addCarrier(addThreeServicesToCarrier(carrierV4));
		createCarriers(carriers, fleetSize, carrierV4, vehicleTypes);

		scenario.addScenarioElement("carrierVehicleTypes", vehicleTypes);
		scenario.addScenarioElement("carriers", carriers);
		CarrierUtils.setJspritIterations(carrierV4, 10);

		FreightUtils.runJsprit(scenario, ConfigUtils.addOrGetModule(config, FreightConfigGroup.class));

		Assert.assertEquals("Not the correct amout of scheduled tours", 2,
				carrierV4.getSelectedPlan().getScheduledTours().size());

		double maxDistance_vehicleType_Large4 = VehicleUtils.getEnergyCapacity(vehicleType_LargeV4.getEngineInformation())
				/ VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_LargeV4.getEngineInformation());
		double maxDistance_vehicleType_SmallV4 = VehicleUtils.getEnergyCapacity(vehicleType_SmallV4.getEngineInformation())
				/ VehicleUtils.getEnergyConsumptionKWhPerMeter(vehicleType_SmallV4.getEngineInformation());

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30000, maxDistance_vehicleType_Large4,
				MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30000, maxDistance_vehicleType_SmallV4,
				MatsimTestUtils.EPSILON);
		
		for (ScheduledTour scheduledTour : carrierV4.getSelectedPlan().getScheduledTours()) {

			String thisTypeId = scheduledTour.getVehicle().getType().getId().toString();
			double distanceTour = 0.0;
			List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
			for (Tour.TourElement element : elements) {
				if (element instanceof Tour.Leg) {
					Tour.Leg legElement = (Tour.Leg) element;
					if (legElement.getRoute().getDistance() != 0)
						distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0,
								0, scenario.getNetwork());
				}
			}
			if (thisTypeId.equals("SmallBattery_V4"))
				Assert.assertEquals("The schedulded tour has a non expected distance", 24000, distanceTour,
						MatsimTestUtils.EPSILON);
			else if (thisTypeId.equals("DieselVehicle"))
				Assert.assertEquals("The schedulded tour has a non expected distance", 36000, distanceTour,
						MatsimTestUtils.EPSILON);
			else
				Assert.fail("Wrong vehicleType used");
		}
	}

	/**
	 * Deletes the existing output file and sets the number of the last MATSim iteration to 0.
	 *
	 * @param config the config
	 */
	static void prepareConfig(Config config) {
		config.network().setInputFile(IOUtils.extendUrl(SCENARIO_URL, "grid9x9.xml").toString());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(0);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfigGroup.setUseDistanceConstraintForTourPlanning(UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
	}

	private static Carrier addTwoServicesToCarrier(Carrier carrier) {
		// Service 1
		CarrierService service1 = CarrierService.Builder
				.newInstance(Id.create("Service1", CarrierService.class), Id.createLinkId("j(3,8)"))
				.setServiceDuration(20).setServiceStartTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarrierUtils.addService(carrier, service1);

		// Service 2
		CarrierService service2 = CarrierService.Builder
				.newInstance(Id.create("Service2", CarrierService.class), Id.createLinkId("j(0,3)R"))
				.setServiceDuration(20).setServiceStartTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarrierUtils.addService(carrier, service2);

		return carrier;
	}

	private static Carrier addThreeServicesToCarrier(Carrier carrier) {
		// Services 1 and 2
		addTwoServicesToCarrier(carrier);

		// Service 3
		CarrierService service3 = CarrierService.Builder
				.newInstance(Id.create("Service3", CarrierService.class), Id.createLinkId("j(9,2)"))
				.setServiceDuration(20).setServiceStartTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarrierUtils.addService(carrier, service3);

		return carrier;
	}


	/**
	 * Creates the vehicle at the depot, ads this vehicle to the carriers and sets
	 * the capabilities. Sets TimeWindow for the carriers.
	 *
	 * @param
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
	 * @param
	 *
	 * @return new carrierVehicle at the depot
	 */
	static CarrierVehicle createGarbageTruck(String vehicleName, double earliestStartingTime,
											 double latestFinishingTime, VehicleType singleVehicleType) {

		return CarrierVehicle.Builder.newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId("i(1,8)"))
				.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime)
				.setTypeId(singleVehicleType.getId()).setType(singleVehicleType).build();
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers
	 *
	 * @param
	 *
	 */
	private static void defineCarriers(Carriers carriers, FleetSize fleetSize, Carrier singleCarrier,
									   List<CarrierVehicle> vehicles, CarrierVehicleTypes vehicleTypes) {

		singleCarrier.setCarrierCapabilities(CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build());
		for (CarrierVehicle carrierVehicle : vehicles) {
			CarrierUtils.addCarrierVehicle(singleCarrier, carrierVehicle);
		}
		singleCarrier.getCarrierCapabilities().getVehicleTypes().addAll(vehicleTypes.getVehicleTypes().values());

		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
	}
}
