package org.matsim.contrib.freight.jsprit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.FreightConfigGroup.UseDistanceConstraint;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
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
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit.Strategy;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Test for the distance constraint. 4 different setups are used to control the correct working of the constraint
 * 
 * @author rewert
 *
 */
public class TestDistanceConstraint {
	static final Logger log = Logger.getLogger(TestDistanceConstraint.class);

	private static final String original_Chessboard = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";

	/**
	 * Option 1: Tour is possible with the vehicle with the small battery and the
	 * vehicle with the small battery is cheaper
	 */

	@Test
	public final void CarrierSmallBatteryTest_Version1() {

		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/original_Chessboard_Test/Version1");
		config.network().setInputFile(original_Chessboard);
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfigGroup.setUseDistanceConstraint(UseDistanceConstraint.basedOnEnergyConsumption);
		String test = config.getModules().get("freight").getParams().get("useDistanceConstraint");
		config = prepareConfig(config, 0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV1 = CarrierUtils.createCarrier(Id.create("Carrier_Version1", Carrier.class));
		VehicleType newVT1 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V1", VehicleType.class));
		newVT1.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT1.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT1.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT1.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT1.getCapacity().setOther(80.);
		newVT1.setDescription("Carrier_Version1");
		VehicleType newVT2 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V1", VehicleType.class));
		newVT2.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		newVT2.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT2.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 300.);
		newVT2.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 10.);
		newVT2.setDescription("Carrier_Version1");
		newVT2.getCapacity().setOther(80.);

		vehicleTypes.getVehicleTypes().put(newVT1.getId(), newVT1);
		vehicleTypes.getVehicleTypes().put(newVT2.getId(), newVT2);

		boolean threeServices = false;
		createServices(carrierV1, threeServices, carriers);
		createCarriers(carriers, fleetSize, carrierV1, scenario, vehicleTypes);

		int jspritIterations = 100;
		solveJspritAndMATSim(scenario, vehicleTypes, carriers, jspritIterations);

		Assert.assertEquals("Not the correct amout of scheduled tours", 1,
				carrierV1.getSelectedPlan().getScheduledTours().size());

		Assert.assertEquals(newVT2.getId(), carrierV1.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle().getVehicleType().getId());
		double maxDistanceVehicle1 = (double) newVT1.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT1.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");
		double maxDistanceVehilce2 = (double) newVT2.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT2.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30, maxDistanceVehicle1,
				MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30, maxDistanceVehilce2,
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
	 */
	@Test
	public final void CarrierLargeBatteryTest_Version2() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/original_Chessboard/Test/Version2");
		config.network().setInputFile(original_Chessboard);
		config = prepareConfig(config, 0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

		Carrier carrierV2 = CarrierUtils.createCarrier(Id.create("Carrier_Version2", Carrier.class));

		VehicleType newVT3 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V2", VehicleType.class));
		newVT3.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT3.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT3.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT3.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT3.setDescription("Carrier_Version2");
		newVT3.getCapacity().setOther(80.);
		VehicleType newVT4 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V2", VehicleType.class));
		newVT4.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		newVT4.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT4.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 150.);
		newVT4.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 10.);
		newVT4.setDescription("Carrier_Version2");
		newVT4.getCapacity().setOther(80.);

		vehicleTypes.getVehicleTypes().put(newVT3.getId(), newVT3);
		vehicleTypes.getVehicleTypes().put(newVT4.getId(), newVT4);

		boolean threeServices = false;
		createServices(carrierV2, threeServices, carriers);
		createCarriers(carriers, fleetSize, carrierV2, scenario, vehicleTypes);

		int jspritIterations = 100;
		solveJspritAndMATSim(scenario, vehicleTypes, carriers, jspritIterations);

		Assert.assertEquals("Not the correct amout of scheduled tours", 1,
				carrierV2.getSelectedPlan().getScheduledTours().size());

		Assert.assertEquals(newVT3.getId(), carrierV2.getSelectedPlan().getScheduledTours().iterator().next()
				.getVehicle().getVehicleType().getId());
		double maxDistanceVehicle3 = (double) newVT3.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT3.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");
		double maxDistanceVehilce4 = (double) newVT4.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT4.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30, maxDistanceVehicle3,
				MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 15, maxDistanceVehilce4,
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
	 */

	@Test
	public final void Carrier2SmallBatteryTest_Version3() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/original_Chessboard/Test/Version3");
		config.network().setInputFile(original_Chessboard);
		config = prepareConfig(config, 0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV3 = CarrierUtils.createCarrier(Id.create("Carrier_Version3", Carrier.class));

		VehicleType newVT5 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V3", VehicleType.class));
		newVT5.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT5.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT5.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT5.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT5.setDescription("Carrier_Version3");
		newVT5.getCapacity().setOther(80.);
		VehicleType newVT6 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V3", VehicleType.class));
		newVT6.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(40.);
		newVT6.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT6.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 300.);
		newVT6.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 10.);
		newVT6.setDescription("Carrier_Version3");
		newVT6.getCapacity().setOther(40.);

		vehicleTypes.getVehicleTypes().put(newVT5.getId(), newVT5);
		vehicleTypes.getVehicleTypes().put(newVT6.getId(), newVT6);

		boolean threeServices = false;
		createServices(carrierV3, threeServices, carriers);
		createCarriers(carriers, fleetSize, carrierV3, scenario, vehicleTypes);

		int jspritIterations = 100;
		solveJspritAndMATSim(scenario, vehicleTypes, carriers, jspritIterations);

		Assert.assertEquals("Not the correct amout of scheduled tours", 2,
				carrierV3.getSelectedPlan().getScheduledTours().size());

		double maxDistanceVehicle5 = (double) newVT5.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT5.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");
		double maxDistanceVehilce6 = (double) newVT6.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT6.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30, maxDistanceVehicle5,
				MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30, maxDistanceVehilce6,
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
			Assert.assertEquals(newVT6.getId(), scheduledTour.getVehicle().getVehicleType().getId());
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
	 */

	@Test
	public final void CarrierWithAddiotionalDieselVehicleTest_Version4() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/original_Chessboard/Test/Version4");
		config.network().setInputFile(original_Chessboard);
		config = prepareConfig(config, 0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;
		Carrier carrierV4 = CarrierUtils.createCarrier(Id.create("Carrier_Version4", Carrier.class));

		VehicleType newVT7 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V4", VehicleType.class));
		newVT7.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT7.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT7.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT7.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT7.setDescription("Carrier_Version4");
		newVT7.getCapacity().setOther(120.);
		VehicleType newVT8 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V4", VehicleType.class));
		newVT8.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		newVT8.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT8.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 300.);
		newVT8.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 10.);
		newVT8.setDescription("Carrier_Version4");
		newVT8.getCapacity().setOther(120.);
		VehicleType newVT9 = VehicleUtils.createVehicleType(Id.create("DieselVehicle", VehicleType.class));
		newVT9.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(400.);
		newVT9.getEngineInformation().getAttributes().putAttribute("fuelType", "diesel");
		newVT9.getEngineInformation().getAttributes().putAttribute("fuelConsumptionLitersPerMeter", 0.0001625);
		newVT9.setDescription("Carrier_Version4");
		newVT9.getCapacity().setOther(40.);

		vehicleTypes.getVehicleTypes().put(newVT7.getId(), newVT7);
		vehicleTypes.getVehicleTypes().put(newVT8.getId(), newVT8);
		vehicleTypes.getVehicleTypes().put(newVT9.getId(), newVT9);

		boolean threeServices = true;
		createServices(carrierV4, threeServices, carriers);
		createCarriers(carriers, fleetSize, carrierV4, scenario, vehicleTypes);

		int jspritIterations = 100;
		solveJspritAndMATSim(scenario, vehicleTypes, carriers, jspritIterations);

		Assert.assertEquals("Not the correct amout of scheduled tours", 2,
				carrierV4.getSelectedPlan().getScheduledTours().size());

		double maxDistanceVehicle7 = (double) newVT7.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT7.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");
		double maxDistanceVehilce8 = (double) newVT8.getEngineInformation().getAttributes()
				.getAttribute("engeryCapacity")
				/ (double) newVT8.getEngineInformation().getAttributes().getAttribute("engeryConsumptionPerKm");

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30, maxDistanceVehicle7,
				MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong maximum distance of the tour of this vehicleType", 30, maxDistanceVehilce8,
				MatsimTestUtils.EPSILON);

		for (ScheduledTour scheduledTour : carrierV4.getSelectedPlan().getScheduledTours()) {

			String thisTypeId = scheduledTour.getVehicle().getVehicleType().getId().toString();
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
			if (thisTypeId == "SmallBattery_V4")
				Assert.assertEquals("The schedulded tour has a non expected distance", 24000, distanceTour,
						MatsimTestUtils.EPSILON);
			else if (thisTypeId == "DieselVehicle")
				Assert.assertEquals("The schedulded tour has a non expected distance", 36000, distanceTour,
						MatsimTestUtils.EPSILON);
			else
				Assert.fail("Wrong vehicleType used");
		}
	}

	/**
	 * Deletes the existing output file and sets the number of the last MATSim
	 * iteration.
	 * 
	 * @param config
	 */
	static Config prepareConfig(Config config, int lastMATSimIteration) {
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(lastMATSimIteration);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		return config;
	}

	private static void createServices(Carrier carrier, boolean threeServices, Carriers carriers) {
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

// Service 3
		if (threeServices == true) {
			CarrierService service3 = CarrierService.Builder
					.newInstance(Id.create("Service3", CarrierService.class), Id.createLinkId("j(9,2)"))
					.setServiceDuration(20).setServiceStartTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
					.setCapacityDemand(40).build();
			CarrierUtils.addService(carrier, service3);
		}
		carriers.addCarrier(carrier);
	}

	/**
	 * Creates the vehicle at the depot, ads this vehicle to the carriers and sets
	 * the capabilities. Sets TimeWindow for the carriers.
	 * 
	 * @param
	 */
	private static void createCarriers(Carriers carriers, FleetSize fleetSize, Carrier singleCarrier, Scenario scenario,
			CarrierVehicleTypes vehicleTypes) {
		double earliestStartingTime = 8 * 3600;
		double latestFinishingTime = 10 * 3600;
		List<CarrierVehicle> vehicles = new ArrayList<CarrierVehicle>();
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

	private static void solveJspritAndMATSim(Scenario scenario, CarrierVehicleTypes vehicleTypes, Carriers carriers,
			int jspritIterations) {
		solveWithJsprit(scenario, carriers, jspritIterations, vehicleTypes);
		final Controler controler = new Controler(scenario);

		scoringAndManagerFactory(scenario, carriers, controler);
		controler.run();
	}

	/**
	 * Solves with jsprit and gives a xml output of the plans and a plot of the
	 * solution. Because of using the distance constraint it is necessary to create
	 * a cost matrix before solving the vrp with jsprit. The jsprit algorithm solves
	 * a solution for every created carrier separately.
	 * 
	 * @param
	 */
	private static void solveWithJsprit(Scenario scenario, Carriers carriers, int jspritIteration,
			CarrierVehicleTypes vehicleTypes) {

		Network network = scenario.getNetwork();
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();

		for (Carrier singleCarrier : carriers.getCarriers().values()) {

			netBuilder.setTimeSliceWidth(1800);

			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(singleCarrier,
					network);
			vrpBuilder.setRoutingCost(netBasedCosts);

			VehicleRoutingProblem problem = vrpBuilder.build();

			StateManager stateManager = new StateManager(problem);

			StateId distanceStateId = stateManager.createStateId("distance");

			stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, netBasedCosts));

			ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
			constraintManager.addConstraint(
					new DistanceConstraint(distanceStateId, stateManager, vehicleTypes, netBasedCosts),
					ConstraintManager.Priority.CRITICAL);

			// get the algorithm out-of-the-box, search solution and get the best one.
			//
			VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
					.setStateAndConstraintManager(stateManager, constraintManager)
					.setProperty(Strategy.RADIAL_REGRET.toString(), "1.").buildAlgorithm();
//			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			algorithm.setMaxIterations(jspritIteration);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

			// Routing bestPlan to Network
			CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(singleCarrier, bestSolution);
			NetworkRouter.routePlan(carrierPlanServices, netBasedCosts);
			singleCarrier.setSelectedPlan(carrierPlanServices);

		}
	}

	/**
	 * @param
	 */
	static void scoringAndManagerFactory(Scenario scenario, Carriers carriers, final Controler controler) {
		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = createMyStrategymanager();

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory);
		controler.addOverridingModule(listener);
	}

	/**
	 * @param scenario
	 * @return
	 */
	private static CarrierScoringFunctionFactoryImpl createMyScoringFunction2(final Scenario scenario) {

		return new CarrierScoringFunctionFactoryImpl(scenario.getNetwork());
	}

	/**
	 * @return
	 */
	private static CarrierPlanStrategyManagerFactory createMyStrategymanager() {
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};
	}
}
