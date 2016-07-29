/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReaderXMLv1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EventsReaderXMLv1 extends MatsimXmlParser {

	public interface CustomEventMapper<T extends Event> /* extends Function<GenericEvent, T> */ {
		T apply(GenericEvent event);
	}

	static public final String EVENT = "event";

	private final EventsManager events;
	private final Map<String, CustomEventMapper> customEventMappers = new HashMap<>();

	public EventsReaderXMLv1(final EventsManager events) {
		this.events = events;
		this.setValidating(false);// events-files have no DTD, thus they cannot validate
	}

	public void addCustomEventMapper(String eventType, CustomEventMapper cem) {
		customEventMappers.put(eventType, cem);
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (EVENT.equals(name)) {
			startEvent(atts);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// ignore characters to prevent OutOfMemoryExceptions
		/* the events-file only contains empty tags with attributes,
		 * but without the dtd or schema, all whitespace between tags is handled
		 * by characters and added up by super.characters, consuming huge
		 * amount of memory when large events-files are read in.
		 */
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
	}

	private void startEvent(final Attributes atts) {
		double time = Double.parseDouble(atts.getValue("time"));
		String eventType = atts.getValue("type");

		// === material related to wait2link below here ===
		if (LinkLeaveEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkLeaveEvent(time, 
					Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE), Vehicle.class), 
					Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK), Link.class)
					// had driver id in previous version
					));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkEnterEvent(time, 
					Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE), Vehicle.class), 
					Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK), Link.class)
					// had driver id in previous version
					));
		} else if (VehicleEntersTrafficEvent.EVENT_TYPE.equals(eventType) ) {
			// (this is the new version, marked by the new events name)

			this.events.processEvent(new VehicleEntersTrafficEvent(time, 
					Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_DRIVER), Person.class), 
					Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_LINK), Link.class), 
					Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE), Vehicle.class),
					atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_NETWORKMODE), 
					Double.parseDouble( atts.getValue( VehicleEntersTrafficEvent.ATTRIBUTE_POSITION) )
					));
		} else if ( "wait2link".equals(eventType) ) {
			// (this is the old version, marked by the old events name)

			// retrofit vehicle Id:
			Id<Vehicle> vehicleId ;
			if ( atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE) != null ) {
				vehicleId = Id.create( atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE), Vehicle.class ) ;
			} else {
				// for the old events type, we set the vehicle id to the driver id if the vehicle id does not exist:
				vehicleId = Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_DRIVER), Vehicle.class);
			}
			// retrofit position:
			double position ;
			if ( atts.getValue( VehicleEntersTrafficEvent.ATTRIBUTE_POSITION)!=null ) {
				position = Double.parseDouble( atts.getValue( VehicleEntersTrafficEvent.ATTRIBUTE_POSITION) ) ;
			} else {
				position = 1.0 ;
			}
			this.events.processEvent(new VehicleEntersTrafficEvent(time, 
					Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_DRIVER), Person.class), 
					Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_LINK), Link.class), 
					vehicleId,
					atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_NETWORKMODE), 
					position
					));
		} else if (VehicleLeavesTrafficEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new VehicleLeavesTrafficEvent(time, 
					Id.create(atts.getValue(VehicleLeavesTrafficEvent.ATTRIBUTE_DRIVER), Person.class), 
					Id.create(atts.getValue(VehicleLeavesTrafficEvent.ATTRIBUTE_LINK), Link.class), 
					atts.getValue(VehicleLeavesTrafficEvent.ATTRIBUTE_VEHICLE) == null ? null : Id.create(atts.getValue(VehicleLeavesTrafficEvent.ATTRIBUTE_VEHICLE), Vehicle.class), 
					atts.getValue(VehicleLeavesTrafficEvent.ATTRIBUTE_NETWORKMODE), 
					Double.parseDouble( atts.getValue( VehicleLeavesTrafficEvent.ATTRIBUTE_POSITION) )
					));
		}
		// === material related to wait2link above here
		else if (ActivityEndEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new ActivityEndEvent(
					time, 
					Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_PERSON), Person.class), 
					Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_LINK), Link.class), 
					atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY) == null ? null : Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY), ActivityFacility.class), 
					atts.getValue(ActivityEndEvent.ATTRIBUTE_ACTTYPE)));
		} else if (ActivityStartEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new ActivityStartEvent(time, Id.create(atts.getValue(ActivityStartEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(ActivityStartEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY) == null ? null : Id.create(atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY), ActivityFacility.class), atts.getValue(ActivityStartEvent.ATTRIBUTE_ACTTYPE)));
		} else if (PersonArrivalEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonArrivalEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(new PersonArrivalEvent(time, Id.create(atts.getValue(PersonArrivalEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(PersonArrivalEvent.ATTRIBUTE_LINK), Link.class), mode));
		} else if (PersonDepartureEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonDepartureEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(new PersonDepartureEvent(time, Id.create(atts.getValue(PersonDepartureEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(PersonDepartureEvent.ATTRIBUTE_LINK), Link.class), mode));
		} else if (PersonStuckEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonStuckEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			String linkIdString = atts.getValue(PersonStuckEvent.ATTRIBUTE_LINK);
			Id<Link> linkId = linkIdString == null ? null : Id.create(linkIdString, Link.class); // linkId is optional
			this.events.processEvent(new PersonStuckEvent(time, Id.create(atts.getValue(PersonStuckEvent.ATTRIBUTE_PERSON), Person.class), linkId, mode));
		} else if (VehicleAbortsEvent.EVENT_TYPE.equals(eventType)) {
			String linkIdString = atts.getValue(VehicleAbortsEvent.ATTRIBUTE_LINK);
			Id<Link> linkId = linkIdString == null ? null : Id.create(linkIdString, Link.class);
			this.events.processEvent(new VehicleAbortsEvent(time, Id.create(atts.getValue(VehicleAbortsEvent.ATTRIBUTE_VEHICLE), Vehicle.class), linkId));
		}else if (PersonMoneyEvent.EVENT_TYPE.equals(eventType) || "agentMoney".equals(eventType)) {
			this.events.processEvent(new PersonMoneyEvent(time, Id.create(atts.getValue(PersonMoneyEvent.ATTRIBUTE_PERSON), Person.class), Double.parseDouble(atts.getValue(PersonMoneyEvent.ATTRIBUTE_AMOUNT))));
		} else if (PersonEntersVehicleEvent.EVENT_TYPE.equals(eventType)) {
			String personString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_PERSON);
			String vehicleString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE);
			this.events.processEvent(new PersonEntersVehicleEvent(time, Id.create(personString, Person.class), Id.create(vehicleString, Vehicle.class)));
		} else if (PersonLeavesVehicleEvent.EVENT_TYPE.equals(eventType)) {
			Id<Person> pId = Id.create(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON), Person.class);
			Id<Vehicle> vId = Id.create(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
			this.events.processEvent(new PersonLeavesVehicleEvent(time, pId, vId));
		} else if (TeleportationArrivalEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new TeleportationArrivalEvent(
					time, 
					Id.create(atts.getValue(TeleportationArrivalEvent.ATTRIBUTE_PERSON), Person.class), 
					Double.parseDouble(atts.getValue(TeleportationArrivalEvent.ATTRIBUT_DISTANCE))));
		} else if (VehicleArrivesAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_DELAY);
			this.events.processEvent(new VehicleArrivesAtFacilityEvent(time, Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE), Vehicle.class), Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY), TransitStopFacility.class), delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (VehicleDepartsAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleDepartsAtFacilityEvent.ATTRIBUTE_DELAY);
			this.events.processEvent(new VehicleDepartsAtFacilityEvent(time, Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE), Vehicle.class), Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY), TransitStopFacility.class), delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (TransitDriverStartsEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new TransitDriverStartsEvent(time, Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DRIVER_ID), Person.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_VEHICLE_ID), Vehicle.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_LINE_ID), TransitLine.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_ROUTE_ID), TransitRoute.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DEPARTURE_ID), Departure.class)));
		} else if (BoardingDeniedEvent.EVENT_TYPE.equals(eventType)){
			Id<Person> personId = Id.create(atts.getValue(BoardingDeniedEvent.ATTRIBUTE_PERSON_ID), Person.class);
			Id<Vehicle> vehicleId = Id.create(atts.getValue(BoardingDeniedEvent.ATTRIBUTE_VEHICLE_ID), Vehicle.class);
			this.events.processEvent(new BoardingDeniedEvent(time, personId, vehicleId));
		} else if (AgentWaitingForPtEvent.EVENT_TYPE.equals(eventType)){
			Id<Person> agentId = Id.create(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_AGENT), Person.class);
			Id<TransitStopFacility> waitStopId = Id.create(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_WAITSTOP), TransitStopFacility.class);
			Id<TransitStopFacility> destinationStopId = Id.create(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_DESTINATIONSTOP), TransitStopFacility.class);
			this.events.processEvent(new AgentWaitingForPtEvent(time, agentId, waitStopId, destinationStopId));
		} else {
			GenericEvent event = new GenericEvent(eventType, time);
			for ( int ii=0; ii<atts.getLength(); ii++ ) {
				String key = atts.getLocalName(ii);
				if ( key.equals("time") || key.equals("type") ) {
					continue;
				}
				String value = atts.getValue(ii);
				event.getAttributes().put(key, value);
			}
			CustomEventMapper cem = customEventMappers.get(eventType);
			if (cem != null) {
				this.events.processEvent(cem.apply(event));
			} else {
				this.events.processEvent(event);
			}
		}
	}

}
