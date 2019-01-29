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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.contrib.protobuf.events.ProtobufEvents;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.Map;

/**
 * Created by laemmel on 17/02/16.
 */
public abstract class ProtoEvent2Event {
	static Event getEvent(ProtobufEvents.Event pe) {
		if (pe.getType() == ProtobufEvents.Event.Type.LinkEnter) {
			return new LinkEnterEvent(pe.getLinkEnter().getTime(), Id.createVehicleId(pe.getLinkEnter().getVehId().getId()),
					Id.createLinkId(pe.getLinkEnter().getLinkId().getId()));
		}
		else {
			if (pe.getType() == ProtobufEvents.Event.Type.LinkLeave) {
				return new LinkLeaveEvent(pe.getLinkLeave().getTime(), Id.createVehicleId(pe.getLinkLeave().getVehId().getId()),
						Id.createLinkId(pe.getLinkLeave().getLinkId().getId()));
			}
			else {
				if (pe.getType() == ProtobufEvents.Event.Type.ActivityEnd) {
					return new ActivityEndEvent(pe.getActEnd().getTime(), Id.createPersonId(pe.getActEnd().getPersId().getId()),
							Id.createLinkId(pe.getActEnd().getLinkId().getId()), Id.create(pe.getActEnd().getFacilityId().getId(),
							ActivityFacility.class), pe.getActEnd().getActType());
				}
				else {
					if (pe.getType() == ProtobufEvents.Event.Type.ActivityStart) {
						return new ActivityStartEvent(pe.getActStart().getTime(), Id.createPersonId(pe.getActStart().getPersId().getId()),
								Id.createLinkId(pe.getActStart().getLinkId().getId()), Id.create(pe.getActStart().getFacilityId().getId(),
								ActivityFacility.class), pe.getActStart().getActType());

					}
					else {
						if (pe.getType() == ProtobufEvents.Event.Type.PersonArrival) {
							return new PersonArrivalEvent(pe.getPersonArrival().getTime(), Id.createPersonId(pe.getPersonArrival().getPersId().getId()),
									Id.createLinkId(pe.getPersonArrival().getLinkId().getId()), pe.getPersonArrival().getLegMode());
						}
						else {
							if (pe.getType() == ProtobufEvents.Event.Type.PersonDeparture) {
								return new PersonDepartureEvent(pe.getPersonDeparture().getTime(), Id.createPersonId(pe.getPersonDeparture().getPersId().getId()),
										Id.createLinkId(pe.getPersonDeparture().getLinkId().getId()), pe.getPersonDeparture().getLegMode());
							}
							else {
								if (pe.getType() == ProtobufEvents.Event.Type.VehicleEntersTraffic) {
									return new VehicleEntersTrafficEvent(pe.getVehicleEntersTraffic().getTime(), Id.createPersonId(pe.getVehicleEntersTraffic().getDriverId().getId()),
											Id.createLinkId(pe.getVehicleEntersTraffic().getLinkId().getId()), Id.createVehicleId(pe.getVehicleEntersTraffic().getVehId().getId()),
											pe.getVehicleEntersTraffic().getNetworkMode(), pe.getVehicleEntersTraffic().getRelPosOnLink());
								}
								else {
									if (pe.getType() == ProtobufEvents.Event.Type.PersonLeavesVehicle) {
										return new PersonLeavesVehicleEvent(pe.getPersonLeavesVehicle().getTime(), Id.createPersonId(pe.getPersonLeavesVehicle().getPersId().getId()),
												Id.createVehicleId(pe.getPersonLeavesVehicle().getVehId().getId()));
									}
									else {
										if (pe.getType() == ProtobufEvents.Event.Type.VehicleLeavesTraffic) {
											return new VehicleLeavesTrafficEvent(pe.getVehicleLeavesTraffic().getTime(), Id.createPersonId(pe.getVehicleLeavesTraffic().getDriverId().getId()),
													Id.createLinkId(pe.getVehicleLeavesTraffic().getLinkId().getId()), Id.createVehicleId(pe.getVehicleLeavesTraffic().getVehId().getId()),
													pe.getVehicleLeavesTraffic().getNetworkMode(), pe.getVehicleLeavesTraffic().getRelPosOnLink());
										}
										else {
											if (pe.getType() == ProtobufEvents.Event.Type.PersonEntersVehicle) {
												return new PersonEntersVehicleEvent(pe.getPersonEntersVehicle().getTime(), Id.createPersonId(pe.getPersonEntersVehicle().getPersId().getId()),
														Id.createVehicleId(pe.getPersonEntersVehicle().getVehId().getId()));
											}
											else {
												if (pe.getType() == ProtobufEvents.Event.Type.PersonMoney) {
													return new PersonMoneyEvent(pe.getPersonMoney().getTime(), Id.createPersonId(pe.getPersonMoney().getPersId().getId()),
															pe.getPersonMoney().getAmount());
												}
												else {
													if (pe.getType() == ProtobufEvents.Event.Type.PersonStuck) {
														return new PersonStuckEvent(pe.getPersonStuck().getTime(), Id.createPersonId(pe.getPersonStuck().getPersId().getId()),
																Id.createLinkId(pe.getPersonStuck().getLinkId().getId()), pe.getPersonStuck().getLegMode());
													}
													else {
														if (pe.getType() == ProtobufEvents.Event.Type.TransitDriverStarts) {
															return new TransitDriverStartsEvent(pe.getTransitDriverStarts().getTime(), Id.createPersonId(pe.getTransitDriverStarts().getDriverId().getId()),
																	Id.createVehicleId(pe.getTransitDriverStarts().getVehId().getId()), Id.create(pe.getTransitDriverStarts().getTransitLineId().getId(),
																	TransitLine.class), Id.create(pe.getTransitDriverStarts().getTransitRouteId().getId(), TransitRoute.class),
																	Id.create(pe.getTransitDriverStarts().getDepartureId().getId(), Departure.class));
														}
														else {
															if (pe.getType() == ProtobufEvents.Event.Type.VehicleAborts) {
																return new VehicleAbortsEvent(pe.getVehicleAborts().getTime(), Id.createVehicleId(pe.getVehicleAborts().getVehId().getId()),
																		Id.createLinkId(pe.getVehicleAborts().getLinkId().getId()));
															}
															else {
																if (pe.getType() == ProtobufEvents.Event.Type.GenericEvent) {
																	GenericEvent ge = new GenericEvent(pe.getGenericEvent().getType(),pe.getGenericEvent().getTime());
																	Map<String, String> map = ge.getAttributes();
																	for (ProtobufEvents.AttrVal av : pe.getGenericEvent().getAttrValList()) {
																		map.put(av.getAttribut(),av.getValue());
																	}
																	return ge;
																} else {
																	throw new RuntimeException("Unsupported event type: " + pe.getType());
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
		}
	}

}
