/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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


package org.matsim.contrib.freight.jsprit;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.net.URL;
import java.util.Collection;
import org.apache.log4j.Logger;


/**
 * @author kturner
 *
 */
public class FixedCostsTest extends MatsimTestCase {

    private final static Logger log = Logger.getLogger(FixedCostsTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    Carriers carriers = new Carriers();
    Carriers carriersPlannedAndRouted = new Carriers();

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
//        Create carrier with services; service1 nearby the depot, service2 at the opposite side of the network
        Carrier carrier1 = CarrierImpl.newInstance(Id.create("carrier1", Carrier.class));
        carrier1.getServices().add(createMatsimService("Service1", "i(3,0)", 1));
        carrier1.getServices().add(createMatsimService("Service2", "i(9,9)R", 1));

        Carrier carrier2 = CarrierImpl.newInstance(Id.create("carrier2", Carrier.class));
        carrier2.getServices().add(createMatsimService("Service1", "i(3,0)", 1));
        carrier2.getServices().add(createMatsimService("Service2", "i(9,9)R", 1));

        Carrier carrier3 = CarrierImpl.newInstance(Id.create("carrier3", Carrier.class));
        carrier3.getServices().add(createMatsimService("Service1", "i(3,0)", 1));
        carrier3.getServices().add(createMatsimService("Service2", "i(9,9)R", 1));


        //Create add vehicle for carriers
        CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;

        //only variable costs (per distance), no fixed costs
        CarrierVehicleType carrierVehType_A = CarrierVehicleType.Builder.newInstance(Id.create("gridType_A", VehicleType.class))
                .setCapacityWeightInTons(1 )
                .setMaxVelocity(10)
                .setCostPerDistanceUnit(0.001)      //1 EUR per km
                .setCostPerTimeUnit(0.0)
                .setFixCost(0)
                .setEngineInformation(new EngineInformation( EngineInformation.FuelType.diesel, 0.015) )
                .build();
        vehicleTypes.getVehicleTypes().put(carrierVehType_A.getId(), carrierVehType_A);
        CarrierVehicle carrierVehicle_A = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle_A", Vehicle.class), Id.createLinkId("i(1,0)")).setEarliestStart(0.0).setLatestEnd(36000.0).setTypeId(carrierVehType_A.getId()).build();

        //only fixed costs, no variable costs
        CarrierVehicleType carrierVehType_B = CarrierVehicleType.Builder.newInstance(Id.create("gridType_B", VehicleType.class))
                .setCapacityWeightInTons(1 )
                .setMaxVelocity(10)
                .setCostPerDistanceUnit(0.00001)
                .setCostPerTimeUnit(0.0)
                .setFixCost(10)
                .setEngineInformation(new EngineInformation( EngineInformation.FuelType.diesel, 0.015) )
                .build();
        vehicleTypes.getVehicleTypes().put(carrierVehType_B.getId(), carrierVehType_B);
        CarrierVehicle carrierVehicle_B = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle_B", Vehicle.class), Id.createLinkId("i(1,0)")).setEarliestStart(0.0).setLatestEnd(36000.0).setTypeId(carrierVehType_B.getId()).build();

        //carrier1: only vehicles of Type A (no fixed costs, variable costs: 1 EUR/km)
        CarrierCapabilities cc1 = CarrierCapabilities.Builder.newInstance()
                .addType(carrierVehType_A)
                .addVehicle(carrierVehicle_A)
                .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
                .build();
        carrier1.setCarrierCapabilities(cc1);
        carriers.addCarrier(carrier1);

        //carrier2: only vehicles of Type B (fixed costs of 10 EUR/verhicle, no variable costs)
        CarrierCapabilities cc2 = CarrierCapabilities.Builder.newInstance()
                .addType(carrierVehType_B)
                .addVehicle(carrierVehicle_B)
                .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
                .build();
        carrier2.setCarrierCapabilities(cc2);
        carriers.addCarrier(carrier2);

        //carrier3: has both vehicles of Type A (no fixed costs, variable costs: 1 EUR/km) and Type B (fixed costs of 10 EUR/verhicle, no variable costs)
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
		URL networkURL = IOUtils.newUrl( context, "grid9x9.xml" );
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

    @Test
    public final void test_carrier1CostsAreCorrectly() {
        //carrier1: only vehicles of Type A (no fixed costs, variable costs: 1 EUR/km)
           // nearby sercice1: 8km -> 8 EUR; service2: 36km -> 36 EUR  --> total 44EUR -> score = -44
        assertEquals(-44, carriersPlannedAndRouted.getCarriers().get(Id.create("carrier1", Carrier.class)).getSelectedPlan().getScore(), EPSILON);
    }

    @Test
    public final void test_carrier2CostsAreCorrectly() {
        //carrier2: only vehicles of Type B (fixed costs of 10 EUR/verhicle, no variable costs)
        assertEquals(-20.44, carriersPlannedAndRouted.getCarriers().get(Id.create("carrier2", Carrier.class)).getSelectedPlan().getScore(), EPSILON);
    }

    @Test
    public final void test_carrier3CostsAreCorrectly() {
        //carrier3: has both vehicles of Type A (no fixed costs, variable costs: 1 EUR/km) and Type B (fixed costs of 10 EUR/verhicle, no variable costs)
        //should use A for short trip (8 EUR) and B for the long trip (10.36 EUR)
        assertEquals(-18.36, carriersPlannedAndRouted.getCarriers().get(Id.create("carrier3", Carrier.class)).getSelectedPlan().getScore(), EPSILON);
    }

    private static CarrierService createMatsimService(String id, String to, int size) {
        return CarrierService.Builder.newInstance(Id.create(id, CarrierService.class), Id.create(to, Link.class))
                .setCapacityDemand(size)
                .setServiceDuration(31.0)
                .setServiceStartTimeWindow(TimeWindow.newInstance(3601.0, 36001.0))
                .build();
    }

}
