/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.seepage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Tests that in congested part, walk (seep mode) can overtake (seep) car mode.
 * 
 */
public class SeepageSchematicPlotter {
	static private final Logger log = Logger.getLogger( SeepageSchematicPlotter.class);

	public static void main(String [] args){

		SimpleNetwork net = new SimpleNetwork();

		Scenario sc = net.scenario;
		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
		sc.getConfig().qsim().setSnapshotPeriod(1);
		sc.getConfig().qsim().setSnapshotStyle(SnapshotStyle.queue);

		sc.getConfig().controler().setSnapshotFormat(Arrays.asList( "transims", "otfvis" ));
		sc.getConfig().controler().setOutputDirectory("./output/");
		sc.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		sc.getConfig().controler().setLastIteration(0);
		sc.getConfig().controler().setWriteEventsInterval(1);
		sc.getConfig().controler().setCreateGraphs(false);
		sc.getConfig().controler().setDumpDataAtEnd(false);

		ActivityParams homeAct = new ActivityParams("h");
		ActivityParams workAct = new ActivityParams("w");
		homeAct.setTypicalDuration(1*3600);
		workAct.setTypicalDuration(1*3600);

		sc.getConfig().planCalcScore().addActivityParams(homeAct);
		sc.getConfig().planCalcScore().addActivityParams(workAct);

		sc.getConfig().qsim().setLinkWidthForVis((float)0);
		sc.getNetwork().setEffectiveLaneWidth(0.);

		Map<String, VehicleType> modesType = new HashMap<>();
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car,VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		modesType.put(TransportMode.car, car);
		sc.getVehicles().addVehicleType(car);

		VehicleType motorbike = VehicleUtils.getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
		motorbike.setMaximumVelocity(20);
		motorbike.setPcuEquivalents(0.25);
		modesType.put("motorbike", motorbike);
		sc.getVehicles().addVehicleType(motorbike);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.bike,VehicleType.class));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);
		modesType.put(TransportMode.bike, bike);
		sc.getVehicles().addVehicleType(bike);

		for (int i=0;i<4;i++){
			Id<Person> id = Id.createPersonId(i);
			Person p = net.population.getFactory().createPerson(id);
			Plan plan = net.population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
			Leg leg;
			if(i == 0 || i == 1){
				a1.setEndTime( 200 + i );
				leg = net.population.getFactory().createLeg(TransportMode.car);
			} else if( i==2 ){
				a1.setEndTime(49);
				leg = net.population.getFactory().createLeg(TransportMode.bike);
			} else {
				a1.setEndTime(199);
				leg = net.population.getFactory().createLeg("motorbike");
			}

			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);
			Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link3.getId());
			plan.addActivity(a2);
			net.population.addPerson(p);

			Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId,modesType.get(leg.getMode()));
			sc.getVehicles().addVehicle(vehicle);
		}

		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes = new HashMap<>();

		PersonLinkTravelTimeEventHandler handler = new PersonLinkTravelTimeEventHandler(personLinkTravelTimes);

		Controler cnt = new Controler(sc);
		cnt.addOverridingModule(new AbstractModule(
				) {

			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});

		cnt.run();

		Map<Id<Link>, Double> travelTime1 = personLinkTravelTimes.get(Id.createPersonId("0"));
		Map<Id<Link>, Double> travelTime2 = personLinkTravelTimes.get(Id.createPersonId("1"));
		Map<Id<Link>, Double> travelTime3 = personLinkTravelTimes.get(Id.createPersonId("2"));

		int carTravelTime = travelTime1.get(Id.createLinkId("2")).intValue();
		int bikeTravelTime = travelTime2.get(Id.createLinkId("2")).intValue();
		int motorbikeTravelTime = travelTime3.get(Id.createLinkId("2")).intValue();
		
		System.out.println(carTravelTime);
		
		System.out.println(bikeTravelTime);
		
		System.out.println(motorbikeTravelTime);
		
		Assert.assertEquals("Wrong car travel time", 116, carTravelTime);
		Assert.assertEquals("Wrong walk travel time.", 1010, bikeTravelTime);
		Assert.assertEquals("Seepage is not implemented", 894, bikeTravelTime-carTravelTime);
	}

	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final Network network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
			config.qsim().setMainModes(Arrays.asList(TransportMode.car,TransportMode.bike,"motorbike"));
			config.qsim().setLinkDynamics(LinkDynamics.SeepageQ);

			config.qsim().setSeepModes(Arrays.asList(TransportMode.bike,"motorbike") );
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);

			network = scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(0.0, 0.0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(0.0, 100.0));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord(0.0, 1100.0));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord(0.0, 1200.0));

			Set<String> allowedModes = new HashSet<>(); allowedModes.addAll(Arrays.asList(TransportMode.car,TransportMode.bike,"motorbike"));

			link1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("1"), node1, node2, 100, 25, 36000, 1, null, "22"); 
			link2 = NetworkUtils.createAndAddLink(network, Id.createLinkId("2"), node2, node3, 1000, 25, 60, 1, null, "22");	//flow capacity is 1 PCU per min.
			link3 = NetworkUtils.createAndAddLink(network, Id.createLinkId("3"), node3, node4, 100, 25, 36000, 1, null, "22");

			for(Link l :network.getLinks().values()){
				l.setAllowedModes(allowedModes);
			}

			population = scenario.getPopulation();
		}
	}
	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes;

		public PersonLinkTravelTimeEventHandler(final Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes) {
			this.personLinkTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			System.out.println(event.toString());
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(Id.createPersonId(event.getVehicleId()));
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.personLinkTravelTimes.put(Id.createPersonId(event.getVehicleId()), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
			if ( event.getLinkId().equals( Id.createLinkId("2") ) ) {
				log.info( event );
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			System.out.println(event.toString());
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(Id.createPersonId(event.getVehicleId()));
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d.doubleValue();
					travelTimes.put(event.getLinkId(), Double.valueOf(time));
				}
			}
			if ( event.getLinkId().equals( Id.createLinkId("2") ) ) {
				log.info( event );
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}