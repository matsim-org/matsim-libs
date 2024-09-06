/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;
import java.util.stream.Stream;

/**
 * A test to check the functionality of the VehicleSource.
 *
 * @author amit
 */

public class VehicleSourceTest {
	public static Stream<Arguments> arguments () {

		// create the combinations manually, since 'fromVehiclesData' in combination with 'isUsingPersonIdForMissingVehicleId' doesn't make sense
		return Stream.of(Arguments.of(VehiclesSource.defaultVehicle, true, true),
//				new Object[]{VehiclesSource.defaultVehicle, true, false}, // not meaningful for defaultVehicle
				Arguments.of(VehiclesSource.defaultVehicle, false, true),
//				new Object[]{VehiclesSource.defaultVehicle, false, false}, // not meaningful for defaultVehicle
				Arguments.of(VehiclesSource.modeVehicleTypesFromVehiclesData, true, true),
				Arguments.of(VehiclesSource.modeVehicleTypesFromVehiclesData, true, false),
				Arguments.of(VehiclesSource.modeVehicleTypesFromVehiclesData, false, true),
				Arguments.of(VehiclesSource.modeVehicleTypesFromVehiclesData, false, false),
				Arguments.of(VehiclesSource.fromVehiclesData, false, true),
				Arguments.of(VehiclesSource.fromVehiclesData, false, false)
		);
	}

	@RegisterExtension private MatsimTestUtils helper = new MatsimTestUtils();
	private Scenario scenario ;
	private final String[] transportModes = new String[]{"bike", "car"};
	private Link link1;
	private Link link2;
	private Link link3;

	@ParameterizedTest
	@MethodSource("arguments")
	void main(VehiclesSource vehiclesSource, boolean usingPersonIdForMissingVehicleId, boolean providingVehiclesInPerson) {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		createNetwork();
		createPlans(vehiclesSource, providingVehiclesInPerson);

		Config config = scenario.getConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(1.0);
		config.qsim().setMainModes(Arrays.asList(transportModes));
		//config.plansCalcRoute().setNetworkModes(Arrays.asList(transportModes));
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

		config.qsim().setVehiclesSource(vehiclesSource );
		config.qsim().setUsePersonIdForMissingVehicleId(usingPersonIdForMissingVehicleId );

		config.controller()
			.setOutputDirectory(
				helper.getOutputDirectory(vehiclesSource.name() + "_" + usingPersonIdForMissingVehicleId + "_" + providingVehiclesInPerson));
		config.controller().setLastIteration(0);
		config.controller().setWriteEventsInterval(1);
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);

		config.scoring().addActivityParams( new ActivityParams("h").setTypicalDuration(1. * 3600. ) );
		config.scoring().addActivityParams( new ActivityParams("w").setTypicalDuration(1. * 3600. ) );

		final Controler cont = new Controler(scenario);
		cont.getConfig().controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkTravelTimes = new HashMap<>();
		final VehicleLinkTravelTimeEventHandler handler = new VehicleLinkTravelTimeEventHandler(vehicleLinkTravelTimes);

		cont.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});

		boolean expectedException = false ;
		try{
			cont.run();
		} catch ( Exception ee ) {
			if ( providingVehiclesInPerson ){
				throw ee;
			} else {
				// (expected exception)
				System.err.println();
				System.err.println("The following is an expected exception:");
				System.err.println();
				System.err.println(ee.getMessage());
				System.err.println();
				System.err.println("(The above was an expected exception.)");
				System.err.println();
				expectedException = true;
			}
		}
		if ( providingVehiclesInPerson ) {
			Assertions.assertFalse( expectedException );
		} else {
			Assertions.assertTrue( expectedException );
			return ;
		}


		// usually all vehicles will have an id of the form personId_mode. If person id for missing vehicle id is used
		// the vehicle for mode car will have an id equal to the driver's id. All other modes will receive the normal ids
		Id<Vehicle> carId;
		if ( usingPersonIdForMissingVehicleId ) {
			carId = Id.createVehicleId("1");
		} else {
			carId = Id.createVehicleId("1_car");
		}

		Map<Id<Link>, Double> travelTime1 = vehicleLinkTravelTimes.get(Id.create("0_bike", Vehicle.class));
		Map<Id<Link>, Double> travelTime2 = vehicleLinkTravelTimes.get(carId);

		int bikeTravelTime = travelTime1.get(Id.create("2", Link.class)).intValue();
		int carTravelTime = travelTime2.get(Id.create("2", Link.class)).intValue();

		switch (vehiclesSource ) {
			case defaultVehicle: // both bike and car are default vehicles (i.e. identical)
				Assertions.assertEquals(0, bikeTravelTime - carTravelTime, MatsimTestUtils.EPSILON, "Both car, bike are default vehicles (i.e. identical), thus should have same travel time.");
				break;
			case modeVehicleTypesFromVehiclesData:
			case fromVehiclesData:
				Assertions.assertEquals(150, bikeTravelTime - carTravelTime, MatsimTestUtils.EPSILON, "Passing is not executed.");
				break;
			default:
				throw new RuntimeException("not implemented yet.");
		}

	}

	private void createNetwork(){
		Network network = scenario.getNetwork();

		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord( -100.0, 0.0) );
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));

		link1 = NetworkUtils.createAndAddLink(network, Id.create("1", Link.class), node1, node2, 100, 25, 600, 1, null, "22");
		link2 = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node2, node3, 1000, 25, 600, 1, null, "22");
		link3 = NetworkUtils.createAndAddLink(network, Id.create("3", Link.class), node3, node4, 100, 25, 600, 1, null, "22");
	}

	private void createPlans(VehiclesSource vehiclesSource, boolean providingVehiclesInPerson){

		Population population = scenario.getPopulation();
		VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();

		VehicleType bike = vehiclesFactory.createVehicleType(Id.create(transportModes[0], VehicleType.class));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);

		VehicleType car = vehiclesFactory.createVehicleType(Id.create(transportModes[1], VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);

		VehicleType [] vehTypes = {bike, car};

		for(int i=0;i<2;i++){
			Id<Person> id = Id.create(i, Person.class);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);
			{
				Activity a1 = population.getFactory().createActivityFromLinkId( "h", link1.getId() );
				a1.setEndTime( 8 * 3600 + i * 5 );
				plan.addActivity( a1 );
			}
			{
				Leg leg = population.getFactory().createLeg( transportModes[i] );
				plan.addLeg( leg );
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route = (NetworkRoute) factory.createRoute( link1.getId(), link3.getId() );
				route.setLinkIds( link1.getId(), Collections.singletonList( link2.getId() ), link3.getId() );
				leg.setRoute( route );
			}
			{
				Activity a2 = population.getFactory().createActivityFromLinkId( "w", link3.getId() );
				plan.addActivity( a2 );
				population.addPerson( p );
			}

			if ( !providingVehiclesInPerson ) {
				// i.e. testing what happens if the vehicles are not added into the person
				return ;
			}

			//adding vehicle type and vehicle to scenario as needed:
			switch (vehiclesSource ) {
				case defaultVehicle:
					//don't add anything
					break;
				case modeVehicleTypesFromVehiclesData:
					// only vehicle type is necessary
					if (!scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
						scenario.getVehicles().addVehicleType(vehTypes[i]);
					}
					break;
				case fromVehiclesData:
					// vehicle type as well as vehicle info is necessary
					if (!scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
						scenario.getVehicles().addVehicleType(vehTypes[i]);
					}

					Id<Vehicle> vId = VehicleUtils.createVehicleId(p, transportModes[i]);
					Vehicle v = vehiclesFactory.createVehicle(vId, vehTypes[i]);
					scenario.getVehicles().addVehicle(v);
					VehicleUtils.insertVehicleIdsIntoAttributes(p, Map.of(transportModes[i], vId));

					break;
				default:
					throw new RuntimeException("not implemented yet.");
			}
		}
	}

	private static class VehicleLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleTravelTimes;

		VehicleLinkTravelTimeEventHandler( Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes ) {
			this.vehicleTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.computeIfAbsent( event.getVehicleId(), vehicleId -> new HashMap<>() );
			travelTimes.put(event.getLinkId(), event.getTime() );
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.get( event.getVehicleId() );
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d;
					travelTimes.put(event.getLinkId(), time );
				}
			}
		}

		@Override
		public void reset(int iteration) {
			vehicleTravelTimes.clear();
		}
	}
}
