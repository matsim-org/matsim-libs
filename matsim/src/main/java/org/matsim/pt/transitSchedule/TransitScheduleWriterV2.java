/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.io.OutputStream;
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
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

/**
 * Writes a transit schedule to a XML file in the format described by <code>transitSchedule_v2.dtd</code>.
 *
 * @author mrieser
 */
public class TransitScheduleWriterV2 extends MatsimXmlWriter implements MatsimSomeWriter {

	private final CoordinateTransformation coordinateTransformation;
	private final TransitSchedule schedule;
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

	public TransitScheduleWriterV2(final TransitSchedule schedule) {
		this(new IdentityTransformation() , schedule);
	}

	public TransitScheduleWriterV2(
			final CoordinateTransformation coordinateTransformation,
			final TransitSchedule schedule) {
		this.coordinateTransformation = coordinateTransformation;
		this.schedule = schedule;
	}

	public void write(OutputStream stream) throws UncheckedIOException {
		this.openOutputStream(stream);
		try {
			writeData();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void write(final String filename) throws UncheckedIOException {
		this.openFile(filename);
		try {
			writeData();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void writeData() throws IOException, UncheckedIOException {
		this.writeXmlHead();
		this.writeDoctype(Constants.TRANSIT_SCHEDULE, "http://www.matsim.org/files/dtd/transitSchedule_v2.dtd");
		this.writeStartTag(Constants.TRANSIT_SCHEDULE, null);
		this.writer.write(NL);
		this.attributesWriter.writeAttributes( "\t" , this.writer , this.schedule.getAttributes() );

		this.writeTransitStops();
		this.writeMinimalTransferTimes();
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			writeTransitLine(line);
		}
		this.writeEndTag(Constants.TRANSIT_SCHEDULE);
		this.close();
	}

	private void writeTransitStops() throws IOException, UncheckedIOException {
		this.writeStartTag(Constants.TRANSIT_STOPS, null);

		List<Tuple<String, String>> attributes = new ArrayList<>(5);
		for (TransitStopFacility stop : this.schedule.getFacilities().values()) {
			attributes.clear();
			attributes.add(createTuple(Constants.ID, stop.getId().toString()));
			final Coord coord = this.coordinateTransformation.transform( stop.getCoord() );
			attributes.add(createTuple("x", coord.getX()));
			attributes.add(createTuple("y", coord.getY()));
			if (coord.hasZ()) {
				attributes.add(createTuple("z", coord.getZ()));
			}
			if (stop.getLinkId() != null) {
				attributes.add(createTuple("linkRefId", stop.getLinkId().toString()));
			}
			if (stop.getName() != null) {
				attributes.add(createTuple("name", stop.getName()));
			}
			if (stop.getStopAreaId() != null) {
				attributes.add(createTuple(Constants.STOP_AREA_ID, stop.getStopAreaId().toString()));
			}
			attributes.add(createTuple("isBlocking", stop.getIsBlockingLane()));
			if (AttributesUtils.isEmpty(stop.getAttributes())) {
				this.writeStartTag(Constants.STOP_FACILITY, attributes, true);
			} else {
				this.writeStartTag(Constants.STOP_FACILITY, attributes, false);
				if (!AttributesUtils.isEmpty(stop.getAttributes())) {
					this.writer.write(NL);
					this.attributesWriter.writeAttributes("\t\t\t", this.writer, stop.getAttributes());
				}
				this.writeEndTag(Constants.STOP_FACILITY);

			}
		}

		this.writeEndTag(Constants.TRANSIT_STOPS);
	}

	private void writeMinimalTransferTimes() {
		List<Tuple<String, String>> attributes = new ArrayList<>(5);
		MinimalTransferTimes.MinimalTransferTimesIterator iter = this.schedule.getMinimalTransferTimes().iterator();
		if (iter.hasNext()) {
			this.writeStartTag(Constants.MINIMAL_TRANSFER_TIMES, attributes);
			while (iter.hasNext()) {
				iter.next();
				attributes.clear();
				attributes.add(createTuple(Constants.FROM_STOP, iter.getFromStopId().toString()));
				attributes.add(createTuple(Constants.TO_STOP, iter.getToStopId().toString()));
				attributes.add(createTuple(Constants.TRANSFER_TIME, iter.getSeconds()));
				this.writeStartTag(Constants.RELATION, attributes, true);
			}
			this.writeEndTag(Constants.MINIMAL_TRANSFER_TIMES);
		}
	}

	private void writeTransitLine(final TransitLine line) throws IOException,  UncheckedIOException {
		List<Tuple<String, String>> attributes = new ArrayList<>(1);
		attributes.add(createTuple(Constants.ID, line.getId().toString()));
		if (line.getName() != null) {
			attributes.add(createTuple(Constants.NAME, line.getName()));
		}
		this.writeStartTag(Constants.TRANSIT_LINE, attributes);
		if (!AttributesUtils.isEmpty(line.getAttributes())) {
			this.writer.write(NL);
			this.attributesWriter.writeAttributes("\t\t", this.writer, line.getAttributes());
		}

		for (TransitRoute route : line.getRoutes().values()) {
			writeTransitRoute(route);
		}

		this.writeEndTag(Constants.TRANSIT_LINE);
	}

	private void writeTransitRoute(final TransitRoute route) throws IOException, UncheckedIOException {
		List<Tuple<String, String>> attributes = new ArrayList<>(1);
		attributes.add(createTuple(Constants.ID, route.getId().toString()));
		this.writeStartTag(Constants.TRANSIT_ROUTE, attributes);

		if (!AttributesUtils.isEmpty(route.getAttributes())) {
			this.writer.write(NL);
			this.attributesWriter.writeAttributes("\t\t\t", this.writer, route.getAttributes());
		}

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
		List<Tuple<String, String>> attributes = new ArrayList<>(4);
		for (TransitRouteStop stop : stops) {
			attributes.clear();
			attributes.add(createTuple(Constants.REF_ID, stop.getStopFacility().getId().toString()));
			if (stop.getArrivalOffset() != Time.UNDEFINED_TIME) {
				attributes.add(createTimeTuple(Constants.ARRIVAL_OFFSET, stop.getArrivalOffset()));
			}
			if (stop.getDepartureOffset() != Time.UNDEFINED_TIME) {
				attributes.add(createTimeTuple(Constants.DEPARTURE_OFFSET, stop.getDepartureOffset()));
			}
			attributes.add(createTuple(Constants.AWAIT_DEPARTURE, String.valueOf(stop.isAwaitDepartureTime())));
			this.writeStartTag(Constants.STOP, attributes, true);
		}

		this.writeEndTag(Constants.ROUTE_PROFILE);
	}

	private void writeRoute(final NetworkRoute route) throws UncheckedIOException {
		if (route != null) {
			this.writeStartTag(Constants.ROUTE, null);

			// optimization: only create one List for multiple departures
			List<Tuple<String, String>> attributes = new ArrayList<>(1);
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

	private void writeDepartures(final Map<Id<Departure>, Departure> departures) throws IOException, UncheckedIOException {
		this.writeStartTag(Constants.DEPARTURES, null);

		// optimization: only create one List for multiple departures
		List<Tuple<String, String>> attributes = new ArrayList<>(3);

		for (Departure dep : departures.values()) {
			attributes.clear();
			attributes.add(createTuple(Constants.ID, dep.getId().toString()));
			attributes.add(createTimeTuple(Constants.DEPARTURE_TIME, dep.getDepartureTime()));
			if (dep.getVehicleId() != null) {
				attributes.add(createTuple(Constants.VEHICLE_REF_ID, dep.getVehicleId().toString()));
			}
			if (AttributesUtils.isEmpty(dep.getAttributes())) {
				this.writeStartTag(Constants.DEPARTURE, attributes, true);
			} else {
				this.writeStartTag(Constants.DEPARTURE, attributes, false);
				this.writer.write(NL);
				this.attributesWriter.writeAttributes("\t\t\t\t\t", this.writer, dep.getAttributes());
				this.writeEndTag(Constants.DEPARTURE);
			}

		}

		this.writeEndTag(Constants.DEPARTURES);
	}
}
