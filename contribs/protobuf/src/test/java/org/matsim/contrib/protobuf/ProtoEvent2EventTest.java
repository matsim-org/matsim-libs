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
import org.matsim.api.core.v01.events.*;
import org.matsim.contrib.protobuf.events.ProtobufEvents;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Created by laemmel on 17/02/16.
 */
public class ProtoEvent2EventTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testProtoEvent2EventLinkEnter() {
		ProtobufEvents.LinkEnterEvent.Builder le = ProtobufEvents.LinkEnterEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T."));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.LinkEnter).
				setLinkEnter(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof LinkEnterEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("link123", ((LinkEnterEvent) e).getLinkId().toString());
		Assert.assertEquals("K.I.T.T.", ((LinkEnterEvent) e).getVehicleId().toString());
	}

	@Test
	public final void testProtoEvent2EventLinkLeave() {
		ProtobufEvents.LinkLeaveEvent.Builder le = ProtobufEvents.LinkLeaveEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T."));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.LinkLeave).
				setLinkLeave(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof LinkLeaveEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("link123", ((LinkLeaveEvent) e).getLinkId().toString());
		Assert.assertEquals("K.I.T.T.", ((LinkLeaveEvent) e).getVehicleId().toString());
	}

	@Test
	public final void testProtoEvent2EventActivityEnd() {
		ProtobufEvents.ActivityEndEvent.Builder le = ProtobufEvents.ActivityEndEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setPersId(ProtobufEvents.PersonId.newBuilder().setId("Bob"));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.ActivityEnd).
				setActEnd(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof ActivityEndEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("link123", ((ActivityEndEvent) e).getLinkId().toString());
		Assert.assertEquals("Bob", ((ActivityEndEvent) e).getPersonId().toString());
	}

	@Test
	public final void testProtoEvent2EventActivityStart() {
		ProtobufEvents.ActivityStartEvent.Builder le = ProtobufEvents.ActivityStartEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setPersId(ProtobufEvents.PersonId.newBuilder().setId("Bob"));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.ActivityStart).
				setActStart(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof ActivityStartEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("link123", ((ActivityStartEvent) e).getLinkId().toString());
		Assert.assertEquals("Bob", ((ActivityStartEvent) e).getPersonId().toString());
	}

	@Test
	public final void testProtoEvent2EventPersonArrival() {
		ProtobufEvents.PersonArrivalEvent.Builder le = ProtobufEvents.PersonArrivalEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setPersId(ProtobufEvents.PersonId.newBuilder().
				setId("Bob")).setLegMode("crawling");

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.PersonArrival).
				setPersonArrival(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof PersonArrivalEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("link123", ((PersonArrivalEvent) e).getLinkId().toString());
		Assert.assertEquals("Bob", ((PersonArrivalEvent) e).getPersonId().toString());
		Assert.assertEquals("crawling", ((PersonArrivalEvent) e).getLegMode());
	}

	@Test
	public final void testProtoEvent2EventPersonDeparture() {
		ProtobufEvents.PersonDepartureEvent.Builder le = ProtobufEvents.PersonDepartureEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setPersId(ProtobufEvents.PersonId.newBuilder().
				setId("Alice")).setLegMode("crawling");

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.PersonDeparture).
				setPersonDeparture(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof PersonDepartureEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("link123", ((PersonDepartureEvent) e).getLinkId().toString());
		Assert.assertEquals("Alice", ((PersonDepartureEvent) e).getPersonId().toString());
		Assert.assertEquals("crawling", ((PersonDepartureEvent) e).getLegMode());
	}

	@Test
	public final void testProtoEvent2EventPersonEntersVehicle() {
		ProtobufEvents.PersonEntersVehicleEvent.Builder le = ProtobufEvents.PersonEntersVehicleEvent.newBuilder().setTime(42.0).
				setPersId(ProtobufEvents.PersonId.newBuilder().setId("Alice")).setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T."));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.PersonEntersVehicle).
				setPersonEntersVehicle(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof PersonEntersVehicleEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("Alice", ((PersonEntersVehicleEvent) e).getPersonId().toString());
		Assert.assertEquals("K.I.T.T.", ((PersonEntersVehicleEvent) e).getVehicleId().toString());
	}

	@Test
	public final void testProtoEvent2EventPersonLeavesVehicle() {
		ProtobufEvents.PersonLeavesVehicleEvent.Builder le = ProtobufEvents.PersonLeavesVehicleEvent.newBuilder().setTime(42.0).
				setPersId(ProtobufEvents.PersonId.newBuilder().setId("Alice")).setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T."));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.PersonLeavesVehicle).
				setPersonLeavesVehicle(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof PersonLeavesVehicleEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("Alice", ((PersonLeavesVehicleEvent) e).getPersonId().toString());
		Assert.assertEquals("K.I.T.T.", ((PersonLeavesVehicleEvent) e).getVehicleId().toString());
	}

	@Test
	public final void testProtoEvent2EventPersonMoney() {
		ProtobufEvents.PersonMoneyEvent.Builder le = ProtobufEvents.PersonMoneyEvent.newBuilder().setTime(42.0).
				setPersId(ProtobufEvents.PersonId.newBuilder().setId("Alice")).setAmount(-123.45);

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.PersonMoney).
				setPersonMoney(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof PersonMoneyEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("Alice", ((PersonMoneyEvent) e).getPersonId().toString());
		Assert.assertEquals(-123.45, ((PersonMoneyEvent) e).getAmount(), 0.);
	}

	@Test
	public final void testProtoEvent2EventPersonStuck() {
		ProtobufEvents.PersonStuckEvent.Builder le = ProtobufEvents.PersonStuckEvent.newBuilder().setTime(42.0).
				setPersId(ProtobufEvents.PersonId.newBuilder().setId("Alice")).setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).
				setLegMode("flying");


		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.PersonStuck).
				setPersonStuck(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof PersonStuckEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("Alice", ((PersonStuckEvent) e).getPersonId().toString());
		Assert.assertEquals("link123", ((PersonStuckEvent) e).getLinkId().toString());
		Assert.assertEquals("flying", ((PersonStuckEvent) e).getLegMode());
	}

	@Test
	public final void testProtoEvent2EventTransitDriverStarts() {
		ProtobufEvents.TransitDriverStartsEvent.Builder le = ProtobufEvents.TransitDriverStartsEvent.newBuilder().setTime(42.0).
				setDriverId(ProtobufEvents.PersonId.newBuilder().setId("Alice")).setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T.")).
				setTransitLineId(ProtobufEvents.TransitLineId.newBuilder().setId("tl11")).
				setTransitRouteId(ProtobufEvents.TransitRouteId.newBuilder().setId("tr11")).
				setDepartureId(ProtobufEvents.DepartureId.newBuilder().setId("d11"));


		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.TransitDriverStarts).
				setTransitDriverStarts(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof TransitDriverStartsEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("Alice", ((TransitDriverStartsEvent) e).getDriverId().toString());
		Assert.assertEquals("K.I.T.T.", ((TransitDriverStartsEvent) e).getVehicleId().toString());
		Assert.assertEquals("tl11", ((TransitDriverStartsEvent) e).getTransitLineId().toString());
		Assert.assertEquals("tr11", ((TransitDriverStartsEvent) e).getTransitRouteId().toString());
		Assert.assertEquals("d11", ((TransitDriverStartsEvent) e).getDepartureId().toString());
	}

	@Test
	public final void testProtoEvent2EventVehicleAborts() {
		ProtobufEvents.VehicleAbortsEvent.Builder le = ProtobufEvents.VehicleAbortsEvent.newBuilder().setTime(42.0).
				setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T.")).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123"));


		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.VehicleAborts).
				setVehicleAborts(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof VehicleAbortsEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("K.I.T.T.", ((VehicleAbortsEvent) e).getVehicleId().toString());
		Assert.assertEquals("link123", ((VehicleAbortsEvent) e).getLinkId().toString());
	}

	@Test
	public final void testProtoEvent2EventVehicleEntersTraffic() {
		ProtobufEvents.VehicleEntersTrafficEvent.Builder le = ProtobufEvents.VehicleEntersTrafficEvent.newBuilder().setTime(42.0).
				setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T.")).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setDriverId(ProtobufEvents.PersonId.newBuilder().setId("Alice")).
				setNetworkMode("super pursuit").setRelPosOnLink(3.1415);


		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.VehicleEntersTraffic).
				setVehicleEntersTraffic(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof VehicleEntersTrafficEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("K.I.T.T.", ((VehicleEntersTrafficEvent) e).getVehicleId().toString());
		Assert.assertEquals("link123", ((VehicleEntersTrafficEvent) e).getLinkId().toString());
		Assert.assertEquals("Alice", ((VehicleEntersTrafficEvent) e).getPersonId().toString());
		Assert.assertEquals("super pursuit", ((VehicleEntersTrafficEvent) e).getNetworkMode());
		Assert.assertEquals(3.1415, ((VehicleEntersTrafficEvent) e).getRelativePositionOnLink(), 0.);
	}

	@Test
	public final void testProtoEvent2EventVehicleLeavesTraffic() {
		ProtobufEvents.VehicleLeavesTrafficEvent.Builder le = ProtobufEvents.VehicleLeavesTrafficEvent.newBuilder().setTime(42.0).
				setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T.")).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setDriverId(ProtobufEvents.PersonId.newBuilder().setId("Alice")).
				setNetworkMode("super pursuit").setRelPosOnLink(3.1415);


		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.VehicleLeavesTraffic).
				setVehicleLeavesTraffic(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof VehicleLeavesTrafficEvent);
		Assert.assertEquals(42.0, e.getTime(), 0.);
		Assert.assertEquals("K.I.T.T.", ((VehicleLeavesTrafficEvent) e).getVehicleId().toString());
		Assert.assertEquals("link123", ((VehicleLeavesTrafficEvent) e).getLinkId().toString());
		Assert.assertEquals("Alice", ((VehicleLeavesTrafficEvent) e).getPersonId().toString());
		Assert.assertEquals("super pursuit", ((VehicleLeavesTrafficEvent) e).getNetworkMode());
		Assert.assertEquals(3.1415, ((VehicleLeavesTrafficEvent) e).getRelativePositionOnLink(), 0.);
	}
}
