/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.HasFacilityId;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonInitializedEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.core.events.MatsimEventsReader.CustomEventMapper;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.ZstdInputStream;

/**
 * @author mrieser / Simunto GmbH
 */
public final class EventsReaderJson {

	private final static Logger LOG = LogManager.getLogger(EventsReaderJson.class);
	private final static ObjectMapper MAPPER = new ObjectMapper();

	private final EventsManager events;
	private final Map<String, CustomEventMapper> customEventMappers = new HashMap<>();

	public EventsReaderJson(final EventsManager events) {
		this.events = events;
	}

	public void addCustomEventMapper(String eventType, CustomEventMapper cem) {
		this.customEventMappers.put(eventType, cem);
	}

	void parse(final String filename) throws UncheckedIOException {
		parse(IOUtils.getBufferedReader(filename), filename);
	}

	void parse(final InputStream stream) throws UncheckedIOException {
		parse(new BufferedReader(new InputStreamReader(stream)), "stream");
	}

	void parse(URL url) throws UncheckedIOException {
		LOG.info("starting to parse json from url " + url + " ...");
		try (InputStream urlStream = url.openStream()) {
			InputStream inStream = urlStream;
			if (url.getFile().endsWith(".gz")) {
				inStream = new GZIPInputStream(urlStream);
			}
			if (url.getFile().endsWith(".zst")) {
				inStream = new ZstdInputStream(urlStream);
			}
			parse(inStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void parse(final BufferedReader in, final String filename) throws UncheckedIOException {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				parseLine(line);
			}
			try {
				in.close();
			}
			catch (IOException e) {
				LOG.error("Could not close file: " + filename, e);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void parseLine(final String line) throws JsonProcessingException, IOException {
		JsonNode o = MAPPER.reader().readTree(line);
		parseEvent(o);
	}

	private void parseEvent(JsonNode o) {
		String eventType = o.get("type").asText();
		double time = o.get("time").asDouble();

		// === material related to wait2link below here ===
		if (LinkLeaveEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkLeaveEvent(time,
					Id.create(o.get(LinkLeaveEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class),
					Id.create(o.get(LinkLeaveEvent.ATTRIBUTE_LINK).asText(), Link.class)
					// had driver id in previous version
					));
		} else if (LinkEnterEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new LinkEnterEvent(time,
					Id.create(o.get(LinkEnterEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class),
					Id.create(o.get(LinkEnterEvent.ATTRIBUTE_LINK).asText(), Link.class)
					// had driver id in previous version
					));
		} else if (VehicleEntersTrafficEvent.EVENT_TYPE.equals(eventType)) {
			// (this is the new version, marked by the new events name)

			this.events.processEvent(new VehicleEntersTrafficEvent(time,
					Id.create(o.get(HasPersonId.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(VehicleEntersTrafficEvent.ATTRIBUTE_LINK).asText(), Link.class),
					Id.create(o.get(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class),
					o.get(VehicleEntersTrafficEvent.ATTRIBUTE_NETWORKMODE).asText(),
					o.get(VehicleEntersTrafficEvent.ATTRIBUTE_POSITION).asDouble()
					));
		} else if ("wait2link".equals(eventType)) {
			// (this is the old version, marked by the old events name)

			// retrofit vehicle Id:
			Id<Vehicle> vehicleId;
			if (o.has(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE)) {
				vehicleId = Id.create(o.get(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class);
			} else {
				// for the old events type, we set the vehicle id to the driver id if the vehicle id does not exist:
				vehicleId = Id.create(o.get(HasPersonId.ATTRIBUTE_PERSON).asText(), Vehicle.class);
			}
			// retrofit position:
			double position = o.path(VehicleEntersTrafficEvent.ATTRIBUTE_POSITION).asDouble(1.0);
			this.events.processEvent(new VehicleEntersTrafficEvent(time,
					Id.create(o.get(HasPersonId.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(VehicleEntersTrafficEvent.ATTRIBUTE_LINK).asText(), Link.class),
					vehicleId,
					o.get(VehicleEntersTrafficEvent.ATTRIBUTE_NETWORKMODE).asText(),
					position
					));
		} else if (VehicleLeavesTrafficEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new VehicleLeavesTrafficEvent(time,
					Id.create(o.get(VehicleLeavesTrafficEvent.ATTRIBUTE_DRIVER).asText(), Person.class),
					Id.create(o.get(VehicleLeavesTrafficEvent.ATTRIBUTE_LINK).asText(), Link.class),
					o.has(VehicleLeavesTrafficEvent.ATTRIBUTE_VEHICLE) ? Id.create(o.get(VehicleLeavesTrafficEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class) : null,
					o.get(VehicleLeavesTrafficEvent.ATTRIBUTE_NETWORKMODE).asText(),
					o.get(VehicleLeavesTrafficEvent.ATTRIBUTE_POSITION).asDouble()
					));
		}
		// === material related to wait2link above here
		else if (ActivityEndEvent.EVENT_TYPE.equals(eventType)) {
			Coord coord = null;
			if (o.has(Event.ATTRIBUTE_X)) {
				double xx = o.get(Event.ATTRIBUTE_X).asDouble();
				double yy = o.get(Event.ATTRIBUTE_Y).asDouble();
				coord = new Coord(xx, yy);
			}
			this.events.processEvent(new ActivityEndEvent(
					time,
					Id.create(o.get(HasPersonId.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(HasLinkId.ATTRIBUTE_LINK).asText(), Link.class),
					o.has(HasFacilityId.ATTRIBUTE_FACILITY) ? Id.create(o.get(HasFacilityId.ATTRIBUTE_FACILITY).asText(), ActivityFacility.class) : null,
					o.get(ActivityEndEvent.ATTRIBUTE_ACTTYPE).asText(),
					coord));
		} else if (ActivityStartEvent.EVENT_TYPE.equals(eventType)) {
			Coord coord = null;
			if (o.has(Event.ATTRIBUTE_X)) {
				double xx = o.get(Event.ATTRIBUTE_X).asDouble();
				double yy = o.get(Event.ATTRIBUTE_Y).asDouble();
				coord = new Coord(xx, yy);
			}
			try {
				this.events.processEvent(new ActivityStartEvent(
					time,
					Id.create(o.get(HasPersonId.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(HasLinkId.ATTRIBUTE_LINK).asText(), Link.class),
					o.has(HasFacilityId.ATTRIBUTE_FACILITY) ? Id.create(o.get(HasFacilityId.ATTRIBUTE_FACILITY).asText(), ActivityFacility.class) : null,
					o.get(ActivityStartEvent.ATTRIBUTE_ACTTYPE).asText(),
					coord));
			} catch (NullPointerException e) {
				e.printStackTrace();
				boolean hasFacility = o.has(HasFacilityId.ATTRIBUTE_FACILITY);
				this.events.processEvent(new ActivityStartEvent(
					time,
					Id.create(o.get(HasPersonId.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(HasLinkId.ATTRIBUTE_LINK).asText(), Link.class),
					hasFacility ? Id.create(o.get(HasFacilityId.ATTRIBUTE_FACILITY).asText(), ActivityFacility.class) : null,
					o.get(ActivityStartEvent.ATTRIBUTE_ACTTYPE).asText(),
					coord));
			}
	} else if (PersonArrivalEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = o.path(PersonArrivalEvent.ATTRIBUTE_LEGMODE).asText(null);
			String mode = legMode == null ? null : legMode.intern();
			this.events.processEvent(new PersonArrivalEvent(
					time,
					Id.create(o.get(PersonArrivalEvent.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(PersonArrivalEvent.ATTRIBUTE_LINK).asText(), Link.class),
					mode));
		} else if (PersonDepartureEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = o.path(PersonDepartureEvent.ATTRIBUTE_LEGMODE).asText(null);
			String canonicalLegMode = legMode == null ? null : legMode.intern();
			String routingMode = o.path(PersonDepartureEvent.ATTRIBUTE_ROUTING_MODE).asText(null);
			String canonicalRoutingMode = routingMode == null ? null : routingMode.intern();
			this.events.processEvent(new PersonDepartureEvent(
					time,
					Id.create(o.get(PersonDepartureEvent.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(PersonDepartureEvent.ATTRIBUTE_LINK).asText(), Link.class),
					canonicalLegMode, canonicalRoutingMode));
		} else if (PersonStuckEvent.EVENT_TYPE.equals(eventType)) {
			String legMode = o.path(PersonStuckEvent.ATTRIBUTE_LEGMODE).asText(null);
			String mode = legMode == null ? null : legMode.intern();
			String linkIdString = o.path(PersonStuckEvent.ATTRIBUTE_LINK).asText(null);
			Id<Link> linkId = linkIdString == null ? null : Id.create(linkIdString, Link.class); // linkId is optional
			this.events.processEvent(new PersonStuckEvent(
					time,
					Id.create(o.get(PersonStuckEvent.ATTRIBUTE_PERSON).asText(), Person.class),
					linkId,
					mode));
		} else if (VehicleAbortsEvent.EVENT_TYPE.equals(eventType)) {
			String linkIdString = o.path(VehicleAbortsEvent.ATTRIBUTE_LINK).asText(null);
			Id<Link> linkId = linkIdString == null ? null : Id.create(linkIdString, Link.class);
			this.events.processEvent(new VehicleAbortsEvent(
					time,
					Id.create(o.get(VehicleAbortsEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class),
					linkId));
		} else if (PersonMoneyEvent.EVENT_TYPE.equals(eventType) || "agentMoney".equals(eventType)) {
			this.events.processEvent(new PersonMoneyEvent(time, Id.create(o.get(PersonMoneyEvent.ATTRIBUTE_PERSON).asText(), Person.class), o.get(PersonMoneyEvent.ATTRIBUTE_AMOUNT).asDouble(), o.path(PersonMoneyEvent.ATTRIBUTE_PURPOSE).asText(null), o.path(PersonMoneyEvent.ATTRIBUTE_TRANSACTION_PARTNER).asText(null), null));
		} else if (PersonScoreEvent.EVENT_TYPE.equals(eventType) || "personScore".equals(eventType)) {
			this.events.processEvent(new PersonScoreEvent(
					time,
					Id.create(o.get(PersonScoreEvent.ATTRIBUTE_PERSON).asText(), Person.class),
					o.get(PersonScoreEvent.ATTRIBUTE_AMOUNT).asDouble(),
					o.path(PersonScoreEvent.ATTRIBUTE_KIND).asText(null)));
		} else if (PersonEntersVehicleEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new PersonEntersVehicleEvent(
					time,
					Id.create(o.get(PersonEntersVehicleEvent.ATTRIBUTE_PERSON).asText(), Person.class),
					Id.create(o.get(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class)));
		} else if (PersonLeavesVehicleEvent.EVENT_TYPE.equals(eventType)) {
			Id<Person> pId = Id.create(o.get(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON).asText(), Person.class);
			Id<Vehicle> vId = Id.create(o.get(PersonLeavesVehicleEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class);
			this.events.processEvent(new PersonLeavesVehicleEvent(time, pId, vId));
		} else if (TeleportationArrivalEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new TeleportationArrivalEvent(
					time,
					Id.create(o.get(TeleportationArrivalEvent.ATTRIBUTE_PERSON).asText(), Person.class),
					o.get(TeleportationArrivalEvent.ATTRIBUTE_DISTANCE).asDouble(),
					o.path(TeleportationArrivalEvent.ATTRIBUTE_MODE).asText(null)));
		} else if (VehicleArrivesAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			double delay = o.path(VehicleArrivesAtFacilityEvent.ATTRIBUTE_DELAY).asDouble(0.0);
			this.events.processEvent(new VehicleArrivesAtFacilityEvent(
					time,
					Id.create(o.get(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class),
					Id.create(o.get(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY).asText(), TransitStopFacility.class),
					delay));
		} else if (VehicleDepartsAtFacilityEvent.EVENT_TYPE.equals(eventType)) {
			double delay = o.get(VehicleDepartsAtFacilityEvent.ATTRIBUTE_DELAY).asDouble(0.0);
			this.events.processEvent(new VehicleDepartsAtFacilityEvent(
					time,
					Id.create(o.get(VehicleArrivesAtFacilityEvent.ATTRIBUTE_VEHICLE).asText(), Vehicle.class),
					Id.create(o.get(VehicleArrivesAtFacilityEvent.ATTRIBUTE_FACILITY).asText(), TransitStopFacility.class),
					delay));
		} else if (TransitDriverStartsEvent.EVENT_TYPE.equals(eventType)) {
			this.events.processEvent(new TransitDriverStartsEvent(
					time,
					Id.create(o.get(TransitDriverStartsEvent.ATTRIBUTE_DRIVER_ID).asText(), Person.class),
					Id.create(o.get(TransitDriverStartsEvent.ATTRIBUTE_VEHICLE_ID).asText(), Vehicle.class),
					Id.create(o.get(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_LINE_ID).asText(), TransitLine.class),
					Id.create(o.get(TransitDriverStartsEvent.ATTRIBUTE_TRANSIT_ROUTE_ID).asText(), TransitRoute.class),
					Id.create(o.get(TransitDriverStartsEvent.ATTRIBUTE_DEPARTURE_ID).asText(), Departure.class)));
		} else if (BoardingDeniedEvent.EVENT_TYPE.equals(eventType)){
			Id<Person> personId = Id.create(o.get(BoardingDeniedEvent.ATTRIBUTE_PERSON_ID).asText(), Person.class);
			Id<Vehicle> vehicleId = Id.create(o.get(BoardingDeniedEvent.ATTRIBUTE_VEHICLE_ID).asText(), Vehicle.class);
			this.events.processEvent(new BoardingDeniedEvent(time, personId, vehicleId));
		} else if (AgentWaitingForPtEvent.EVENT_TYPE.equals(eventType)){
			Id<Person> agentId = Id.create(o.get(AgentWaitingForPtEvent.ATTRIBUTE_AGENT).asText(), Person.class);
			Id<TransitStopFacility> waitStopId = Id.create(o.get(AgentWaitingForPtEvent.ATTRIBUTE_WAITSTOP).asText(), TransitStopFacility.class);
			Id<TransitStopFacility> destinationStopId = Id.create(o.get(AgentWaitingForPtEvent.ATTRIBUTE_DESTINATIONSTOP).asText(), TransitStopFacility.class);
			this.events.processEvent(new AgentWaitingForPtEvent(time, agentId, waitStopId, destinationStopId));
		} else if (PersonInitializedEvent.EVENT_TYPE.equals(eventType)){
			Id<Person> personId = Id.create(o.get(PersonInitializedEvent.ATTRIBUTE_PERSON).asText(), Person.class);
			Coord coord = null;
			if (o.get(Event.ATTRIBUTE_X ) != null) {
				double xx = o.get(Event.ATTRIBUTE_X).asDouble();
				double yy = o.get(Event.ATTRIBUTE_Y).asDouble();
				coord = new Coord(xx, yy) ;
			}
			this.events.processEvent(new PersonInitializedEvent(time, personId, coord));
		} else {
			GenericEvent event = new GenericEvent(eventType, time);

			Iterator<Map.Entry<String, JsonNode>> iter = o.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> e = iter.next();
				String key = e.getKey();
				if (key.equals("time") || key.equals("type")) {
					continue;
				}
				String value = e.getValue().asText(null);
				event.getAttributes().put(key, value);
			}
			CustomEventMapper cem = this.customEventMappers.get(eventType);
			if (cem != null) {
				this.events.processEvent(cem.apply(event));
			} else {
				this.events.processEvent(event);
			}
		}
	}

}
