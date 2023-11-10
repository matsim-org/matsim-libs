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

package org.matsim.pt.transitSchedule;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Writes a transit schedule to a XML file in the format described by <code>transitSchedule_v1.dtd</code>.
 *
 * @author mrieser
 */
public class TransitScheduleWriterV1 extends MatsimXmlWriter implements MatsimSomeWriter {

	private final CoordinateTransformation coordinateTransformation;

	private final TransitSchedule schedule;

	public TransitScheduleWriterV1(final TransitSchedule schedule) {
		this( new IdentityTransformation() , schedule );
	}

	public TransitScheduleWriterV1(
			final CoordinateTransformation coordinateTransformation,
			final TransitSchedule schedule) {
		this.coordinateTransformation = coordinateTransformation;
		this.schedule = schedule;
	}

	public void write(final String filename) throws UncheckedIOException {
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

	private void writeTransitStops() throws UncheckedIOException {
		this.writeStartTag(Constants.TRANSIT_STOPS, null);

		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(5);
		for (TransitStopFacility stop : this.schedule.getFacilities().values()) {
			attributes.clear();
			attributes.add(createTuple(Constants.ID, stop.getId().toString()));
			final Coord coord = coordinateTransformation.transform( stop.getCoord() );
			attributes.add(createTuple("x", coord.getX()));
			attributes.add(createTuple("y", coord.getY()));
			if (stop.getLinkId() != null) {
				attributes.add(createTuple("linkRefId", stop.getLinkId().toString()));
			}
			if (stop.getName() != null) {
				attributes.add(createTuple("name", stop.getName()));
			}
			attributes.add(createTuple("isBlocking", stop.getIsBlockingLane()));
			this.writeStartTag(Constants.STOP_FACILITY, attributes, true);
		}

		this.writeEndTag(Constants.TRANSIT_STOPS);
	}

	private void writeTransitLine(final TransitLine line) throws UncheckedIOException {
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
		attributes.add(createTuple(Constants.ID, line.getId().toString()));
		if (line.getName() != null) {
			attributes.add(createTuple(Constants.NAME, line.getName()));
		}
		this.writeStartTag(Constants.TRANSIT_LINE, attributes);

		for (TransitRoute route : line.getRoutes().values()) {
			writeTransitRoute(route);
		}

		this.writeEndTag(Constants.TRANSIT_LINE);
	}

	private void writeTransitRoute(final TransitRoute route) throws UncheckedIOException {
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
		attributes.add(createTuple(Constants.ID, route.getId().toString()));
		this.writeStartTag(Constants.TRANSIT_ROUTE, attributes);

		if (route.getDescription() != null) {
			this.writeStartTag(Constants.DESCRIPTION, null);
			this.writeContent(route.getDescription(), false);
			this.writeEndTag(Constants.DESCRIPTION);
		}

		this.writeStartTag(Constants.TRANSPORT_MODE, null);
		this.writeContent(route.getTransportMode(), false);
		this.writeEndTag(Constants.TRANSPORT_MODE);

		this.writeRouteProfile(route.getStops());
		this.writeRoute(route.getRoute());
		this.writeDepartures(route.getDepartures());

		this.writeEndTag(Constants.TRANSIT_ROUTE);
	}

	private void writeRouteProfile(final List<TransitRouteStop> stops) throws UncheckedIOException {
		this.writeStartTag(Constants.ROUTE_PROFILE, null);

		// optimization: only create one List for multiple departures
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(4);
		for (TransitRouteStop stop : stops) {
			attributes.clear();
			attributes.add(createTuple(Constants.REF_ID, stop.getStopFacility().getId().toString()));
			stop.getArrivalOffset()
					.ifDefined(offset -> attributes.add(createTimeTuple(Constants.ARRIVAL_OFFSET, offset)));
			stop.getDepartureOffset().ifDefined(offset->
				attributes.add(createTimeTuple(Constants.DEPARTURE_OFFSET, offset)));
			attributes.add(createTuple(Constants.AWAIT_DEPARTURE, String.valueOf(stop.isAwaitDepartureTime())));
			this.writeStartTag(Constants.STOP, attributes, true);
		}

		this.writeEndTag(Constants.ROUTE_PROFILE);
	}

	private void writeRoute(final NetworkRoute route) throws UncheckedIOException {
		if (route != null) {
			this.writeStartTag(Constants.ROUTE, null);

			// optimization: only create one List for multiple departures
			List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
			attributes.add(createTuple(Constants.REF_ID, route.getStartLinkId().toString()));
			this.writeStartTag(Constants.LINK, attributes, true);

			for (Id<Link> linkId : route.getLinkIds()) {
				attributes.clear();
				attributes.add(createTuple(Constants.REF_ID, linkId.toString()));
				this.writeStartTag(Constants.LINK, attributes, true);
			}

			attributes.clear();
			attributes.add(createTuple(Constants.REF_ID, route.getEndLinkId().toString()));
			this.writeStartTag(Constants.LINK, attributes, true);

			this.writeEndTag(Constants.ROUTE);
		}
	}

	private void writeDepartures(final Map<Id<Departure>, Departure> departures) throws UncheckedIOException {
		this.writeStartTag(Constants.DEPARTURES, null);

		// optimization: only create one List for multiple departures
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(3);

		for (Departure dep : departures.values()) {
			attributes.clear();
			attributes.add(createTuple(Constants.ID, dep.getId().toString()));
			attributes.add(createTimeTuple(Constants.DEPARTURE_TIME, dep.getDepartureTime()));
			if (dep.getVehicleId() != null) {
				attributes.add(createTuple(Constants.VEHICLE_REF_ID, dep.getVehicleId().toString()));
			}
			this.writeStartTag(Constants.DEPARTURE, attributes, true);
		}

		this.writeEndTag(Constants.DEPARTURES);
	}
}
