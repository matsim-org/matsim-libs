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
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.SignalGroupStateChangedEvent;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.signalsystems.model.SignalGroupState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

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

		if (XYVxVyEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new XYVxVyEventImpl(new IdImpl(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_PERSON)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_X)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_Y)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_VX)), Double.parseDouble(atts.getValue(XYVxVyEventImpl.ATTRIBUTE_VY)), time));
		}else if (LinkLeaveEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkLeaveEvent(time, new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK)), atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE) == null ? null : new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE))));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkEnterEvent(time, new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK)), atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE) == null ? null : new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE))));
		} else if (ActivityEndEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new ActivityEndEvent(time, new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_LINK)), atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY) == null ? null : new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY)), atts.getValue(ActivityEndEvent.ATTRIBUTE_ACTTYPE)));
		} else if (ActivityStartEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new ActivityStartEvent(time, new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_LINK)), atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY) == null ? null : new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY)), atts.getValue(ActivityStartEvent.ATTRIBUTE_ACTTYPE)));
		} else if (PersonArrivalEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonArrivalEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(new PersonArrivalEvent(time, new IdImpl(atts.getValue(PersonArrivalEvent.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(PersonArrivalEvent.ATTRIBUTE_LINK)), mode));
		} else if (PersonDepartureEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonDepartureEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(new PersonDepartureEvent(time, new IdImpl(atts.getValue(PersonDepartureEvent.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(PersonDepartureEvent.ATTRIBUTE_LINK)), mode));
		} else if (Wait2LinkEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new Wait2LinkEvent(time, new IdImpl(atts.getValue(Wait2LinkEvent.ATTRIBUTE_PERSON)), new IdImpl(atts.getValue(Wait2LinkEvent.ATTRIBUTE_LINK)), atts.getValue(Wait2LinkEvent.ATTRIBUTE_VEHICLE) == null ? null : new IdImpl(atts.getValue(Wait2LinkEvent.ATTRIBUTE_VEHICLE))));
		} else if (PersonStuckEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(PersonStuckEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			String linkIdString = atts.getValue(PersonStuckEvent.ATTRIBUTE_LINK);
			IdImpl linkId = linkIdString == null ? null : new IdImpl(linkIdString); // linkId is optional
			this.events.processEvent(new PersonStuckEvent(time, new IdImpl(atts.getValue(PersonStuckEvent.ATTRIBUTE_PERSON)), linkId, mode));
		} else if (PersonMoneyEvent.EVENT_TYPE.equals(eventType) || "agentMoney".equals(eventType)) {
			this.events.processEvent(new PersonMoneyEvent(time, new IdImpl(atts.getValue(PersonMoneyEvent.ATTRIBUTE_PERSON)), Double.parseDouble(atts.getValue(PersonMoneyEvent.ATTRIBUTE_AMOUNT))));
		} else if (PersonEntersVehicleEvent.EVENT_TYPE.equals(eventType)) {
			String personString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_PERSON);
			String vehicleString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE);
			this.events.processEvent(new PersonEntersVehicleEvent(time, new IdImpl(personString), new IdImpl(vehicleString)));
		} else if (PersonLeavesVehicleEvent.EVENT_TYPE.equals(eventType)) {
			IdImpl pId = new IdImpl(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON));
			IdImpl vId = new IdImpl(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_VEHICLE));
			this.events.processEvent(new PersonLeavesVehicleEvent(time, pId, vId));
		} else if (TeleportationArrivalEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new TeleportationArrivalEvent(
					time, 
					new IdImpl(atts.getValue(TeleportationArrivalEvent.ATTRIBUTE_PERSON)), 
					Double.parseDouble(atts.getValue(TeleportationArrivalEvent.ATTRIBUT_DISTANCE))));
		} else if (VehicleArrivesAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_DELAY);
			this.events.processEvent(new VehicleArrivesAtFacilityEvent(time, new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)), new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY)), delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (VehicleDepartsAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleDepartsAtFacilityEvent.ATTRIBUTE_DELAY);
			this.events.processEvent(new VehicleDepartsAtFacilityEvent(time, new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)), new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY)), delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (TransitDriverStartsEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new TransitDriverStartsEvent(time, new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DRIVER_ID)), new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_VEHICLE_ID)), new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_LINE_ID)), new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_ROUTE_ID)), new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DEPARTURE_ID))));
		} else if (SignalGroupStateChangedEvent.EVENT_TYPE.equals(eventType)){
			Id systemId = new IdImpl(atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALSYSTEM_ID));
			Id groupId = new IdImpl(atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_ID));
			String state = atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_STATE);
			SignalGroupState newState = SignalGroupState.valueOf(state);
			this.events.processEvent(new SignalGroupStateChangedEvent(time, systemId, groupId, newState));
		} else if (BoardingDeniedEvent.EVENT_TYPE.equals(eventType)){
			Id personId = new IdImpl(atts.getValue(BoardingDeniedEvent.ATTRIBUTE_PERSON_ID));
			Id vehicleId = new  IdImpl(atts.getValue(BoardingDeniedEvent.ATTRIBUTE_VEHICLE_ID));
			this.events.processEvent(new BoardingDeniedEvent(time, personId, vehicleId));
		} else if (AgentWaitingForPtEvent.EVENT_TYPE.equals(eventType)){
			Id agentId = new IdImpl(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_AGENT));
			Id waitStopId = new  IdImpl(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_WAITSTOP));
			Id destinationStopId = new  IdImpl(atts.getValue(AgentWaitingForPtEvent.ATTRIBUTE_DESTINATIONSTOP));
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
