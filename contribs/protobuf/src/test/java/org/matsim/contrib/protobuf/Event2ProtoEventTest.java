package org.matsim.contrib.protobuf;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.contrib.protobuf.events.ProtobufEvents;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Created by laemmel on 16/02/16.
 */
public class Event2ProtoEventTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testEvent2ProtoEventActivityEndEvent() {

		ActivityEndEvent e = new ActivityEndEvent(42.0, Id.createPersonId("Alice"),
				Id.createLinkId("link123"), Id.create(55, ActivityFacility.class), "sleep");
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.ActivityEnd);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == true);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getActEnd().getTime(),0.);
		Assert.assertEquals("Alice",pbe.getActEnd().getPersId().getId());
		Assert.assertEquals("link123",pbe.getActEnd().getLinkId().getId());
		Assert.assertEquals("55",pbe.getActEnd().getFacilityId().getId());
		Assert.assertEquals("sleep",pbe.getActEnd().getActType());
	}

	@Test
	public final void testEvent2ProtoEventActivityStartEvent() {

		ActivityStartEvent e = new ActivityStartEvent(42.0, Id.createPersonId("Bob"),
				Id.createLinkId("link123"), Id.create(55, ActivityFacility.class), "sleep");
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.ActivityStart);
		Assert.assertTrue(pbe.hasActStart() == true);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getActStart().getTime(),0.);
		Assert.assertEquals("Bob",pbe.getActStart().getPersId().getId());
		Assert.assertEquals("link123",pbe.getActStart().getLinkId().getId());
		Assert.assertEquals("55",pbe.getActStart().getFacilityId().getId());
		Assert.assertEquals("sleep",pbe.getActStart().getActType());
	}

	@Test
	public final void testEvent2ProtoEventLinkEnterEvent() {

		LinkEnterEvent e = new LinkEnterEvent(42.0,Id.createVehicleId("K.I.T.T."),Id.createLinkId("link123"));
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.LinkEnter);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == true);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getLinkEnter().getTime(),0.);
		Assert.assertEquals("K.I.T.T.",pbe.getLinkEnter().getVehId().getId());
		Assert.assertEquals("link123",pbe.getLinkEnter().getLinkId().getId());
	}

	@Test
	public final void testEvent2ProtoEventLinkLeaveEvent() {

		LinkLeaveEvent e = new LinkLeaveEvent(42.0,Id.createVehicleId("K.I.T.T."),Id.createLinkId("link123"));
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.LinkLeave);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == true);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getLinkLeave().getTime(),0.);
		Assert.assertEquals("K.I.T.T.",pbe.getLinkLeave().getVehId().getId());
		Assert.assertEquals("link123",pbe.getLinkLeave().getLinkId().getId());
	}

	@Test
	public final void testEvent2ProtoEventPersonArriaveEvent() {

		PersonArrivalEvent e = new PersonArrivalEvent(42.0,Id.createPersonId("Alice"),Id.createLinkId("link123"),"swim");
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.PersonArrival);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == true);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getPersonArrival().getTime(),0.);
		Assert.assertEquals("Alice",pbe.getPersonArrival().getPersId().getId());
		Assert.assertEquals("link123",pbe.getPersonArrival().getLinkId().getId());
	}

	@Test
	public final void testEvent2ProtoEventPersonDepartureEvent() {

		PersonDepartureEvent e = new PersonDepartureEvent(42.0,Id.createPersonId("Bob"),Id.createLinkId("link123"),"swim");
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.PersonDeparture);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == true);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getPersonDeparture().getTime(),0.);
		Assert.assertEquals("Bob",pbe.getPersonDeparture().getPersId().getId());
		Assert.assertEquals("link123",pbe.getPersonDeparture().getLinkId().getId());
	}

	@Test
	public final void testEvent2ProtoEventPersonEntersVehicleEvent() {

		PersonEntersVehicleEvent e = new PersonEntersVehicleEvent(42.,Id.createPersonId("Alice"),Id.createVehicleId("K.I.T.T."));
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.PersonEntersVehicle);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == true);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getPersonEntersVehicle().getTime(),0.);
		Assert.assertEquals("Alice",pbe.getPersonEntersVehicle().getPersId().getId());
		Assert.assertEquals("K.I.T.T.",pbe.getPersonEntersVehicle().getVehId().getId());
	}

	@Test
	public final void testEvent2ProtoEventPersonLeavesVehicleEvent() {

		PersonLeavesVehicleEvent e = new PersonLeavesVehicleEvent(42.,Id.createPersonId("Alice"),Id.createVehicleId("K.I.T.T."));
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.PersonLeavesVehicle);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == true);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getPersonLeavesVehicle().getTime(),0.);
		Assert.assertEquals("Alice",pbe.getPersonLeavesVehicle().getPersId().getId());
		Assert.assertEquals("K.I.T.T.",pbe.getPersonLeavesVehicle().getVehId().getId());
	}

	@Test
	public final void testEvent2ProtoEventPersonMoneyEvent() {

		PersonMoneyEvent e = new PersonMoneyEvent(42.0,Id.createPersonId("Bob"),-123.45);
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.PersonMoney);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == true);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getPersonMoney().getTime(),0.);
		Assert.assertEquals("Bob",pbe.getPersonMoney().getPersId().getId());
		Assert.assertEquals(-123.45,pbe.getPersonMoney().getAmount(),0);
	}

	@Test
	public final void testEvent2ProtoEventPersonStuckEvent() {

		PersonStuckEvent e = new PersonStuckEvent(42.0,Id.createPersonId("Alice"), Id.createLinkId("link123"),"fly");
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.PersonStuck);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == true);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getPersonStuck().getTime(),0.);
		Assert.assertEquals("Alice",pbe.getPersonStuck().getPersId().getId());
		Assert.assertEquals("link123", pbe.getPersonStuck().getLinkId().getId());
		Assert.assertEquals("fly",pbe.getPersonStuck().getLegMode());
	}

	@Test
	public final void testEvent2ProtoEventTransitDricerStartsEvent() {

		TransitDriverStartsEvent e = new TransitDriverStartsEvent(42.0,Id.createPersonId("Bob"),Id.createVehicleId("K.I.T.T."),
				Id.create("l11", TransitLine.class),Id.create("r11", TransitRoute.class),Id.create("d11", Departure.class));
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.TransitDriverStarts);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == true);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getTransitDriverStarts().getTime(),0.);
		Assert.assertEquals("Bob",pbe.getTransitDriverStarts().getDriverId().getId());
		Assert.assertEquals("K.I.T.T.",pbe.getTransitDriverStarts().getVehId().getId());
		Assert.assertEquals("l11",pbe.getTransitDriverStarts().getTransitLineId().getId());
		Assert.assertEquals("r11",pbe.getTransitDriverStarts().getTransitRouteId().getId());
		Assert.assertEquals("d11",pbe.getTransitDriverStarts().getDepartureId().getId());
	}

	@Test
	public final void testEvent2ProtoEventVehicleAbortsEvent() {

		VehicleAbortsEvent e = new VehicleAbortsEvent(42.0,Id.createVehicleId("K.I.T.T."),Id.createLinkId("link123"));
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.VehicleAborts);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == true);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getVehicleAborts().getTime(),0.);
		Assert.assertEquals("K.I.T.T.",pbe.getVehicleAborts().getVehId().getId());
		Assert.assertEquals("link123",pbe.getVehicleAborts().getLinkId().getId());

	}

	@Test
	public final void testEvent2ProtoEventVehicleEntersTrafficEvent() {

		VehicleEntersTrafficEvent e = new VehicleEntersTrafficEvent(42.0,Id.createPersonId("Alice"),Id.createLinkId("link123"),
				Id.createVehicleId("K.I.T.T."),"super pursuit",111.0);
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.VehicleEntersTraffic);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == true);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == false);

		Assert.assertEquals(42.0,pbe.getVehicleEntersTraffic().getTime(),0.);
		Assert.assertEquals("Alice",pbe.getVehicleEntersTraffic().getDriverId().getId());
		Assert.assertEquals("link123",pbe.getVehicleEntersTraffic().getLinkId().getId());
		Assert.assertEquals("K.I.T.T.",pbe.getVehicleEntersTraffic().getVehId().getId());
		Assert.assertEquals("super pursuit",pbe.getVehicleEntersTraffic().getNetworkMode());
		Assert.assertEquals(111.0,pbe.getVehicleEntersTraffic().getRelPosOnLink(),0);

	}

	@Test
	public final void testEvent2ProtoEventVehicleLeavesTrafficEvent() {

		VehicleLeavesTrafficEvent e = new VehicleLeavesTrafficEvent(42.0,Id.createPersonId("Alice"),Id.createLinkId("link123"),
				Id.createVehicleId("K.I.T.T."),"super pursuit",111.0);
		ProtobufEvents.Event pbe = Event2ProtoEvent.getProtoEvent(e);
		Assert.assertTrue(pbe.getType() == ProtobufEvents.Event.Type.VehicleLeavesTraffic);
		Assert.assertTrue(pbe.hasActStart() == false);
		Assert.assertTrue(pbe.hasActEnd() == false);
		Assert.assertTrue(pbe.hasLinkEnter() == false);
		Assert.assertTrue(pbe.hasLinkLeave() == false);
		Assert.assertTrue(pbe.hasPersonArrival() == false);
		Assert.assertTrue(pbe.hasPersonDeparture() == false);
		Assert.assertTrue(pbe.hasPersonEntersVehicle() == false);
		Assert.assertTrue(pbe.hasPersonLeavesVehicle() == false);
		Assert.assertTrue(pbe.hasPersonMoney() == false);
		Assert.assertTrue(pbe.hasPersonStuck() == false);
		Assert.assertTrue(pbe.hasTransitDriverStarts() == false);
		Assert.assertTrue(pbe.hasVehicleAborts() == false);
		Assert.assertTrue(pbe.hasVehicleEntersTraffic() == false);
		Assert.assertTrue(pbe.hasVehicleLeavesTraffic() == true);

		Assert.assertEquals(42.0,pbe.getVehicleLeavesTraffic().getTime(),0.);
		Assert.assertEquals("Alice",pbe.getVehicleLeavesTraffic().getDriverId().getId());
		Assert.assertEquals("link123",pbe.getVehicleLeavesTraffic().getLinkId().getId());
		Assert.assertEquals("K.I.T.T.",pbe.getVehicleLeavesTraffic().getVehId().getId());
		Assert.assertEquals("super pursuit",pbe.getVehicleLeavesTraffic().getNetworkMode());
		Assert.assertEquals(111.0,pbe.getVehicleLeavesTraffic().getRelPosOnLink(),0);

	}
}
