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
import com.graphhopper.jsprit.core.util.Solutions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.net.URL;
import java.util.Collection;


/**
 * @author kturner
 *
 */
public class FixedCostsTest  {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private final static Logger log = LogManager.getLogger(FixedCostsTest.class);

	private final Carriers carriers = new Carriers();
	private final Carriers carriersPlannedAndRouted = new Carriers();

	@BeforeEach
	public void setUp() throws Exception {
		// Create carrier with services; service1 nearby the depot, service2 at the opposite side of the network
		CarrierService service1 = createMatsimService("Service1", "i(3,0)", 1);
		CarrierService service2 = createMatsimService("Service2", "i(9,9)R", 1);

		Carrier carrier1 = CarriersUtils.createCarrier(Id.create("carrier1", Carrier.class ) );
		CarriersUtils.addService(carrier1, service1);
		CarriersUtils.addService(carrier1, service2);

		Carrier carrier2 = CarriersUtils.createCarrier(Id.create("carrier2", Carrier.class ) );
		CarriersUtils.addService(carrier2, service1);
		CarriersUtils.addService(carrier2, service2);

		Carrier carrier3 = CarriersUtils.createCarrier(Id.create("carrier3", Carrier.class ) );
		CarriersUtils.addService(carrier3, service1);
		CarriersUtils.addService(carrier3, service2);


		//Create add vehicle for carriers
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;

		//only variable costs (per distance), no fixed costs
		final Id<VehicleType> vehicleTypeId = Id.create( "gridType_A", VehicleType.class );
		VehicleType carrierVehType_A = VehicleUtils.getFactory().createVehicleType( vehicleTypeId );
		{
			EngineInformation engineInformation1 = carrierVehType_A.getEngineInformation();
			engineInformation1.setFuelType( EngineInformation.FuelType.diesel );
			engineInformation1.setFuelConsumption( 0.015 );
			carrierVehType_A.getCapacity().setOther( 1. );
			carrierVehType_A.getCostInformation().setFixedCost( 0. ).setCostsPerMeter( 0.001 ).setCostsPerSecond( 0.0 );
			carrierVehType_A.setMaximumVelocity( 10 );
			vehicleTypes.getVehicleTypes().put( carrierVehType_A.getId(), carrierVehType_A );
		}
		CarrierVehicle carrierVehicle_A = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle_A", Vehicle.class), Id.createLinkId("i(1,0)"),
				carrierVehType_A ).setEarliestStart(0.0 ).setLatestEnd(36000.0 ).build();

		//only fixed costs, no variable costs
		final Id<VehicleType> vehicleTypeId1 = Id.create( "gridType_B", VehicleType.class );
		VehicleType carrierVehType_B = VehicleUtils.getFactory().createVehicleType( vehicleTypeId1 );
		{
			EngineInformation engineInformation = carrierVehType_B.getEngineInformation();
			engineInformation.setFuelType( EngineInformation.FuelType.diesel );
			engineInformation.setFuelConsumption( 0.015 );
			carrierVehType_B.getCapacity().setOther( 1. );
			carrierVehType_B.getCostInformation().setFixedCost( 10. ).setCostsPerMeter( 0.00001 ).setCostsPerSecond( 0. ) ;
			carrierVehType_B.setMaximumVelocity( 10. );
			vehicleTypes.getVehicleTypes().put( carrierVehType_B.getId(), carrierVehType_B );
		}
		CarrierVehicle carrierVehicle_B = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle_B", Vehicle.class), Id.createLinkId("i(1,0)"),
				carrierVehType_B ).setEarliestStart(0.0 ).setLatestEnd(36000.0 ).build();

		//carrier1: only vehicles of Type A (no fixed costs, variable costs: 1 EUR/km)
		CarrierCapabilities cc1 = CarrierCapabilities.Builder.newInstance()
										     .addType(carrierVehType_A)
										     .addVehicle(carrierVehicle_A)
										     .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
										     .build();
		carrier1.setCarrierCapabilities(cc1);
		carriers.addCarrier(carrier1);

		//carrier2: only vehicles of Type B (fixed costs of 10 EUR/vehicle, no variable costs)
		CarrierCapabilities cc2 = CarrierCapabilities.Builder.newInstance()
										     .addType(carrierVehType_B)
										     .addVehicle(carrierVehicle_B)
										     .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
										     .build();
		carrier2.setCarrierCapabilities(cc2);
		carriers.addCarrier(carrier2);

		//carrier3: has both vehicles of Type A (no fixed costs, variable costs: 1 EUR/km) and Type B (fixed costs of 10 EUR/vehicle, no variable costs)
		CarrierCapabilities cc3 = CarrierCapabilities.Builder.newInstance()
										     .addType(carrierVehType_A)
										     .addType(carrierVehType_B)
										     .addVehicle(carrierVehicle_A)
										     .addVehicle(carrierVehicle_B)
										     .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
										     .build();
		carrier3.setCarrierCapabilities(cc3);
		carriers.addCarrier(carrier3);


		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

		//load Network and build netbasedCosts for jsprit

		URL context = org.matsim.examples.ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" );
		URL networkURL = IOUtils.extendUrl(context, "grid9x9.xml");
        Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readURL(networkURL);

		NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
		netBuilder.setTimeSliceWidth(86400) ; // !!!!, otherwise it will not do anything.

		for (Carrier carrier : carriers.getCarriers().values()) {
			log.info("creating and solving VRP for carrier: " + carrier.getId().toString());
			//Build VRP
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build();

			// get the algorithm out-of-the-box, search solution and get the best one.
			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			algorithm.setMaxIterations(100);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

			//Routing bestPlan to Network
			CarrierPlan carrierPlan = MatsimJspritFactory.createPlan(carrier, bestSolution) ;
			NetworkRouter.routePlan(carrierPlan,netBasedCosts) ;
			carrier.setSelectedPlan(carrierPlan) ;
			carriersPlannedAndRouted.addCarrier(carrier);
		}
	}

	/*
	 * carrier1: only vehicles of Type A (no fixed costs, variable costs: 1 EUR/km)
	 * nearby service1: 8km -> 8 EUR; service2: 36km -> 36 EUR  --> total 44EUR -> score = -44
	 */
	@Test
	final void test_carrier1CostsAreCorrectly() {

		Assertions.assertEquals(-44, carriersPlannedAndRouted.getCarriers().get(Id.create("carrier1", Carrier.class)).getSelectedPlan().getJspritScore(), MatsimTestUtils.EPSILON);
	}

	/*
	 * carrier2: only vehicles of Type B (fixed costs of 10 EUR/vehicle, no variable costs)
	 */
	@Test
	final void test_carrier2CostsAreCorrectly() {
		Assertions.assertEquals(-20.44, carriersPlannedAndRouted.getCarriers().get(Id.create("carrier2", Carrier.class)).getSelectedPlan().getJspritScore(), MatsimTestUtils.EPSILON);
	}

	/*
	 * carrier3: has both vehicles of Type A (no fixed costs, variable costs: 1 EUR/km) and Type B (fixed costs of 10 EUR/vehicle, no variable costs)
	 * should use A for short trip (8 EUR) and B for the long trip (10.36 EUR)
	*/
	@Test
	final void test_carrier3CostsAreCorrectly() {
		Assertions.assertEquals(-18.36, carriersPlannedAndRouted.getCarriers().get(Id.create("carrier3", Carrier.class)).getSelectedPlan().getJspritScore(), MatsimTestUtils.EPSILON);
	}

	private static CarrierService createMatsimService(String id, String to, int size) {
		return CarrierService.Builder.newInstance(Id.create(id, CarrierService.class), Id.create(to, Link.class))
						     .setCapacityDemand(size)
						     .setServiceDuration(31.0)
						     .setServiceStartTimeWindow(TimeWindow.newInstance(3601.0, 36001.0))
						     .build();
	}

}
