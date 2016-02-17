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
import org.matsim.contrib.hybrid.events.ProtobufEvents;
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
		if (e instanceof LinkEnterEvent ) {
			Assert.assertEquals(42.0, e.getTime(), 0.);
			Assert.assertEquals("link123", ((LinkEnterEvent) e).getLinkId().toString());
			Assert.assertEquals("K.I.T.T.",((LinkEnterEvent) e).getVehicleId().toString());
		}
	}

	@Test
	public final void testProtoEvent2EventLinkLeave() {
		ProtobufEvents.LinkLeaveEvent.Builder le = ProtobufEvents.LinkLeaveEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setVehId(ProtobufEvents.VehicleId.newBuilder().setId("K.I.T.T."));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.LinkLeave).
				setLinkLeave(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof LinkLeaveEvent);
		if (e instanceof LinkLeaveEvent ) {
			Assert.assertEquals(42.0, e.getTime(), 0.);
			Assert.assertEquals("link123", ((LinkLeaveEvent) e).getLinkId().toString());
			Assert.assertEquals("K.I.T.T.",((LinkLeaveEvent) e).getVehicleId().toString());
		}
	}

	@Test
	public final void testProtoEvent2EventActivityEnd() {
		ProtobufEvents.ActivityEndEvent.Builder le = ProtobufEvents.ActivityEndEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setPersId(ProtobufEvents.PersonId.newBuilder().setId("Bob"));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.ActivityEnd).
				setActEnd(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof ActivityEndEvent);
		if (e instanceof ActivityEndEvent ) {
			Assert.assertEquals(42.0, e.getTime(), 0.);
			Assert.assertEquals("link123", ((ActivityEndEvent) e).getLinkId().toString());
			Assert.assertEquals("Bob",((ActivityEndEvent) e).getPersonId().toString());
		}
	}

	@Test
	public final void testProtoEvent2EventActivityStart() {
		ProtobufEvents.ActivityStartEvent.Builder le = ProtobufEvents.ActivityStartEvent.newBuilder().setTime(42.0).
				setLinkId(ProtobufEvents.LinkId.newBuilder().setId("link123")).setPersId(ProtobufEvents.PersonId.newBuilder().setId("Bob"));

		ProtobufEvents.Event pe = ProtobufEvents.Event.newBuilder().setType(ProtobufEvents.Event.Type.ActivityStart).
				setActStart(le).build();

		Event e = ProtoEvent2Event.getEvent(pe);
		Assert.assertTrue(e instanceof ActivityStartEvent);
		if (e instanceof ActivityStartEvent ) {
			Assert.assertEquals(42.0, e.getTime(), 0.);
			Assert.assertEquals("link123", ((ActivityStartEvent) e).getLinkId().toString());
			Assert.assertEquals("Bob",((ActivityStartEvent) e).getPersonId().toString());
		}
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
		if (e instanceof PersonArrivalEvent ) {
			Assert.assertEquals(42.0, e.getTime(), 0.);
			Assert.assertEquals("link123", ((PersonArrivalEvent) e).getLinkId().toString());
			Assert.assertEquals("Bob",((PersonArrivalEvent) e).getPersonId().toString());
			Assert.assertEquals("crawling",((PersonArrivalEvent) e).getLegMode());
		}
	}

}
