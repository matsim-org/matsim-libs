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

package playground.gregor.sim2d_v4.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Stack;

public class EventsReaderXMLv1ExtendedSim2DVersion extends MatsimXmlParser {

	static public final String EVENT = "event";

	private final EventsManager events;

	public EventsReaderXMLv1ExtendedSim2DVersion(final EventsManager events) {
		this.events = events;
		this.setValidating(false);// events-files have no DTD, thus they cannot validate
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (EVENT.equals(name)) {
			startEvent(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
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

	private void startEvent(final Attributes atts) {
		double time = Double.parseDouble(atts.getValue("time"));
		String eventType = atts.getValue("type");

		if (XYVxVyEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new XYVxVyEventImpl(Id.create(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_PERSON), Person.class), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_X)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_Y)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_VX)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_VY)), time));
		}else if (LinkLeaveEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkLeaveEvent(time, atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE) == null ? null : Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE), Vehicle.class), Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK), Link.class)));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkEnterEvent(time, atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE) == null ? null : Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE), Vehicle.class), Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK), Link.class)));
		} else if (ActivityEndEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new ActivityEndEvent(time, Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY) == null ? null : Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY), ActivityFacility.class), atts.getValue(ActivityEndEvent.ATTRIBUTE_ACTTYPE)));
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
		} else if (VehicleEntersTrafficEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new VehicleEntersTrafficEvent(time, Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_DRIVER), Person.class), Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE) == null ? null : Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE), Vehicle.class), atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_NETWORKMODE), 1.0));
		} else if (PersonStuckEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonStuckEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			String linkIdString = atts.getValue(PersonStuckEvent.ATTRIBUTE_LINK);
			Id<Link> linkId = linkIdString == null ? null : Id.create(linkIdString, Link.class); // linkId is optional
			this.events.processEvent(new PersonStuckEvent(time, Id.create(atts.getValue(PersonStuckEvent.ATTRIBUTE_PERSON), Person.class), linkId, mode));
		} else if (PersonMoneyEvent.EVENT_TYPE.equals(eventType) || "agentMoney".equals(eventType)) {
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
					Double.parseDouble(atts.getValue(TeleportationArrivalEvent.ATTRIBUTE_DISTANCE))));
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
			Event event = new GenericEvent(eventType, time);
			for ( int ii=0; ii<atts.getLength(); ii++ ) {
				String key = atts.getLocalName(ii);
				if ( key.equals("time") || key.equals("type") ) {
					continue;
				}
				String value = atts.getValue(ii);
				event.getAttributes().put(key, value);
			}
			this.events.processEvent(event);
		}
	}

}
