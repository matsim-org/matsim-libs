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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.*;
import org.matsim.contrib.protobuf.events.ProtobufEvents;
import org.matsim.core.gbl.Gbl;

import java.util.Map;

/**
 * Created by laemmel on 16/02/16.
 */
public abstract class Event2ProtoEvent {

	private static final Logger log = Logger.getLogger(Event2ProtoEvent.class);
	private static boolean REPORT_GENERIC_EVENT = true;

	static ProtobufEvents.Event getProtoEvent(Event event) {
		ProtobufEvents.Event.Builder eb = ProtobufEvents.Event.newBuilder();

		if (event instanceof LinkLeaveEvent) {
			ProtobufEvents.LinkLeaveEvent.Builder ll = ProtobufEvents.LinkLeaveEvent.newBuilder()
					.setTime(event.getTime())
					.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((LinkLeaveEvent) event).getLinkId().toString()))
					.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((LinkLeaveEvent) event).getVehicleId().toString()));
			eb.setType(ProtobufEvents.Event.Type.LinkLeave).setLinkLeave(ll);
		}
		else {
			if (event instanceof LinkEnterEvent) {
				ProtobufEvents.LinkEnterEvent.Builder ll = ProtobufEvents.LinkEnterEvent.newBuilder()
						.setTime(event.getTime())
						.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((LinkEnterEvent) event).getLinkId().toString()))
						.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((LinkEnterEvent) event).getVehicleId().toString()));
				eb.setType(ProtobufEvents.Event.Type.LinkEnter).setLinkEnter(ll);
			}
			else {
				if (event instanceof ActivityEndEvent) {
					ProtobufEvents.ActivityEndEvent.Builder ae = ProtobufEvents.ActivityEndEvent.newBuilder()
							.setTime(event.getTime())
							.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((ActivityEndEvent) event).getLinkId().toString()))
							.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((ActivityEndEvent) event).getPersonId().toString()))
							.setActType(((ActivityEndEvent) event).getActType());
					if (((ActivityEndEvent) event).getFacilityId() != null)  ae.setFacilityId(ProtobufEvents.ActivityFacilityId.newBuilder().setId(((ActivityEndEvent) event).getFacilityId().toString()));
					eb.setType(ProtobufEvents.Event.Type.ActivityEnd).setActEnd(ae);

				}
				else {
					if (event instanceof ActivityStartEvent) {
						ProtobufEvents.ActivityStartEvent.Builder as = ProtobufEvents.ActivityStartEvent.newBuilder()
								.setTime(event.getTime())
								.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((ActivityStartEvent) event).getLinkId().toString()))
								.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((ActivityStartEvent) event).getPersonId().toString()))
								.setActType(((ActivityStartEvent) event).getActType());
						if (((ActivityStartEvent) event).getFacilityId() != null ) {
							as.setFacilityId(ProtobufEvents.ActivityFacilityId.newBuilder().setId(((ActivityStartEvent) event).getFacilityId().toString()));
						}
						eb.setType(ProtobufEvents.Event.Type.ActivityStart).setActStart(as);
					}
					else {
						if (event instanceof PersonArrivalEvent) {
							ProtobufEvents.PersonArrivalEvent.Builder pa = ProtobufEvents.PersonArrivalEvent.newBuilder()
									.setTime(event.getTime())
									.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((PersonArrivalEvent) event).getLinkId().toString()))
									.setLegMode(((PersonArrivalEvent) event).getLegMode())
									.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((PersonArrivalEvent) event).getPersonId().toString()));
							eb.setType(ProtobufEvents.Event.Type.PersonArrival).setPersonArrival(pa);
						}
						else {
							if (event instanceof PersonDepartureEvent) {
								ProtobufEvents.PersonDepartureEvent.Builder pd = ProtobufEvents.PersonDepartureEvent.newBuilder()
										.setTime(event.getTime())
										.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((PersonDepartureEvent) event).getLinkId().toString()))
										.setLegMode(((PersonDepartureEvent) event).getLegMode())
										.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((PersonDepartureEvent) event).getPersonId().toString()));
								eb.setType(ProtobufEvents.Event.Type.PersonDeparture).setPersonDeparture(pd);
							}
							else {
								if (event instanceof PersonEntersVehicleEvent) {
									ProtobufEvents.PersonEntersVehicleEvent.Builder pe = ProtobufEvents.PersonEntersVehicleEvent.newBuilder()
											.setTime(event.getTime())
											.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((PersonEntersVehicleEvent) event).getPersonId().toString()))
											.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((PersonEntersVehicleEvent) event).getVehicleId().toString()));
									eb.setType(ProtobufEvents.Event.Type.PersonEntersVehicle).setPersonEntersVehicle(pe);
								}
								else {
									if (event instanceof PersonLeavesVehicleEvent) {
										ProtobufEvents.PersonLeavesVehicleEvent.Builder pl = ProtobufEvents.PersonLeavesVehicleEvent.newBuilder()
												.setTime(event.getTime())
												.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((PersonLeavesVehicleEvent) event).getPersonId().toString()))
												.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((PersonLeavesVehicleEvent) event).getVehicleId().toString()));
										eb.setType(ProtobufEvents.Event.Type.PersonLeavesVehicle).setPersonLeavesVehicle(pl);
									}
									else {
										if (event instanceof PersonMoneyEvent) {
											ProtobufEvents.PersonMoneyEvent.Builder pm = ProtobufEvents.PersonMoneyEvent.newBuilder()
													.setTime(event.getTime())
													.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((PersonMoneyEvent) event).getPersonId().toString()))
													.setAmount(((PersonMoneyEvent) event).getAmount());
											eb.setType(ProtobufEvents.Event.Type.PersonMoney).setPersonMoney(pm);
										}
										else {
											if (event instanceof PersonStuckEvent) {
												ProtobufEvents.PersonStuckEvent.Builder ps = ProtobufEvents.PersonStuckEvent.newBuilder()
														.setTime(event.getTime())
														.setPersId(ProtobufEvents.PersonId.newBuilder().setId(((PersonStuckEvent) event).getPersonId().toString()))
														.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((PersonStuckEvent) event).getLinkId().toString()))
														.setLegMode(((PersonStuckEvent) event).getLegMode());
												eb.setType(ProtobufEvents.Event.Type.PersonStuck).setPersonStuck(ps);
											}
											else {
												if (event instanceof TransitDriverStartsEvent) {
													ProtobufEvents.TransitDriverStartsEvent.Builder td = ProtobufEvents.TransitDriverStartsEvent.newBuilder()
															.setTime(event.getTime())
															.setDriverId(ProtobufEvents.PersonId.newBuilder().setId(((TransitDriverStartsEvent) event).getDriverId().toString()))
															.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((TransitDriverStartsEvent) event).getVehicleId().toString()))
															.setTransitRouteId(ProtobufEvents.TransitRouteId.newBuilder().setId(((TransitDriverStartsEvent) event).getTransitRouteId().toString()))
															.setTransitLineId(ProtobufEvents.TransitLineId.newBuilder().setId(((TransitDriverStartsEvent) event).getTransitLineId().toString()))
															.setDepartureId(ProtobufEvents.DepartureId.newBuilder().setId(((TransitDriverStartsEvent) event).getDepartureId().toString()));
													eb.setType(ProtobufEvents.Event.Type.TransitDriverStarts).setTransitDriverStarts(td);
												}
												else {
													if (event instanceof VehicleAbortsEvent) {
														ProtobufEvents.VehicleAbortsEvent.Builder va = ProtobufEvents.VehicleAbortsEvent.newBuilder()
																.setTime(event.getTime())
																.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((VehicleAbortsEvent) event).getVehicleId().toString()))
																.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((VehicleAbortsEvent) event).getLinkId().toString()));
														eb.setType(ProtobufEvents.Event.Type.VehicleAborts).setVehicleAborts(va);
													}
													else {
														if (event instanceof VehicleEntersTrafficEvent) {
															ProtobufEvents.VehicleEntersTrafficEvent.Builder ve = ProtobufEvents.VehicleEntersTrafficEvent.newBuilder()
																	.setTime(event.getTime())
																	.setDriverId(ProtobufEvents.PersonId.newBuilder().setId(((VehicleEntersTrafficEvent) event).getPersonId().toString()))
																	.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((VehicleEntersTrafficEvent) event).getLinkId().toString()))
																	.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((VehicleEntersTrafficEvent) event).getVehicleId().toString()))
																	.setNetworkMode(((VehicleEntersTrafficEvent) event).getNetworkMode())
																	.setRelPosOnLink(((VehicleEntersTrafficEvent) event).getRelativePositionOnLink());
															eb.setType(ProtobufEvents.Event.Type.VehicleEntersTraffic).setVehicleEntersTraffic(ve);
														}
														else {
															if (event instanceof VehicleLeavesTrafficEvent) {
																ProtobufEvents.VehicleLeavesTrafficEvent.Builder vl = ProtobufEvents.VehicleLeavesTrafficEvent.newBuilder()
																		.setTime(event.getTime())
																		.setDriverId(ProtobufEvents.PersonId.newBuilder().setId(((VehicleLeavesTrafficEvent) event).getPersonId().toString()))
																		.setLinkId(ProtobufEvents.LinkId.newBuilder().setId(((VehicleLeavesTrafficEvent) event).getLinkId().toString()))
																		.setVehId(ProtobufEvents.VehicleId.newBuilder().setId(((VehicleLeavesTrafficEvent) event).getVehicleId().toString()))
																		.setNetworkMode(((VehicleLeavesTrafficEvent) event).getNetworkMode())
																		.setRelPosOnLink(((VehicleLeavesTrafficEvent) event).getRelativePositionOnLink());
																eb.setType(ProtobufEvents.Event.Type.VehicleLeavesTraffic).setVehicleLeavesTraffic(vl);
															}
															else {
																if (Event2ProtoEvent.REPORT_GENERIC_EVENT) {
																	Event2ProtoEvent.REPORT_GENERIC_EVENT = false;
																	log.warn("Unknown event type: " + event.getEventType() + " creating generic protobuf event");
																	log.warn(Gbl.ONLYONCE);
//																throw new RuntimeException("Unsopported event type:" + event.getEventType());
																}
																ProtobufEvents.GenericEvent.Builder ge = ProtobufEvents.GenericEvent.newBuilder();
																for (Map.Entry<String,String> e : event.getAttributes().entrySet()) {
																	ge.addAttrVal(ProtobufEvents.AttrVal.newBuilder().setValue(e.getValue()).setAttribut(e.getKey()));
																}
																eb.setType(ProtobufEvents.Event.Type.GenericEvent).setGenericEvent(ge);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return eb.build();
	}
}
