/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.transitSchedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * Writes a transit schedule to a XML file in the format described by <code>transitSchedule_v1.dtd</code>.
 *
 * @author mrieser
 */
public class TransitScheduleWriterV1 extends MatsimXmlWriter {


	private final TransitSchedule schedule;

	public TransitScheduleWriterV1(final TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public void write(final String filename) throws IOException {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeDoctype(Constants.TRANSIT_SCHEDULE, "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd");
		this.writeStartTag(Constants.TRANSIT_SCHEDULE, null);

		this.writeTransitStops();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			writeTransitLine(line);
		}
		this.writeEndTag(Constants.TRANSIT_SCHEDULE);
		this.close();
	}

	private void writeTransitStops() throws IOException {
		this.writeStartTag(Constants.TRANSIT_STOPS, null);

		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(5);
		for (TransitStopFacility stop : this.schedule.getFacilities().values()) {
			attributes.clear();
			attributes.add(this.createTuple(Constants.ID, stop.getId().toString()));
			attributes.add(this.createTuple("x", stop.getCoord().getX()));
			attributes.add(this.createTuple("y", stop.getCoord().getY()));
			if (stop.getLinkId() != null) {
				attributes.add(this.createTuple("linkRefId", stop.getLinkId().toString()));
			}
			if (stop.getName() != null) {
				attributes.add(this.createTuple("name", stop.getName()));
			}
			attributes.add(this.createTuple("isBlocking", stop.getIsBlockingLane()));
			this.writeStartTag(Constants.STOP_FACILITY, attributes, true);
		}

		this.writeEndTag(Constants.TRANSIT_STOPS);
	}

	private void writeTransitLine(final TransitLine line) throws IOException {
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
		attributes.add(this.createTuple(Constants.ID, line.getId().toString()));
		this.writeStartTag(Constants.TRANSIT_LINE, attributes);

		for (TransitRoute route : line.getRoutes().values()) {
			writeTransitRoute(route);
		}

		this.writeEndTag(Constants.TRANSIT_LINE);
	}

	private void writeTransitRoute(final TransitRoute route) throws IOException {
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
		attributes.add(this.createTuple(Constants.ID, route.getId().toString()));
		this.writeStartTag(Constants.TRANSIT_ROUTE, attributes);

		if (route.getDescription() != null) {
			this.writeStartTag(Constants.DESCRIPTION, null);
			this.writeContent(route.getDescription(), false);
			this.writeEndTag(Constants.DESCRIPTION);
		}

		this.writeStartTag(Constants.TRANSPORT_MODE, null);
		this.writeContent(route.getTransportMode().toString(), false);
		this.writeEndTag(Constants.TRANSPORT_MODE);

		this.writeRouteProfile(route.getStops());
		this.writeRoute(route.getRoute());
		this.writeDepartures(route.getDepartures());

		this.writeEndTag(Constants.TRANSIT_ROUTE);
	}

	private void writeRouteProfile(final List<TransitRouteStop> stops) throws IOException {
		this.writeStartTag(Constants.ROUTE_PROFILE, null);

		// optimization: only create one List for multiple departures
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(4);
		for (TransitRouteStop stop : stops) {
			attributes.clear();
			attributes.add(this.createTuple(Constants.REF_ID, stop.getStopFacility().getId().toString()));
			if (stop.getArrivalOffset() != Time.UNDEFINED_TIME) {
				attributes.add(this.createTimeTuple(Constants.ARRIVAL_OFFSET, stop.getArrivalOffset()));
			}
			if (stop.getDepartureOffset() != Time.UNDEFINED_TIME) {
				attributes.add(this.createTimeTuple(Constants.DEPARTURE_OFFSET, stop.getDepartureOffset()));
			}
			attributes.add(this.createTuple(Constants.AWAIT_DEPARTURE, String.valueOf(stop.isAwaitDepartureTime())));
			this.writeStartTag(Constants.STOP, attributes, true);
		}

		this.writeEndTag(Constants.ROUTE_PROFILE);
	}

	private void writeRoute(final NetworkRouteWRefs route) throws IOException {
		if (route != null) {
			this.writeStartTag(Constants.ROUTE, null);

			// optimization: only create one List for multiple departures
			List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
			attributes.add(this.createTuple(Constants.REF_ID, route.getStartLinkId().toString()));
			this.writeStartTag(Constants.LINK, attributes, true);

			for (Id linkId : route.getLinkIds()) {
				attributes.clear();
				attributes.add(this.createTuple(Constants.REF_ID, linkId.toString()));
				this.writeStartTag(Constants.LINK, attributes, true);
			}

			attributes.clear();
			attributes.add(this.createTuple(Constants.REF_ID, route.getEndLinkId().toString()));
			this.writeStartTag(Constants.LINK, attributes, true);

			this.writeEndTag(Constants.ROUTE);
		}
	}

	private void writeDepartures(final Map<Id, Departure> departures) throws IOException {
		this.writeStartTag(Constants.DEPARTURES, null);

		// optimization: only create one List for multiple departures
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(3);

		for (Departure dep : departures.values()) {
			attributes.clear();
			attributes.add(this.createTuple(Constants.ID, dep.getId().toString()));
			attributes.add(this.createTimeTuple(Constants.DEPARTURE_TIME, dep.getDepartureTime()));
			if (dep.getVehicleId() != null) {
				attributes.add(this.createTuple(Constants.VEHICLE_REF_ID, dep.getVehicleId().toString()));
			}
			this.writeStartTag(Constants.DEPARTURE, attributes, true);
		}

		this.writeEndTag(Constants.DEPARTURES);
	}
}
