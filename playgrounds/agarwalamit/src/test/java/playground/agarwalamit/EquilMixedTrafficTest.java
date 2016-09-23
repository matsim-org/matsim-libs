/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * Just setting up the equil scenario for mixed traffic conditions.
 * 
 * @author amit
 */

public class EquilMixedTrafficTest {
	
	private static final String EQUIL_DIR = "../../matsim/examples/equil-mixedTraffic/";
	
	@Test
	public void run() {
		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters 
		
		Config config = ConfigUtils.loadConfig(EQUIL_DIR+"/config.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		Controler controler = new Controler(scenario);

		final PersonLinkTravelTimeEventHandler handler = new PersonLinkTravelTimeEventHandler();

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addTravelTimeBinding("bicycle").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());

				// add event handler to test...
				addEventHandlerBinding().toInstance(handler);
			}
		});
		
		controler.run();

		final Map<Id<Vehicle>,Map<Id<Link>,Double>> vehicle2link2enterTime = handler.getVehicleId2LinkEnterTime();
		final Map<Id<Vehicle>,Map<Id<Link>,Double>> vehicle2link2leaveTime = handler.getVehicleId2LinkLeaveTime();

		Id<Vehicle> bikeVeh = Id.createVehicleId(9);
		Id<Vehicle> carVeh = Id.createVehicleId(2);

		Id<Link> link2 = Id.createLinkId(2);
		Id<Link> link22 = Id.createLinkId(22);

		Assert.assertEquals("Wrong travel time of agent 9 on link 2",  Math.floor(10000/4.17)+1.0, vehicle2link2leaveTime.get(bikeVeh).get(link2) - vehicle2link2enterTime.get(bikeVeh).get(link2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time of agent 2 on link 2",  Math.floor(10000/16.677)+1.0, vehicle2link2leaveTime.get(carVeh).get(link2) - vehicle2link2enterTime.get(carVeh).get(link2), MatsimTestUtils.EPSILON);

		// passing happens on link 22 (35000 m); t_free_car = 35000 / 16.67 = 2100; t_free_car = 35000/4.17 = 8394
		double bikeTT = vehicle2link2leaveTime.get(bikeVeh).get(link22) - vehicle2link2enterTime.get(bikeVeh).get(link22);
		double carTT = vehicle2link2leaveTime.get(carVeh).get(link22) - vehicle2link2enterTime.get(carVeh).get(link22);

		Assert.assertTrue("Car did not enter after bike", vehicle2link2enterTime.get(carVeh).get(link22) > vehicle2link2enterTime.get(bikeVeh).get(link22));
		Assert.assertTrue("Car did not leave before bike", vehicle2link2leaveTime.get(carVeh).get(link22) < vehicle2link2leaveTime.get(bikeVeh).get(link22));

		Assert.assertEquals("Wrong travel time on link 22",2100.0,carTT, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time on link 22",8394, bikeTT, MatsimTestUtils.EPSILON);
	}

	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkLeaveTimes =  new HashMap<>();
		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkEnterTimes =  new HashMap<>();

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> leaveTime = this.vehicleLinkLeaveTimes.get(event.getVehicleId());
			if (leaveTime == null) {
				leaveTime = new HashMap<>();
				this.vehicleLinkLeaveTimes.put( event.getVehicleId() , leaveTime);
			}
			leaveTime.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> enterTime = this.vehicleLinkEnterTimes.get(event.getVehicleId());
			if (enterTime == null) {
				enterTime = new HashMap<>();
				this.vehicleLinkEnterTimes.put( event.getVehicleId() , enterTime);
			}
			enterTime.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void reset(int iteration) {
			this.vehicleLinkEnterTimes.clear();
			this.vehicleLinkLeaveTimes.clear();
		}

		public Map<Id<Vehicle>, Map<Id<Link>, Double>> getVehicleId2LinkEnterTime(){
			return this.vehicleLinkEnterTimes;
		}

		public Map<Id<Vehicle>, Map<Id<Link>, Double>> getVehicleId2LinkLeaveTime(){
			return this.vehicleLinkLeaveTimes;
		}
	}
}
