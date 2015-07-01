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
package org.matsim.lanes;

import junit.framework.Assert;
import org.jfree.util.Log;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;


public class MixedLaneTest {

	private MixedLaneTestFixture fixture;

	@Before
	public void initFixture(){
		fixture = new MixedLaneTestFixture();
	}

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
		Assert.assertEquals(100.1, lane1ol.getStartsAtMeterFromLinkEnd());
		Assert.assertEquals(7200.0, lane1ol.getCapacityVehiclesPerHour());
		Assert.assertEquals(2.0 , lane1ol.getNumberOfRepresentedLanes() );
		Assert.assertEquals(fixture.laneId1 , lane1ol.getToLaneIds().get(0));
		Assert.assertNull(lane1ol.getToLinkIds());
		Lane lane1 = lanesLink1.getLanes().get(fixture.laneId1);
		Assert.assertNotNull(lane1);
		Assert.assertEquals(50.0, lane1.getStartsAtMeterFromLinkEnd());
		Assert.assertEquals(7200.0, lane1.getCapacityVehiclesPerHour());
		Assert.assertEquals(2.0 , lane1.getNumberOfRepresentedLanes() );
		Assert.assertEquals(2 , lane1.getToLinkIds().size());
		Assert.assertNull(lane1.getToLaneIds());
	}

	
	/**
	 * Tests travel times for a single person that departs on a link that has lanes attached.
	 * @author dgrether
	 */
	@Test
	public void test1PersonStartingOnLane(){
		fixture.create1PersonFromLink1Population();
		
		EventsManager events = EventsUtils.createEventsManager();
		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(this.fixture);
		events.addHandler(handler);
		QSim qsim = QSimUtils.createDefaultQSim(this.fixture.sc, events);
		qsim.run();
		Assert.assertNotNull(handler.agentDepartureEvent);
		Assert.assertEquals(3600.0, handler.agentDepartureEvent.getTime());

		Assert.assertNull(handler.link1EnterEvent);

		Assert.assertNull(handler.lane1olEnterEvent);
		Assert.assertEquals(3600.0 + 1.0, handler.lane1olLeaveEvent.getTime());
		Assert.assertEquals(3600.0 + 1.0, handler.lane1EnterEvent.getTime());
		Assert.assertEquals(3600.0 + 7.0, handler.lane1LeaveEvent.getTime());
		
		Assert.assertNotNull(handler.link2Event);
		Assert.assertEquals(this.fixture.pid1, handler.link2Event.getPersonId());
		Assert.assertEquals(3600.0 + 7.0, handler.link2Event.getTime());

		Assert.assertNull(handler.link3Event);
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
		fixture.create1PersonFromLink1Population();
		fixture.sc.getConfig().qsim().setStartTime(3500.0);
		fixture.sc.getConfig().qsim().setEndTime(7200.0);
		LaneDefinitions20 lanes = fixture.sc.getLanes();
		Lane lane1 = lanes.getLanesToLinkAssignments().get(fixture.id1).getLanes().get(fixture.laneId1);
		lane1.setCapacityVehiclesPerHour(1800.0);
		Lane lane1ol = lanes.getLanesToLinkAssignments().get(fixture.id1).getLanes().get(fixture.link1FirstLaneId);
		lane1ol.setCapacityVehiclesPerHour(1800.0);
		
		
		EventsManager events = EventsUtils.createEventsManager();
		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(this.fixture);
		events.addHandler(handler);
		QSim qsim = (QSim) QSimUtils.createDefaultQSim(this.fixture.sc, events);
		qsim.run();
		Assert.assertNotNull(handler.agentDepartureEvent);
		Assert.assertEquals(3600.0, handler.agentDepartureEvent.getTime());

		Assert.assertNull(handler.link1EnterEvent);

		Assert.assertNull(handler.lane1olEnterEvent);
		Assert.assertNotNull(handler.lane1olLeaveEvent);
		Assert.assertEquals(3600.0 + 1.0, handler.lane1olLeaveEvent.getTime());
		Assert.assertEquals(3600.0 + 1.0, handler.lane1EnterEvent.getTime());
		Assert.assertEquals(3600.0 + 7.0, handler.lane1LeaveEvent.getTime());
		
		Assert.assertNotNull(handler.link2Event);
		Assert.assertEquals(this.fixture.pid1, handler.link2Event.getPersonId());
		Assert.assertEquals(3600.0 + 7.0, handler.link2Event.getTime());

		Assert.assertNull(handler.link3Event);
	}
	
	private static final double linkEnterOffset = 1.0 ;
	private static final double linkLeaveOffset = 12.0 ;
	
	/**
	 * Tests if one lane with 2 toLinks produces the correct traffic, i.e. one
	 * person arrives on each of the toLinks at a specific time
	 * @author dgrether
	 */
	@Test
	public void testMixedLane2PersonsDriving() {
		fixture.create2PersonPopulation();
		EventsManager events = EventsUtils.createEventsManager();

//		events.addHandler(new LogOutputEventHandler());

		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(this.fixture);
		events.addHandler(handler);

		QSim qsim = (QSim) QSimUtils.createDefaultQSim(this.fixture.sc, events);
		qsim.run();
		
		Assert.assertNotNull(handler.agentDepartureEvent);
		Assert.assertEquals(3600.0, handler.agentDepartureEvent.getTime());

		Assert.assertNotNull(handler.link1EnterEvent);
		Assert.assertEquals(3600.0 + linkEnterOffset, handler.link1EnterEvent.getTime());
		
		// (*)

		Assert.assertNotNull(handler.link2Event);
		Assert.assertEquals(this.fixture.pid1, handler.link2Event.getPersonId());
		Assert.assertEquals(3600.0 + linkLeaveOffset, handler.link2Event.getTime());

		Assert.assertNotNull(handler.link3Event);
		Assert.assertEquals(this.fixture.pid2, handler.link3Event.getPersonId());
		Assert.assertEquals(3600.0 + linkLeaveOffset, handler.link3Event.getTime());

		// the following comes chronologically at (*) but was moved here so that it can fail first on
		// the link leave values (to test equivalency with link only dynamics, see below)
		Assert.assertEquals(3600.0 + linkEnterOffset, handler.lane1olEnterEvent.getTime());
		Assert.assertEquals(3600.0 + 6.0, handler.lane1olLeaveEvent.getTime());
		Assert.assertEquals(3600.0 + 6.0, handler.lane1EnterEvent.getTime());
		Assert.assertEquals(3600.0 + linkLeaveOffset, handler.lane1LeaveEvent.getTime());
		
}

	/**
	 * Tests if a regular link generates the same output as the lane test above.
	 *   
	 * @author nagel,thunig
	 */
	@Test
	public void testLink2PersonsDriving() {
		Log.info("starting testLink2PersonsDriving()");
		
		@SuppressWarnings("hiding")
		MixedLaneTestFixture fixture = new MixedLaneTestFixture(false, 1.0);
		
		fixture.create2PersonPopulation();
		EventsManager events = EventsUtils.createEventsManager();

//		events.addHandler(new LogOutputEventHandler());

		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(fixture);
		events.addHandler(handler);

		QSim qsim = (QSim) QSimUtils.createDefaultQSim(fixture.sc, events);
		qsim.run();
		
		Assert.assertNotNull(handler.agentDepartureEvent);
		Assert.assertEquals(3600.0, handler.agentDepartureEvent.getTime());

		Assert.assertNotNull(handler.link1EnterEvent);
		Assert.assertEquals(3600.0 + linkEnterOffset, handler.link1EnterEvent.getTime());

//		Assert.assertEquals(3600.0 + linkEnterOffset, handler.lane1olEnterEvent.getTime());
//		Assert.assertEquals(3600.0 + 6.0, handler.lane1olLeaveEvent.getTime());
//		Assert.assertEquals(3600.0 + 6.0, handler.lane1EnterEvent.getTime());
//		Assert.assertEquals(3600.0 + linkLeaveOffset, handler.lane1LeaveEvent.getTime());
		
		Assert.assertNotNull(handler.link2Event);
		Assert.assertEquals(fixture.pid1, handler.link2Event.getPersonId());
		Assert.assertEquals(3600.0 + linkLeaveOffset, handler.link2Event.getTime());

		Assert.assertNotNull(handler.link3Event);
		Assert.assertEquals(fixture.pid2, handler.link3Event.getPersonId());
		Assert.assertEquals(3600.0 + linkLeaveOffset, handler.link3Event.getTime());
	}


	/**
	 * Tests if a capacity restriction works as expected with the setup of the previous test. 
	 * @author dgrether
	 */
	@Test
	public void testMixedLane2AgentsDrivingCapacityRestriction() {
		fixture.create2PersonPopulation();
		LaneDefinitions20 lanes = fixture.sc.getLanes();
		Lane lane1 = lanes.getLanesToLinkAssignments().get(fixture.id1).getLanes().get(fixture.laneId1);
		lane1.setCapacityVehiclesPerHour(1800.0);
		
		EventsManager events = EventsUtils.createEventsManager();

		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(this.fixture);
		events.addHandler(handler);

		QSim qsim = (QSim) QSimUtils.createDefaultQSim(this.fixture.sc, events);
		qsim.run();
		
		Assert.assertNotNull(handler.agentDepartureEvent);
		Assert.assertEquals(3600.0, handler.agentDepartureEvent.getTime());

		Assert.assertNotNull(handler.link1EnterEvent);
		Assert.assertEquals(3600.0 + 1.0, handler.link1EnterEvent.getTime());

		Assert.assertEquals(3600.0 + 1.0, handler.lane1olEnterEvent.getTime());
		Assert.assertEquals(3600.0 + 6.0, handler.lane1olLeaveEvent.getTime());
		Assert.assertEquals(3600.0 + 6.0, handler.lane1EnterEvent.getTime());
		Assert.assertEquals(3600.0 + 14.0, handler.lane1LeaveEvent.getTime());
		
		Assert.assertNotNull(handler.link2Event);
		Assert.assertEquals(this.fixture.pid1, handler.link2Event.getPersonId());
		Assert.assertEquals(3600.0 + 14.0, handler.link2Event.getTime());

		Assert.assertNotNull(handler.link3Event);
		Assert.assertEquals(this.fixture.pid2, handler.link3Event.getPersonId());
		Assert.assertEquals(3600.0 + 12.0, handler.link3Event.getTime());
	}

	
	
	private static class MixedLanesEventsHandler implements LaneEnterEventHandler, LinkEnterEventHandler, 
		LaneLeaveEventHandler, PersonDepartureEventHandler {

		LaneEnterEvent lane1olEnterEvent = null;
		LaneLeaveEvent lane1olLeaveEvent = null;
		LaneEnterEvent lane1EnterEvent = null;
		LaneLeaveEvent lane1LeaveEvent = null;
		LinkEnterEvent link2Event = null;
		LinkEnterEvent link3Event = null;
		LinkEnterEvent link1EnterEvent = null;
		PersonDepartureEvent agentDepartureEvent = null;
		private MixedLaneTestFixture fixture;

		public MixedLanesEventsHandler(MixedLaneTestFixture fixture) {
			this.fixture = fixture;
		}

		@Override
		public void reset(int iteration) {}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			agentDepartureEvent = event;
		}
		
		@Override
		public void handleEvent(LinkEnterEvent event) {
			
			if (event.getLinkId().equals(this.fixture.id2)){
				link2Event = event;
			}
			else if (event.getLinkId().equals(this.fixture.id3)){
				link3Event = event;
			}
			else if (event.getLinkId().equals(this.fixture.id1)){
				link1EnterEvent = event;
			}
		}

		@Override
		public void handleEvent(LaneEnterEvent event) {
			if (this.fixture.laneId1.equals(event.getLaneId())){
				lane1EnterEvent = event;
				System.err.println(event);
			}
			else if (this.fixture.link1FirstLaneId.equals((event.getLaneId()))){
				lane1olEnterEvent = event;
			}
		}

		@Override
		public void handleEvent(LaneLeaveEvent event) {
			if (this.fixture.laneId1.equals(event.getLaneId())){
				lane1LeaveEvent = event;
			}
			else if (this.fixture.link1FirstLaneId.equals((event.getLaneId()))){
				lane1olLeaveEvent = event;
			}
		}


	}
	
}
