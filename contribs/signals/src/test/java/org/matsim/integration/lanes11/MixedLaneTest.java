/* *********************************************************************** *
 * project: org.matsim.*
 * MixedLaneTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.integration.lanes11;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public class MixedLaneTest {

	private static final Logger log = Logger.getLogger(MixedLaneTest.class);
	
	private static final double firstLinkOrLaneTT = 1.0 ;
	private static final double linkTTWithoutLanes = 11.0;
	private static final double laneTTlane1ol = 6.0;
	private static final double laneTTlane1 = 6.0;
	
	private MixedLaneTestFixture fixture;

	@Before
	public void initFixture(){
		fixture = new MixedLaneTestFixture();
	}
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Ensures that the capacities of the lanes are correctly calculated by the fixture, i.e. the 
	 * lanes1.1 to lanes2.0 conversion works as expected.
	 * @author dgrether
	 */
	@Test
	public void testFixture(){
		LaneDefinitions20 lanes = this.fixture.sc.getLanes();
		Assert.assertNotNull(lanes);
		Assert.assertNotNull(lanes.getLanesToLinkAssignments());
		LanesToLinkAssignment20 lanesLink1 = lanes.getLanesToLinkAssignments().get(fixture.id1);
		Assert.assertNotNull(lanesLink1);
		Assert.assertEquals(2, lanesLink1.getLanes().size());
		Lane lane1ol = lanesLink1.getLanes().get(fixture.link1FirstLaneId);
		Assert.assertNotNull(lane1ol);
		Assert.assertEquals(100.1, lane1ol.getStartsAtMeterFromLinkEnd(), testUtils.EPSILON);
		Assert.assertEquals(7200.0, lane1ol.getCapacityVehiclesPerHour(), testUtils.EPSILON);
		Assert.assertEquals(2.0 , lane1ol.getNumberOfRepresentedLanes(), testUtils.EPSILON);
		Assert.assertEquals(fixture.laneId1 , lane1ol.getToLaneIds().get(0));
		Assert.assertNull(lane1ol.getToLinkIds());
		Lane lane1 = lanesLink1.getLanes().get(fixture.laneId1);
		Assert.assertNotNull(lane1);
		Assert.assertEquals(50.0, lane1.getStartsAtMeterFromLinkEnd(), testUtils.EPSILON);
		Assert.assertEquals(7200.0, lane1.getCapacityVehiclesPerHour(), testUtils.EPSILON);
		Assert.assertEquals(2.0 , lane1.getNumberOfRepresentedLanes() , testUtils.EPSILON);
		Assert.assertEquals(2 , lane1.getToLinkIds().size());
		Assert.assertNull(lane1.getToLaneIds());
	}

	
	/**
	 * Tests travel times for a single person that departs on a link that has lanes attached.
	 * @author dgrether
	 */
	@Test
	public void test1PersonStartingOnLane(){
		test1PersonStartingOnLane(false);
	}

	
	/**
	 * Tests travel times for a single person that departs on a link that has lanes attached. 
	 * In this test the capacities of the lanes are < 1 veh per second. This can reproduce 
	 * the erroneous behavior reported by Stephan Rath on the users mailinglist at 08.08.2013.
	 * 
	 * @author dgrether
	 */
	@Test
	public void test1PersonsStartingOnLaneCapacityRestriction(){
		test1PersonStartingOnLane(true);
	}
	
	private void test1PersonStartingOnLane(boolean reduceCap) {
		fixture.create1PersonFromLink1Population();
		
		if (reduceCap){
			fixture.sc.getConfig().qsim().setStartTime(3500.0);
			fixture.sc.getConfig().qsim().setEndTime(7200.0);
			LaneDefinitions20 lanes = fixture.sc.getLanes();
			Lane lane1 = lanes.getLanesToLinkAssignments().get(fixture.id1).getLanes().get(fixture.laneId1);
			lane1.setCapacityVehiclesPerHour(1800.0);
			Lane lane1ol = lanes.getLanesToLinkAssignments().get(fixture.id1).getLanes().get(fixture.link1FirstLaneId);
			lane1ol.setCapacityVehiclesPerHour(1800.0);
		}
		
		EventsManager events = EventsUtils.createEventsManager();
		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(this.fixture);
		events.addHandler(handler);
		QSim qsim = QSimUtils.createDefaultQSim(this.fixture.sc, events);
		qsim.run();
		Assert.assertNotNull(handler.lastAgentDepartureEvent);
		Assert.assertEquals(3600.0, 
				handler.lastAgentDepartureEvent.getTime(), testUtils.EPSILON);
	
		Assert.assertNull(handler.lastLink1EnterEvent);
	
		Assert.assertNull(handler.lastLane1olEnterEvent);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT, 
				handler.lastLane1olLeaveEvent.getTime(), testUtils.EPSILON);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT, 
				handler.lastLane1EnterEvent.getTime(), testUtils.EPSILON);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + laneTTlane1, 
				handler.lastLane1LeaveEvent.getTime(), testUtils.EPSILON);
		
		Assert.assertNotNull(handler.lastLink2EnterEvent);
		Assert.assertEquals(this.fixture.pid1, 
				handler.vehId2DriverId.get(handler.lastLink2EnterEvent.getVehicleId()));
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + laneTTlane1, 
				handler.lastLink2EnterEvent.getTime(), testUtils.EPSILON);
	
		Assert.assertNull(handler.lastLink3EnterEvent);
	}


	/**
	 * Tests if one lane with 2 toLinks produces the correct traffic, i.e. one
	 * person arrives on each of the toLinks at a specific time
	 * @author dgrether
	 */
	@Test
	public void testMixedLane2PersonsDriving() {
		testMixedLane2PersonsDriving(false);
	}

	/**
	 * Tests if a capacity restriction works as expected with the setup of the previous test. 
	 * @author dgrether
	 */
	@Test
	public void testMixedLane2AgentsDrivingCapacityRestriction() {
		testMixedLane2PersonsDriving(true);
	}

	
	private void testMixedLane2PersonsDriving(boolean reduceCap) {
		fixture.create2PersonPopulation();
		
		// if no capacity is reduced the delay of both agents should be zero
		double delayOfAgent1 = 0;
		double delayOfAgent2 = 0;
		
		if (reduceCap){
			// reduce capacity on lane 1
			LaneDefinitions20 lanes = fixture.sc.getLanes();
			Lane lane1 = lanes.getLanesToLinkAssignments().
					get(fixture.id1).getLanes().get(fixture.laneId1);
			lane1.setCapacityVehiclesPerHour(1800.0);
			
			// the delay of the second agent on lane 1 should be two seconds if
			// capacity is reduced to 1800 veh/h
			delayOfAgent2 = 2;
		}
		
		EventsManager events = EventsUtils.createEventsManager();
	
		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(this.fixture);
		events.addHandler(handler);
	
		QSim qsim = (QSim) QSimUtils.createDefaultQSim(this.fixture.sc, events);
		qsim.run();
		
		Assert.assertNotNull(handler.lastAgentDepartureEvent);
		Assert.assertEquals(3600.0, handler.lastAgentDepartureEvent.getTime(), testUtils.EPSILON);
	
		Assert.assertNotNull(handler.lastLink1EnterEvent);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT, 
				handler.lastLink1EnterEvent.getTime(), testUtils.EPSILON);
		
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT, 
				handler.lastLane1olEnterEvent.getTime(), testUtils.EPSILON);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + laneTTlane1ol, 
				handler.lastLane1olLeaveEvent.getTime(), testUtils.EPSILON);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + laneTTlane1ol, 
				handler.lastLane1EnterEvent.getTime(), testUtils.EPSILON);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + laneTTlane1ol + laneTTlane1 + delayOfAgent2, 
				handler.lastLane1LeaveEvent.getTime(), testUtils.EPSILON);
	
		Assert.assertNotNull(handler.lastLink2EnterEvent);
		Assert.assertEquals(this.fixture.pid1, handler.vehId2DriverId.get(handler.lastLink2EnterEvent.getVehicleId()));
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + laneTTlane1ol + laneTTlane1 + delayOfAgent2, 
				handler.lastLink2EnterEvent.getTime(), testUtils.EPSILON);
	
		Assert.assertNotNull(handler.lastLink3EnterEvent);
		Assert.assertEquals(this.fixture.pid2, handler.vehId2DriverId.get(handler.lastLink3EnterEvent.getVehicleId()));
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + laneTTlane1ol + laneTTlane1 + delayOfAgent1, 
				handler.lastLink3EnterEvent.getTime(), testUtils.EPSILON);		
	}


	/**
	 * Tests travel times in the same scenario as above but without lanes. 
	 * Note, that they are different in this example!
	 *   
	 * @author thunig
	 */
	@Test
	public void testLink2PersonsDriving() {
		log.info("starting testLink2PersonsDriving()");
		
		fixture.sc.getConfig().qsim().setUseLanes(false);
		
		fixture.create2PersonPopulation();
		EventsManager events = EventsUtils.createEventsManager();
	
		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(fixture);
		events.addHandler(handler);
	
		QSim qsim = (QSim) QSimUtils.createDefaultQSim(fixture.sc, events);
		qsim.run();
		
		Assert.assertNotNull(handler.lastAgentDepartureEvent);
		Assert.assertEquals(3600.0, handler.lastAgentDepartureEvent.getTime(), testUtils.EPSILON);
	
		Assert.assertNotNull(handler.lastLink1EnterEvent);
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT, 
				handler.lastLink1EnterEvent.getTime(), testUtils.EPSILON);
		
		Assert.assertNotNull(handler.lastLink2EnterEvent);
		Assert.assertEquals(fixture.pid1, 
				handler.vehId2DriverId.get(handler.lastLink2EnterEvent.getVehicleId()));
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + linkTTWithoutLanes, 
				handler.lastLink2EnterEvent.getTime(), testUtils.EPSILON);
	
		Assert.assertNotNull(handler.lastLink3EnterEvent);
		Assert.assertEquals(fixture.pid2, 
				handler.vehId2DriverId.get(handler.lastLink3EnterEvent.getVehicleId()));
		Assert.assertEquals(3600.0 + firstLinkOrLaneTT + linkTTWithoutLanes, 
				handler.lastLink3EnterEvent.getTime(), testUtils.EPSILON);
	}



	private static class MixedLanesEventsHandler implements LaneEnterEventHandler, LinkEnterEventHandler, 
		LaneLeaveEventHandler, PersonDepartureEventHandler, Wait2LinkEventHandler {

		LaneEnterEvent lastLane1olEnterEvent = null;
		LaneLeaveEvent lastLane1olLeaveEvent = null;
		LaneEnterEvent lastLane1EnterEvent = null;
		LaneLeaveEvent lastLane1LeaveEvent = null;
		LinkEnterEvent lastLink2EnterEvent = null;
		LinkEnterEvent lastLink3EnterEvent = null;
		LinkEnterEvent lastLink1EnterEvent = null;
		PersonDepartureEvent lastAgentDepartureEvent = null;
		
		Map<Id<Vehicle>, Id<Person>> vehId2DriverId = new HashMap<>();
		
		private MixedLaneTestFixture fixture;

		public MixedLanesEventsHandler(MixedLaneTestFixture fixture) {
			this.fixture = fixture;
		}

		@Override
		public void reset(int iteration) {}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			lastAgentDepartureEvent = event;
		}
		
		@Override
		public void handleEvent(LinkEnterEvent event) {
			
			if (event.getLinkId().equals(this.fixture.id2)){
				lastLink2EnterEvent = event;
				log.info(event);
			}
			else if (event.getLinkId().equals(this.fixture.id3)){
				lastLink3EnterEvent = event;
				log.info(event);
			}
			else if (event.getLinkId().equals(this.fixture.id1)){
				lastLink1EnterEvent = event;
			}
		}

		@Override
		public void handleEvent(LaneEnterEvent event) {
			if (this.fixture.laneId1.equals(event.getLaneId())){
				lastLane1EnterEvent = event;
				log.info(event);
			}
			else if (this.fixture.link1FirstLaneId.equals((event.getLaneId()))){
				lastLane1olEnterEvent = event;
			}
		}

		@Override
		public void handleEvent(LaneLeaveEvent event) {
			if (this.fixture.laneId1.equals(event.getLaneId())){
				lastLane1LeaveEvent = event;
			}
			else if (this.fixture.link1FirstLaneId.equals((event.getLaneId()))){
				lastLane1olLeaveEvent = event;
			}
		}

		@Override
		public void handleEvent(Wait2LinkEvent event) {
			vehId2DriverId.put(event.getVehicleId(), event.getPersonId());
		}


	}
	
}
