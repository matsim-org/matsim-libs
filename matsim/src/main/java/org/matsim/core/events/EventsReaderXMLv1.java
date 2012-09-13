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

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkChangeEvent;
import org.matsim.core.api.experimental.events.LinkChangeFlowCapacityEvent;
import org.matsim.core.api.experimental.events.LinkChangeFreespeedEvent;
import org.matsim.core.api.experimental.events.LinkChangeLanesEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TravelledEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.signalsystems.model.SignalGroupState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EventsReaderXMLv1 extends MatsimXmlParser {

	static public final String EVENT = "event";

	private final EventsManager events;
	private final EventsFactory builder;

	public EventsReaderXMLv1(final EventsManager events) {
		this.events = events;
		this.builder = (EventsFactory) events.getFactory();
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

		if (LinkLeaveEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createLinkLeaveEvent(time,
					new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK)),
					atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE) == null ? null : new IdImpl(atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE))));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createLinkEnterEvent(time,
					new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK)),
					atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE) == null ? null : new IdImpl(atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE))));
		} else if (ActivityEndEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createActivityEndEvent(time,
					new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_LINK)),
					atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY) == null ? null : new IdImpl(atts.getValue(ActivityEndEvent.ATTRIBUTE_FACILITY)),
					atts.getValue(ActivityEndEvent.ATTRIBUTE_ACTTYPE)));
		} else if (ActivityStartEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createActivityStartEvent(time,
					new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_LINK)),
					atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY) == null ? null : new IdImpl(atts.getValue(ActivityStartEvent.ATTRIBUTE_FACILITY)),
					atts.getValue(ActivityStartEvent.ATTRIBUTE_ACTTYPE)));
		} else if (AgentArrivalEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(AgentArrivalEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(this.builder.createAgentArrivalEvent(time,
					new IdImpl(atts.getValue(AgentArrivalEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentArrivalEvent.ATTRIBUTE_LINK)), mode));
		} else if (AgentDepartureEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(AgentDepartureEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(this.builder.createAgentDepartureEvent(time,
					new IdImpl(atts.getValue(AgentDepartureEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentDepartureEvent.ATTRIBUTE_LINK)), mode));
		} else if (AgentWait2LinkEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentWait2LinkEvent(time,
					new IdImpl(atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_LINK)), 
					atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_VEHICLE) == null ? null : new IdImpl(atts.getValue(AgentWait2LinkEvent.ATTRIBUTE_VEHICLE))));
		} else if (AgentStuckEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = atts.getValue(AgentStuckEvent.ATTRIBUTE_LEGMODE);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(this.builder.createAgentStuckEvent(time,
					new IdImpl(atts.getValue(AgentStuckEvent.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentStuckEvent.ATTRIBUTE_LINK)),
					mode));
		} else if (AgentMoneyEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentMoneyEvent(time,
					new IdImpl(atts.getValue(AgentMoneyEvent.ATTRIBUTE_PERSON)),
					Double.parseDouble(atts.getValue(AgentMoneyEvent.ATTRIBUTE_AMOUNT))));
		} else if (PersonEntersVehicleEvent.EVENT_TYPE.equals(eventType)) {
			String personString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_PERSON);
			String vehicleString = atts.getValue(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE);
			this.events.processEvent(this.builder.createPersonEntersVehicleEvent(time,
					new IdImpl(personString),
					new IdImpl(vehicleString)));
		} else if (PersonLeavesVehicleEvent.EVENT_TYPE.equals(eventType)) {
			IdImpl pId = new IdImpl(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON));
			IdImpl vId = new IdImpl(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_VEHICLE));
			this.events.processEvent(this.builder.createPersonLeavesVehicleEvent(time, pId, vId));
		} else if (TravelledEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new TravelledEvent(
					time, 
					new IdImpl(atts.getValue(TravelledEvent.ATTRIBUTE_PERSON)), 
					Double.parseDouble(atts.getValue(TravelledEvent.ATTRIBUT_DISTANCE))));
		} else if (VehicleArrivesAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_DELAY);
			this.events.processEvent(this.builder.createVehicleArrivesAtFacilityEvent(time,
					new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)),
					new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY)),
					delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (VehicleDepartsAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			String delay = atts.getValue(VehicleDepartsAtFacilityEvent.ATTRIBUTE_DELAY);
			this.events.processEvent(this.builder.createVehicleDepartsAtFacilityEvent(time,
					new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)),
					new IdImpl(atts.getValue(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY)),
					delay == null ? 0.0 : Double.parseDouble(delay)));
		} else if (TransitDriverStartsEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createTransitDriverStartsEvent(time,
					new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DRIVER_ID)),
					new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_VEHICLE_ID)),
					new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_LINE_ID)),
					new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_ROUTE_ID)),
					new IdImpl(atts.getValue(TransitDriverStartsEvent.ATTRIBUTE_DEPARTURE_ID))));
		} else if (SignalGroupStateChangedEvent.EVENT_TYPE.equals(eventType)){
			Id systemId = new IdImpl(atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALSYSTEM_ID));
			Id groupId = new IdImpl(atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_ID));
			String state = atts.getValue(SignalGroupStateChangedEvent.ATTRIBUTE_SIGNALGROUP_STATE);
			SignalGroupState newState = SignalGroupState.valueOf(state);
			this.events.processEvent(this.builder.createSignalGroupStateChangedEvent(time, systemId, groupId, newState));
		} 
		else if (LinkChangeFlowCapacityEvent.EVENT_TYPE.equals(eventType)) {
			String changeTypeString = atts.getValue(LinkChangeEvent.CHANGETYPE);
			NetworkChangeEvent.ChangeType changeType = null;
			if (changeTypeString.equals(LinkChangeEvent.CHANGETYPEABSOLUTE)) changeType = NetworkChangeEvent.ChangeType.ABSOLUTE;
			else if (changeTypeString.equals(LinkChangeEvent.CHANGETYPEFACTOR)) changeType = NetworkChangeEvent.ChangeType.FACTOR;
			double value = Double.valueOf(atts.getValue(LinkChangeEvent.CHANGEVALUE));
			NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(changeType, value);
			this.events.processEvent(this.builder.createLinkChangeFlowCapacityEvent(time, new IdImpl(atts.getValue(LinkChangeEvent.ATTRIBUTE_LINK)), changeValue));
		} else if (LinkChangeFreespeedEvent.EVENT_TYPE.equals(eventType)) {
			String changeTypeString = atts.getValue(LinkChangeEvent.CHANGETYPE);
			NetworkChangeEvent.ChangeType changeType = null;
			if (changeTypeString.equals(LinkChangeEvent.CHANGETYPEABSOLUTE)) changeType = NetworkChangeEvent.ChangeType.ABSOLUTE;
			else if (changeTypeString.equals(LinkChangeEvent.CHANGETYPEFACTOR)) changeType = NetworkChangeEvent.ChangeType.FACTOR;
			double value = Double.valueOf(atts.getValue(LinkChangeEvent.CHANGEVALUE));
			NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(changeType, value);
			this.events.processEvent(this.builder.createLinkChangeFreespeedEvent(time, new IdImpl(atts.getValue(LinkChangeEvent.ATTRIBUTE_LINK)), changeValue));
		} else if (LinkChangeLanesEvent.EVENT_TYPE.equals(eventType)) {
			String changeTypeString = atts.getValue(LinkChangeEvent.CHANGETYPE);
			NetworkChangeEvent.ChangeType changeType = null;
			if (changeTypeString.equals(LinkChangeEvent.CHANGETYPEABSOLUTE)) changeType = NetworkChangeEvent.ChangeType.ABSOLUTE;
			else if (changeTypeString.equals(LinkChangeEvent.CHANGETYPEFACTOR)) changeType = NetworkChangeEvent.ChangeType.FACTOR;
			double value = Double.valueOf(atts.getValue(LinkChangeEvent.CHANGEVALUE));
			NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(changeType, value);
			this.events.processEvent(this.builder.createLinkChangeLanesEvent(time, new IdImpl(atts.getValue(LinkChangeEvent.ATTRIBUTE_LINK)), changeValue));
		} else {
			Event event = this.builder.createGenericEvent( eventType, time );
			for ( int ii=0 ; ii<atts.getLength() ; ii++ ) {
				String key = atts.getLocalName(ii) ;
				if ( key.equals("time") || key.equals("type") ) {
					continue ;
				}
				String value = atts.getValue(ii) ;
				event.getAttributes().put(key, value) ;
			}
			this.events.processEvent(event) ;
		}
	}

}
