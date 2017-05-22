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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
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

/**
 * A test to check the functionality of the VehicleSource.
 * 
 * @author amit
 */

@RunWith(Parameterized.class)
public class VehicleSourceTest {

	private final VehiclesSource vehicleSource;
	private final boolean isUsingPersonIdForMissionVehicleId;

	public VehicleSourceTest(VehiclesSource vehicleSource, boolean isUsingPersonIdForMissionVehicleId) {
		this.vehicleSource = vehicleSource;
		this.isUsingPersonIdForMissionVehicleId = isUsingPersonIdForMissionVehicleId;
	}

	@Parameters(name = "{index}: vehicleSource == {0}; isUsingPersonIdForMissionVehicleId == {1}")
	public static Collection<Object[]> parameterObjects () {
		int nrow = VehiclesSource.values().length;
		Object [][] vehicleSources = new Object [nrow][2];
		int index = 0;
		for (VehiclesSource vs : VehiclesSource.values()) {
			vehicleSources[index] = new Object [] {vs, true};
//			vehicleSources[index] = new Object [] {vs, false}; // TODO : fix false cases
			index++;
		}
		return Arrays.asList(vehicleSources);
	}

	@Rule public MatsimTestUtils helper = new MatsimTestUtils();
	private Scenario scenario ;
	private final String transportModes [] = new String [] {"bike","car"};
	private Link link1;
	private Link link2;
	private Link link3;

	@Test
	public void main() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		createNetwork();
		createPlans();

		if(vehicleSource==VehiclesSource.modeVehicleTypesFromVehiclesData &&
				!isUsingPersonIdForMissionVehicleId &&
				scenario.getVehicles().getVehicles().size()==0) {
			throw new RuntimeException("Use of person id for mission vehicle id is not allowed and vehicle source is "+vehicleSource+
			". In such situation vehicles in the scenario must be present.");
		}

		Config config = scenario.getConfig();
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(1.0);
		config.qsim().setMainModes(Arrays.asList(transportModes));
		//config.plansCalcRoute().setNetworkModes(Arrays.asList(transportModes));
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

		config.qsim().setVehiclesSource(this.vehicleSource);
		config.qsim().setUsePersonIdForMissingVehicleId(this.isUsingPersonIdForMissionVehicleId);

		config.controler().setOutputDirectory(helper.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setWriteEventsInterval(1);
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);

		ActivityParams homeAct = new ActivityParams("h");
		ActivityParams workAct = new ActivityParams("w");
		homeAct.setTypicalDuration(1. * 3600.);
		workAct.setTypicalDuration(1. * 3600.);

		config.planCalcScore().addActivityParams(homeAct);
		config.planCalcScore().addActivityParams(workAct);

		final Controler cont = new Controler(scenario);
		cont.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkTravelTimes = new HashMap<>();
		final VehicleLinkTravelTimeEventHandler handler = new VehicleLinkTravelTimeEventHandler(vehicleLinkTravelTimes);

		cont.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});
		cont.run();

		Map<Id<Link>, Double> travelTime1= null ;
		switch( this.vehicleSource ) {
		case defaultVehicle:
		case fromVehiclesData:
			travelTime1 = vehicleLinkTravelTimes.get(Id.create("0", Vehicle.class));
			break;
		case modeVehicleTypesFromVehiclesData:
			travelTime1 = vehicleLinkTravelTimes.get(Id.create("0_bike", Vehicle.class));
			break;
		default:
			throw new RuntimeException("not implemented yet.");
		}
		
		Map<Id<Link>, Double> travelTime2 = vehicleLinkTravelTimes.get(Id.create("1", Vehicle.class));

		int bikeTravelTime = travelTime1.get(Id.create("2", Link.class)).intValue(); 
		int carTravelTime = travelTime2.get(Id.create("2", Link.class)).intValue();

		switch (this.vehicleSource) {
			case defaultVehicle: // both bike and car are default vehicles (i.e. identical)
				Assert.assertEquals("Both car, bike are default vehicles (i.e. identical), thus should have same travel time.",
						0, bikeTravelTime - carTravelTime, MatsimTestUtils.EPSILON);
				break;
			case modeVehicleTypesFromVehiclesData:
			case fromVehiclesData:
				Assert.assertEquals("Passing is not executed.", 150, bikeTravelTime - carTravelTime, MatsimTestUtils.EPSILON);
				break;
			default:
				throw new RuntimeException("not implemented yet.");
		}

	}

	private void createNetwork(){
		Network network = scenario.getNetwork();

		double x = -100.0;
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x, 0.0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));

        link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), node1, node2, (double) 100, (double) 25, (double) 600, (double) 1, null, "22");
        link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), node2, node3, (double) 1000, (double) 25, (double) 600, (double) 1, null, "22");
        link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), node3, node4, (double) 100, (double) 25, (double) 600, (double) 1, null, "22");
	}

	private void createPlans(){

		Population population = scenario.getPopulation();

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[0], VehicleType.class));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[1], VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);

		VehicleType [] vehTypes = {bike, car};

		for(int i=0;i<2;i++){
			Id<Person> id = Id.create(i, Person.class);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
			a1.setEndTime(8*3600+i*5);
			Leg leg = population.getFactory().createLeg(transportModes[i]);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
			route.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
			leg.setRoute(route);

			Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
			plan.addActivity(a2);
			population.addPerson(p);

			//adding vehicle type and vehicle to scenario if vehicleSource is not modeVehicleTypesFromVehiclesData

			switch (this.vehicleSource) {
				case defaultVehicle:
					//don't add anything
					break;
				case modeVehicleTypesFromVehiclesData:
					// only vehicle type is necessary
					if(! scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
						scenario.getVehicles().addVehicleType(vehTypes[i]);
					}
					break;
				case fromVehiclesData:
					// vehicle type as well as vehicle info is necessary
					if(! scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
						scenario.getVehicles().addVehicleType(vehTypes[i]);
					}

					Id<Vehicle> vId = Id.create(p.getId(),Vehicle.class);
					Vehicle v = VehicleUtils.getFactory().createVehicle(vId, vehTypes[i]);
					scenario.getVehicles().addVehicle(v);

					break;
					default:
						throw new RuntimeException("not implemented yet.");
			}
		}
	}

	private static class VehicleLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleTravelTimes;

		public VehicleLinkTravelTimeEventHandler(Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes) {
			this.vehicleTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.get(event.getVehicleId());
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.vehicleTravelTimes.put(event.getVehicleId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.get( event.getVehicleId() );
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d.doubleValue();
					travelTimes.put(event.getLinkId(), Double.valueOf(time));
				}
			}
		}

		@Override
		public void reset(int iteration) {
			vehicleTravelTimes.clear();
		}
	}
}