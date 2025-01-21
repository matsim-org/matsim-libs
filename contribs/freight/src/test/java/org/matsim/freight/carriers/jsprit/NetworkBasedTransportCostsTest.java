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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

public class NetworkBasedTransportCostsTest {


	private static final String TYPE_1 = "type1";
	private static final String TYPE_2 = "type2";
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test_whenAddingTwoDifferentVehicleTypes_itMustAccountForThem(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = utils.getClassInputDirectory() + "network.xml";
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILENAME);

		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(network);
		builder.addVehicleTypeSpecificCosts(TYPE_1, 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts(TYPE_2, 20.0, 0.0, 4.0);
		NetworkBasedTransportCosts c = builder.build();

		Vehicle vehicle1 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type1 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn(TYPE_1);
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn(TYPE_2);
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		Assertions.assertEquals(20000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		Assertions.assertEquals(40000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle1), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle2), 0.01);
	}

	@Test
	void test_whenVehicleTypeNotKnow_throwException(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = utils.getClassInputDirectory() + "network.xml";
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILENAME);

		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(network);
		builder.addVehicleTypeSpecificCosts(TYPE_1, 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts(TYPE_2, 20.0, 0.0, 4.0);
		NetworkBasedTransportCosts c = builder.build();

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn("typeNotKnown");
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		try{
			c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2);
		}
		catch(IllegalStateException e){ Assertions.assertTrue(true); }

	}

	@Test
	void test_whenAddingTwoVehicleTypesViaConstructor_itMustAccountForThat(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = utils.getClassInputDirectory() + "network.xml";
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILENAME);

		VehicleType vehType1 = VehicleUtils.getFactory().createVehicleType(Id.create(TYPE_1, VehicleType.class ));

		CostInformation costInformation1 = vehType1.getCostInformation() ;
		costInformation1.setFixedCost( 0.0 );
		costInformation1.setCostsPerMeter( 2.0 );
		costInformation1.setCostsPerSecond( 0.0 );

		VehicleType vehType2 = VehicleUtils.getFactory().createVehicleType(Id.create(TYPE_2, VehicleType.class ));

		CostInformation costInformation = vehType2.getCostInformation() ;
		costInformation.setFixedCost( 0.0 );
		costInformation.setCostsPerMeter( 4.0 );
		costInformation.setCostsPerSecond( 0.0 );

		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder =
			NetworkBasedTransportCosts.Builder.newInstance(network,Arrays.asList(vehType1,vehType2));
		NetworkBasedTransportCosts networkBasedTransportCosts = builder.build();

		Vehicle vehicle1 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type1 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn(TYPE_1);
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn(TYPE_2);
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		Assertions.assertEquals(20000.0, networkBasedTransportCosts.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		Assertions.assertEquals(40000.0, networkBasedTransportCosts.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);
		Assertions.assertEquals(20000.0, networkBasedTransportCosts.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle1), 0.01);
		Assertions.assertEquals(20000.0, networkBasedTransportCosts.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle2), 0.01);
	}


	/**
	 *  This test is a modified version of {@link #test_whenAddingTwoDifferentVehicleTypes_itMustAccountForThem}
	 *  In addition, there is added a road pricing scheme to toll all vehicles
	 */
	@Test
	void test_whenAddingTwoDifferentVehicleTypes_tollAllTypes(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(utils.getClassInputDirectory() + "network.xml");

		//Create Rp Scheme from code.
		RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		/* Configure roadpricing scheme. */
		RoadPricingUtils.setName(scheme, "DemoToll4Test");
		RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
		RoadPricingUtils.setDescription(scheme, "Tolling scheme for test.");

		/* Add general link based toll for one link */
		RoadPricingUtils.addLink(scheme, Id.createLinkId("21"));
		RoadPricingUtils.createAndAddGeneralCost(scheme, Time.parseTime("00:00:00"), Time.parseTime("72:00:00"), 99.99);

		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork());
		builder.addVehicleTypeSpecificCosts(TYPE_1, 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts(TYPE_2, 20.0, 0.0, 4.0);
		builder.setRoadPricingScheme(scheme);
		NetworkBasedTransportCosts c = builder.build();

		Vehicle vehicle1 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type1 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn(TYPE_1);
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn(TYPE_2);
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		//vehicle1: includes toll
		Assertions.assertEquals(20099.99, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle1), 0.01);

		//vehicle 2: no toll
		Assertions.assertEquals(40099.99, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle2), 0.01);
	}

	/**
	 *  This test is a modified version of {@link #test_whenAddingTwoDifferentVehicleTypes_itMustAccountForThem}
	 *  In addition, there is added a road pricing scheme.
	 *  The scheme is only set for one vehicle type: type1.
	 *  So, only the vehicle using that type (vehicle1) should be tolled, the other (vehicle2) not.
	 *  This test is build, in a way, that it uses the MATSim infrastructure for filtering the vehicles in the toll factor.
	 *  To see it just on jsprit setting, please refer to {@link #test_whenAddingTwoDifferentVehicleTypes_tollBasedOnVehicleId}
	 */
	@Test
	void test_whenAddingTwoDifferentVehicleTypes_tollOneTypeTollFactor(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(utils.getClassInputDirectory() + "network.xml");

		//Create RoadPricing Scheme from code.
		RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		/* Configure roadpricing scheme. */
		RoadPricingUtils.setName(scheme, "DemoToll4Test");
		RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
		RoadPricingUtils.setDescription(scheme, "Tolling scheme for test.");

		/* Add general link based toll for one link */
		RoadPricingUtils.addLink(scheme, Id.createLinkId("21"));
		RoadPricingUtils.createAndAddGeneralCost(scheme, Time.parseTime("00:00:00"), Time.parseTime("72:00:00"), 99.99);

		//Use toll factor to only toll vehicles of type1.
		TollFactor tollFactor = (personId, vehicleId, linkId, time) -> {
			double tollFactor1 = 0.;
			var vehTypeIdString = VehicleUtils.findVehicle(vehicleId, scenario).getType().getId().toString(); //This needs, the vehicles registered in the scenario.
			if (TYPE_1.equals(vehTypeIdString)) {
				tollFactor1 = 1.;
			}
			return tollFactor1;
		};
		RoadPricingScheme rpSchemeWTollFactor = new RoadPricingSchemeUsingTollFactor( scheme , tollFactor );

		/// End creating roadPricing scheme from Code

		//Build MATSim vehicles and convert them to jsprit Vehicles ... just to see, that this works across the whole chain and
		//that we can filter/search by (MATSim) vehicle types in TollFactor usage
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		final Vehicle vehicle1;
		{
			VehicleType vehType1 = vf.createVehicleType( Id.create( TYPE_1, VehicleType.class ) );
				vehType1.getCostInformation().setFixedCost(10.0);
				vehType1.getCostInformation().setCostsPerSecond(0.0);
				vehType1.getCostInformation().setCostsPerMeter(2.0);
			scenario.getVehicles().addVehicleType( vehType1 );
			CarrierVehicle matsimVehicle1 = CarrierVehicle.newInstance( Id.createVehicleId( "vehicle1" ), Id.createLinkId("20"), vehType1 );
			scenario.getVehicles().addVehicle( matsimVehicle1 );
			vehicle1 = MatsimJspritFactory.createJspritVehicle( matsimVehicle1, new Coord() );
		}

		final Vehicle vehicle2;
		{
			VehicleType vehType2 = vf.createVehicleType(Id.create(TYPE_2, VehicleType.class));
				vehType2.getCostInformation().setFixedCost(20.0);
				vehType2.getCostInformation().setCostsPerSecond(0.0);
				vehType2.getCostInformation().setCostsPerMeter(4.0);
			scenario.getVehicles().addVehicleType(vehType2);

			CarrierVehicle matsimVehicle2 = CarrierVehicle.newInstance(Id.createVehicleId("vehicle2"), Id.createLinkId("20"), vehType2);
			scenario.getVehicles().addVehicle(matsimVehicle2);
			vehicle2 = MatsimJspritFactory.createJspritVehicle(matsimVehicle2, new Coord());
		}

		//Build the NetbasedTransportCosts object with the roadpricing scheme.
		NetworkBasedTransportCosts c = NetworkBasedTransportCosts.Builder.newInstance(
				scenario.getNetwork(),
				scenario.getVehicles().getVehicleTypes().values() )
			.setRoadPricingScheme(rpSchemeWTollFactor)
			.build() ;

		//vehicle1: includes toll
		Assertions.assertEquals(20099.99, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle1), 0.01);

		//vehicle 2: no toll
		Assertions.assertEquals(40000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle2), 0.01);
	}


	/**
	 *  This test is a modified version of {@link #test_whenAddingTwoDifferentVehicleTypes_itMustAccountForThem}
	 *  In addition, there is added a road pricing scheme.
	 *  With using a toll factor, the tolling can be set differently for the two vehicles.
	 *  This test is build, in a way, that it uses the JSPRIT vehicle directly.
	 *  To see it with the MATSim settings, please refer to {@link #test_whenAddingTwoDifferentVehicleTypes_tollOneTypeTollFactor()}
	 */
	@Test
	void test_whenAddingTwoDifferentVehicleTypes_tollBasedOnVehicleId(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(utils.getClassInputDirectory() + "network.xml");

		//Create Rp Scheme from code.
		RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		/* Configure roadpricing scheme. */
		RoadPricingUtils.setName(scheme, "DemoToll4TestType1");
		RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
		RoadPricingUtils.setDescription(scheme, "Tolling scheme for test.");

		/* Add general link based toll for one link */
		RoadPricingUtils.addLink(scheme, Id.createLinkId("21"));
		RoadPricingUtils.createAndAddGeneralCost(scheme, Time.parseTime("00:00:00"), Time.parseTime("72:00:00"), 99.99);

		//Use a factor to take into account the different types. type2 gehts tolled with 50% of the toll of type1
		TollFactor tollFactor = (personId, vehicleId, linkId, time) -> {
			//No information about the vehicleType available anywhere, because it is not registered centrally.
			// -> Use the vehicleId to distinguish the types.
			var vehTypeIdString = vehicleId.toString();
			if (vehTypeIdString.equals("vehicle1")) {
				return 1;
			} else if (vehTypeIdString.equals("vehicle2")) {
				return 0.5;
			} else {
				return 0;
			}
		};
		RoadPricingSchemeUsingTollFactor rpSchemeWTollFactor = new RoadPricingSchemeUsingTollFactor( scheme , tollFactor );

		///___ End creating toll scheme from code

		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork());
		builder.addVehicleTypeSpecificCosts(TYPE_1, 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts(TYPE_2, 20.0, 0.0, 4.0);
		builder.setRoadPricingScheme(rpSchemeWTollFactor); //add the rpCalculator to activate the tolling.
		NetworkBasedTransportCosts c = builder.build();

		Vehicle vehicle1 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type1 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn(TYPE_1);
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn(TYPE_2);
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		//vehicle1: includes toll of 99.99 for entering the final link
		Assertions.assertEquals(20099.99, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle1), 0.01);

		//vehicle 2: includes toll of 49.995 (50% of 99.99) for entering the final link
		Assertions.assertEquals(40049.995, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);
		Assertions.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle2), 0.01);
	}

}
