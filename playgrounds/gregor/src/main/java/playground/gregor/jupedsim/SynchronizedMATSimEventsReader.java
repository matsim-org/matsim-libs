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

package playground.gregor.jupedsim;

import java.util.PriorityQueue;
import java.util.Stack;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

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
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.SignalGroupStateChangedEvent;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.signals.model.SignalGroup;
import org.matsim.signals.model.SignalGroupState;
import org.matsim.signals.model.SignalSystem;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

public class SynchronizedMATSimEventsReader extends MatsimXmlParser {

	static public final String EVENT = "event";


	private PriorityQueue<Event> q;


	private ReentrantLock lock;

	public SynchronizedMATSimEventsReader(final PriorityQueue<Event> q, final ReentrantLock lock) {
		this.q = q;
		this.setValidating(false);// events-files have no DTD, thus they cannot validate
		this.lock = lock;
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
		if (!this.lock.isHeldByCurrentThread()){
			this.lock.lock();
		}
		double time = Double.parseDouble(atts.getValue("time"));
		String eventType = atts.getValue("type");

		if (XYVxVyEventImpl.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new XYVxVyEventImpl(Id.create(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_PERSON), Person.class), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_X)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_Y)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_VX)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_VY)), time));
		}else if (LinkLeaveEvent.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new LinkLeaveEvent(time, Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE) == null ? null : Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE), Vehicle.class)));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new LinkEnterEvent(time, Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE) == null ? null : Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE), Vehicle.class)));
		} else if (ActivityEndEvent.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new ActivityEndEvent(time, Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY) == null ? null : Id.create(atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY), ActivityFacility.class), atts.getValue(ActivityEndEvent.ATTRIBUTE_ACTTYPE)));
		} else if (ActivityStartEvent.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new ActivityStartEvent(time, Id.create(atts.getValue(ActivityStartEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(ActivityStartEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY) == null ? null : Id.create(atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY), ActivityFacility.class), atts.getValue(ActivityStartEvent.ATTRIBUTE_ACTTYPE)));
		} else if (PersonArrivalEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonArrivalEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.q.offer(new PersonArrivalEvent(time, Id.create(atts.getValue(PersonArrivalEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(PersonArrivalEvent.ATTRIBUTE_LINK), Link.class), mode));
		} else if (PersonDepartureEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonDepartureEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.q.offer(new PersonDepartureEvent(time, Id.create(atts.getValue(PersonDepartureEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(PersonDepartureEvent.ATTRIBUTE_LINK), Link.class), mode));
		} else if (Wait2LinkEvent.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new Wait2LinkEvent(time, Id.create(atts.getValue(Wait2LinkEvent.ATTRIBUTE_PERSON), Person.class), Id.create(atts.getValue(Wait2LinkEvent.ATTRIBUTE_LINK), Link.class), atts.getValue(Wait2LinkEvent.ATTRIBUTE_VEHICLE) == null ? null : Id.create(atts.getValue(Wait2LinkEvent.ATTRIBUTE_VEHICLE), Vehicle.class)));
		} else if (PersonStuckEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonStuckEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			String linkIdString = atts.getValue(PersonStuckEvent.ATTRIBUTE_LINK);
			Id<Link> linkId = linkIdString == null ? null : Id.create(linkIdString, Link.class); // linkId is optional
			this.q.offer(new PersonStuckEvent(time, Id.create(atts.getValue(PersonStuckEvent.ATTRIBUTE_PERSON), Person.class), linkId, mode));
		} else if (PersonMoneyEvent.EVENT_TYPE.equals(eventType) || "agentMoney".equals(eventType)) {
			this.q.offer(new PersonMoneyEvent(time, Id.create(atts.getValue(PersonMoneyEvent.ATTRIBUTE_PERSON), Person.class), Double.parseDouble(atts.getValue(PersonMoneyEvent.ATTRIBUTE_AMOUNT))));
		} else if (PersonEntersVehicleEvent.EVENT_TYPE.equals(eventType)) {
			String personString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_PERSON);
			String vehicleString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE);
			this.q.offer(new PersonEntersVehicleEvent(time, Id.create(personString, Person.class), Id.create(vehicleString, Vehicle.class)));
		} else if (PersonLeavesVehicleEvent.EVENT_TYPE.equals(eventType)) {
			Id<Person> pId = Id.create(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON), Person.class);
			Id<Vehicle> vId = Id.create(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
			this.q.offer(new PersonLeavesVehicleEvent(time, pId, vId));
		} else if (TeleportationArrivalEvent.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new TeleportationArrivalEvent(
					time, 
					Id.create(atts.getValue(TeleportationArrivalEvent.ATTRIBUTE_PERSON), Person.class), 
					Double.parseDouble(atts.getValue(TeleportationArrivalEvent.ATTRIBUT_DISTANCE))));
		} else if (VehicleArrivesAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_DELAY);
			this.q.offer(new VehicleArrivesAtFacilityEvent(time, Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE), Vehicle.class), Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY), TransitStopFacility.class), delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (VehicleDepartsAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleDepartsAtFacilityEvent.ATTRIBUTE_DELAY);
			this.q.offer(new VehicleDepartsAtFacilityEvent(time, Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE), Vehicle.class), Id.create(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY), TransitStopFacility.class), delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (TransitDriverStartsEvent.EVENT_TYPE.equals(eventType)) {
			this.q.offer(new TransitDriverStartsEvent(time, Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DRIVER_ID), Person.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_VEHICLE_ID), Vehicle.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_LINE_ID), TransitLine.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_ROUTE_ID), TransitRoute.class), Id.create(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DEPARTURE_ID), Departure.class)));
		} else if (SignalGroupStateChangedEvent.EVENT_TYPE.equals(eventType)){
			Id<SignalSystem> systemId = Id.create(atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALSYSTEM_ID), SignalSystem.class);
			Id<SignalGroup> groupId = Id.create(atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_ID), SignalGroup.class);
			String state = atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_STATE);
			SignalGroupState newState = SignalGroupState.valueOf(state);
			this.q.offer(new SignalGroupStateChangedEvent(time, systemId, groupId, newState));
		} else if (BoardingDeniedEvent.EVENT_TYPE.equals(eventType)){
			Id<Person> personId = Id.create(atts.getValue(BoardingDeniedEvent.ATTRIBUTE_PERSON_ID), Person.class);
			Id<Vehicle> vehicleId = Id.create(atts.getValue(BoardingDeniedEvent.ATTRIBUTE_VEHICLE_ID), Vehicle.class);
			this.q.offer(new BoardingDeniedEvent(time, personId, vehicleId));
		} else if (AgentWaitingForPtEvent.EVENT_TYPE.equals(eventType)){
			Id<Person> agentId = Id.create(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_AGENT), Person.class);
			Id<TransitStopFacility> waitStopId = Id.create(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_WAITSTOP), TransitStopFacility.class);
			Id<TransitStopFacility> destinationStopId = Id.create(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_DESTINATIONSTOP), TransitStopFacility.class);
			this.q.offer(new AgentWaitingForPtEvent(time, agentId, waitStopId, destinationStopId));
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
			this.q.offer(event);
		}
		if (time - q.peek().getTime() > 10) {
			this.lock.unlock();
		}
	}

}
