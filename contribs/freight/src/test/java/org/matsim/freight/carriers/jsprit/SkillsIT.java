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

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

public class SkillsIT {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();
	private final Id<Link> carrierLocation = Id.createLinkId("i(1,0)");

	@Test
	void testJspritWithDifferentSkillsRequired() {
		/* First test with different skills. */
		Scenario scenario = setupTestScenario();
		addShipmentsRequiringDifferentSkills(scenario);
		VehicleRoutingProblemSolution solutionWithDifferentSkills = null;
		try {
			solutionWithDifferentSkills = generateCarrierPlans(scenario);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Should run integration test without exception.");
		}
		Assertions.assertEquals(2L, solutionWithDifferentSkills.getRoutes().size(), "Wrong number of vehicles.");
		Assertions.assertEquals(2086.9971014492755, solutionWithDifferentSkills.getCost(), MatsimTestUtils.EPSILON, "Wrong carrier score.");
	}

	@Test
	void testJspritWithSameSkillsRequired(){
		/* Test with same skills. */
		Scenario scenario = setupTestScenario();
		addShipmentsRequiringSameSkills(scenario);
		VehicleRoutingProblemSolution solutionWithSameSkills = null;
		try {
			solutionWithSameSkills = generateCarrierPlans(scenario);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Should run integration test without exception.");
		}
		Assertions.assertEquals(1L, solutionWithSameSkills.getRoutes().size(), "Wrong number of vehicles.");
		Assertions.assertEquals(1044.0985507246377, solutionWithSameSkills.getCost(), MatsimTestUtils.EPSILON, "Wrong carrier score.");
	}

	private VehicleRoutingProblemSolution generateCarrierPlans(Scenario scenario) {
		Carrier carrier = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("TestCarrier", Carrier.class));
		NetworkBasedTransportCosts networkBasedTransportCosts = NetworkBasedTransportCosts.Builder.newInstance(
				scenario.getNetwork(), carrier.getCarrierCapabilities().getVehicleTypes())
				.setTimeSliceWidth((int) Time.parseTime("00:30:00"))
				.build();
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
		vrpBuilder.setRoutingCost(networkBasedTransportCosts);
		VehicleRoutingProblem problem = vrpBuilder.build();

		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

		VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
		CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution);

		NetworkRouter.routePlan(newPlan, networkBasedTransportCosts);
		carrier.setSelectedPlan(newPlan);
		SolutionPrinter.print(problem, solution, SolutionPrinter.Print.VERBOSE);

//		new CarrierPlanXmlWriterV3(CarrierControlerUtils.getCarriers(scenario)).write(utils.getOutputDirectory() + "carriers.xml");
//		Scenario scNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new CarrierPlanXmlReaderV3(CarrierControlerUtils.getCarriers(scNew)).readFile(utils.getOutputDirectory() + "carriers.xml");
		return solution;
	}

	private Scenario setupTestScenario() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(utils.getClassInputDirectory() + "grid-network.xml");

		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario );
		{
			Carrier carrier = CarriersUtils.createCarrier( Id.create("TestCarrier", Carrier.class));
			{
				CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
				capabilitiesBuilder.setFleetSize(CarrierCapabilities.FleetSize.FINITE);

//				CostInformation costInformation1 = new CostInformation() ;
//				costInformation1.setFixedCosts( 1000.0 );
//				costInformation1.setCostsPerMeter( 0.001 );
//				costInformation1.setCostsPerSecond( 0.001 );
//				VehicleCapacity vehicleCapacity = new VehicleCapacity();
//				vehicleCapacity.setWeightInTons(2.0);

				/* Vehicle type 1. */
				VehicleType typeOne = scenario.getVehicles().getFactory().createVehicleType(Id.create("Type 1", VehicleType.class));
//				typeOne.setCostInformation(costInformation1);
				typeOne.getCostInformation().setFixedCost( 1000.0 ).setCostsPerMeter( 0.001 ).setCostsPerSecond( 0.001 ) ;
//				typeOne.setCapacity(vehicleCapacity);
				typeOne.getCapacity().setOther( 2.0 );
				CarriersUtils.addSkill(typeOne, "skill 1");
				capabilitiesBuilder.addType(typeOne);
				CarrierVehicle vehicleOne = CarrierVehicle.Builder.newInstance(Id.createVehicleId("1"), carrierLocation, typeOne )
						.setEarliestStart(0.0)
						.setLatestEnd(Time.parseTime("24:00:00"))
						.build();
				capabilitiesBuilder.addVehicle(vehicleOne);

				/* Vehicle type 2. */
				VehicleType typeTwo = scenario.getVehicles().getFactory().createVehicleType(Id.create("Type 1", VehicleType.class));
//				typeTwo.setCostInformation(costInformation1);
				typeTwo.getCostInformation().setFixedCost( 1000.0 ).setCostsPerMeter( 0.001 ).setCostsPerSecond( 0.001 ) ;
//				typeTwo.setCapacity(vehicleCapacity);
				typeTwo.getCapacity().setOther( 2.0 );
				CarriersUtils.addSkill(typeTwo, "skill 2");
				capabilitiesBuilder.addType(typeTwo);
				CarrierVehicle vehicleTwo = CarrierVehicle.Builder.newInstance(Id.createVehicleId("2"), carrierLocation, typeTwo )
						.setEarliestStart(0.0)
						.setLatestEnd(Time.parseTime("24:00:00"))
						.build();
				capabilitiesBuilder.addVehicle(vehicleTwo);

				carrier.setCarrierCapabilities(capabilitiesBuilder.build());
			}
			carriers.addCarrier(carrier);
		}

		return scenario;
	}

	private void addShipmentsRequiringDifferentSkills(Scenario scenario) {
		Carrier carrier = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("TestCarrier", Carrier.class));
		CarrierShipment shipmentOne = CarrierShipment.Builder.newInstance(
				Id.create("1", CarrierShipment.class),
				carrierLocation,
				Id.createLinkId("i(10,10)R"),
				1)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setPickupServiceTime(Time.parseTime("00:05:00"))
				.setDeliveryTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setDeliveryServiceTime(Time.parseTime("00:05:00"))
				.build();
		CarriersUtils.addSkill(shipmentOne, "skill 1");
		CarriersUtils.addShipment(carrier, shipmentOne);

		CarrierShipment shipmentTwo = CarrierShipment.Builder.newInstance(
				Id.create("2", CarrierShipment.class),
				carrierLocation,
				Id.createLinkId("i(10,10)R"),
				1)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setPickupServiceTime(Time.parseTime("00:05:00"))
				.setDeliveryTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setDeliveryServiceTime(Time.parseTime("00:05:00"))
				.build();
		CarriersUtils.addSkill(shipmentTwo, "skill 2");
		CarriersUtils.addShipment(carrier, shipmentTwo);
	}

	private void addShipmentsRequiringSameSkills(Scenario scenario) {
		Carrier carrier = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("TestCarrier", Carrier.class));
		CarrierShipment shipmentOne = CarrierShipment.Builder.newInstance(
				Id.create("1", CarrierShipment.class),
				carrierLocation,
				Id.createLinkId("i(10,10)R"),
				1)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setPickupServiceTime(Time.parseTime("00:05:00"))
				.setDeliveryTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setDeliveryServiceTime(Time.parseTime("00:05:00"))
				.build();
		CarriersUtils.addSkill(shipmentOne, "skill 1");
		CarriersUtils.addShipment(carrier, shipmentOne);

		CarrierShipment shipmentTwo = CarrierShipment.Builder.newInstance(
				Id.create("2", CarrierShipment.class),
				carrierLocation,
				Id.createLinkId("i(10,10)R"),
				1)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setPickupServiceTime(Time.parseTime("00:05:00"))
				.setDeliveryTimeWindow(TimeWindow.newInstance(0.0, Time.parseTime("24:00:00")))
				.setDeliveryServiceTime(Time.parseTime("00:05:00"))
				.build();
		CarriersUtils.addSkill(shipmentTwo, "skill 1");
		CarriersUtils.addShipment(carrier, shipmentTwo);
	}

}
