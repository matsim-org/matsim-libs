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

import org.xml.sax.Attributes;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.events.BasicVehicleArrivesAtFacilityEvent;
import org.matsim.core.basic.v01.events.BasicVehicleDepartsAtFacilityEvent;
import org.matsim.core.utils.io.MatsimXmlParser;

public class EventsReaderXMLv1 extends MatsimXmlParser {

	static public final String EVENT = "event";
	static public final String EVENTS = "events";

	private final EventsImpl events;
	private EventsFactoryImpl builder;

	public EventsReaderXMLv1(final EventsImpl events) {
		this.events = events;
		this.builder = (EventsFactoryImpl) events.getFactory();
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

	private void startEvent(final Attributes atts) {
		double time = Double.parseDouble(atts.getValue("time"));
		String eventType = atts.getValue("type");

		if (LinkLeaveEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createLinkLeaveEvent(time,
					new IdImpl(atts.getValue(LinkLeaveEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(LinkLeaveEventImpl.ATTRIBUTE_LINK))));
		} else if (LinkEnterEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createLinkEnterEvent(time,
					new IdImpl(atts.getValue(LinkEnterEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(LinkEnterEventImpl.ATTRIBUTE_LINK))));
		} else if (ActivityEndEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createActivityEndEvent(time,
					new IdImpl(atts.getValue(ActivityEndEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(ActivityEndEventImpl.ATTRIBUTE_LINK)),
					atts.getValue(ActivityEndEventImpl.ATTRIBUTE_ACTTYPE)));
		} else if (ActivityStartEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createActivityStartEvent(time,
					new IdImpl(atts.getValue(ActivityStartEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(ActivityStartEventImpl.ATTRIBUTE_LINK)),
					atts.getValue(ActivityStartEventImpl.ATTRIBUTE_ACTTYPE)));
		} else if (AgentArrivalEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentArrivalEvent(time,
					new IdImpl(atts.getValue(AgentArrivalEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentArrivalEventImpl.ATTRIBUTE_LINK))));
		} else if (AgentDepartureEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentDepartureEvent(time,
					new IdImpl(atts.getValue(AgentDepartureEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentDepartureEventImpl.ATTRIBUTE_LINK))));
		} else if (AgentWait2LinkEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentWait2LinkEvent(time,
					new IdImpl(atts.getValue(AgentWait2LinkEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentWait2LinkEventImpl.ATTRIBUTE_LINK))));
		} else if (AgentStuckEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentStuckEvent(time,
					new IdImpl(atts.getValue(AgentStuckEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(AgentStuckEventImpl.ATTRIBUTE_LINK))));
		} else if (AgentMoneyEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createAgentMoneyEvent(time,
					new IdImpl(atts.getValue(AgentStuckEventImpl.ATTRIBUTE_PERSON)),
					Double.parseDouble(atts.getValue(AgentMoneyEventImpl.ATTRIBUTE_AMOUNT))));
		} else if (PersonEntersVehicleEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createPersonEntersVehicleEvent(time,
					new IdImpl(atts.getValue(PersonEntersVehicleEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(PersonEntersVehicleEventImpl.ATTRIBUTE_VEHICLE))));			
		} else if (PersonLeavesVehicleEventImpl.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createPersonLeavesVehicleEvent(time,
					new IdImpl(atts.getValue(PersonLeavesVehicleEventImpl.ATTRIBUTE_PERSON)),
					new IdImpl(atts.getValue(PersonLeavesVehicleEventImpl.ATTRIBUTE_VEHICLE))));
		} else if (BasicVehicleArrivesAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createVehicleArrivesAtFacilityEvent(time,
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)),
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY))));			
		} else if (BasicVehicleDepartsAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(this.builder.createVehicleDepartsAtFacilityEvent(time,
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE)),
					new IdImpl(atts.getValue(BasicVehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY))));			
		}
	}

}
