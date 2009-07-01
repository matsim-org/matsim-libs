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

package playground.marcel.pt.transitSchedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitStopFacility;

/**
 * Writes a transit schedule to a XML file in the format described by <code>transitSchedule_v1.dtd</code>.
 * 
 * @author mrieser
 */
public class TransitScheduleWriterV1 extends MatsimXmlWriter {

	private static final String TRANSIT_STOPS = "transitStops";
	private static final String STOP_FACILITY = "stopFacility";
	private static final String TRANSIT_SCHEDULE = "transitSchedule";
	private static final String TRANSIT_LINE = "transitLine";
	private static final String TRANSIT_ROUTE = "transitRoute";
	private static final String DESCRIPTION = "description";
	private static final String ROUTE_PROFILE = "routeProfile";
	private static final String STOP = "stop";
	private static final String ROUTE = "route";
	private static final String LINK = "link";
	private static final String DEPARTURES = "departures";
	private static final String DEPARTURE = "departure";
	private static final String ID = "id";
	private static final String REF_ID = "refId";
	private static final String TRANSPORT_MODE = "transportMode";
	private static final String DEPARTURE_TIME = "departureTime";
	private static final String DEPARTURE_OFFSET = "departureOffset";
	private static final String ARRIVAL_OFFSET = "arrivalOffset";

	private final TransitScheduleImpl schedule;

	public TransitScheduleWriterV1(final TransitScheduleImpl schedule) {
		this.schedule = schedule;
	}

	public void write(final String filename) throws IOException {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeDoctype(TRANSIT_SCHEDULE, "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd");
		this.writeStartTag(TRANSIT_SCHEDULE, null);

		this.writeTransitStops();
		for (TransitLineImpl line : this.schedule.getTransitLines().values()) {
			writeTransitLine(line);
		}
		this.writeEndTag(TRANSIT_SCHEDULE);
		this.close();
	}
	
	private void writeTransitStops() throws IOException {
		this.writeStartTag(TRANSIT_STOPS, null);
		
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(4);
		for (TransitStopFacility stop : this.schedule.getFacilities().values()) {
			attributes.clear();
			attributes.add(this.createTuple(ID, stop.getId().toString()));
			attributes.add(this.createTuple("x", stop.getCoord().getX()));
			attributes.add(this.createTuple("y", stop.getCoord().getY()));
			if (stop.getLink() != null) {
				attributes.add(this.createTuple("linkRefId", stop.getLinkId().toString()));
			}
			this.writeStartTag(STOP_FACILITY, attributes, true);
		}
		
		this.writeEndTag(TRANSIT_STOPS);
	}

	private void writeTransitLine(final TransitLineImpl line) throws IOException {
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
		attributes.add(this.createTuple(ID, line.getId().toString()));
		this.writeStartTag(TRANSIT_LINE, attributes);

		for (TransitRouteImpl route : line.getRoutes().values()) {
			writeTransitRoute(route);
		}

		this.writeEndTag(TRANSIT_LINE);

	}

	private void writeTransitRoute(final TransitRouteImpl route) throws IOException {
		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
		attributes.add(this.createTuple(ID, route.getId().toString()));
		this.writeStartTag(TRANSIT_ROUTE, attributes);

		if (route.getDescription() != null) {
			this.writeStartTag(DESCRIPTION, null);
			this.writeContent(route.getDescription(), false);
			this.writeEndTag(DESCRIPTION);
		}
		
		this.writeStartTag(TRANSPORT_MODE, null);
		this.writeContent(route.getTransportMode().toString(), false);
		this.writeEndTag(TRANSPORT_MODE);

		this.writeRouteProfile(route.getStops());
		this.writeRoute(route.getRoute());
		this.writeDepartures(route.getDepartures());

		this.writeEndTag(TRANSIT_ROUTE);
	}

	private void writeRouteProfile(final List<TransitRouteStopImpl> stops) throws IOException {
		if (stops != null) {
			this.writeStartTag(ROUTE_PROFILE, null);

			// optimization: only create one List for multiple departures
			List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(3);
			for (TransitRouteStopImpl stop : stops) {
				attributes.clear();
				attributes.add(this.createTuple(REF_ID, stop.getStopFacility().getId().toString()));
				if (stop.getArrivalDelay() != Time.UNDEFINED_TIME) {
					attributes.add(this.createTimeTuple(ARRIVAL_OFFSET, stop.getArrivalDelay()));
				}
				if (stop.getDepartureDelay() != Time.UNDEFINED_TIME) {
					attributes.add(this.createTimeTuple(DEPARTURE_OFFSET, stop.getDepartureDelay()));
				}
				this.writeStartTag(STOP, attributes, true);
			}

			this.writeEndTag(ROUTE_PROFILE);
		}
	}

	private void writeRoute(final NetworkRoute route) throws IOException {
		if (route != null) {
			this.writeStartTag(ROUTE, null);

			// optimization: only create one List for multiple departures
			List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(1);
			attributes.add(this.createTuple(REF_ID, route.getStartLink().getId().toString()));
			this.writeStartTag(LINK, attributes, true);

			for (Link link : route.getLinks()) {
				attributes.clear();
				attributes.add(this.createTuple(REF_ID, link.getId().toString()));
				this.writeStartTag(LINK, attributes, true);
			}

			attributes.clear();
			attributes.add(this.createTuple(REF_ID, route.getEndLink().getId().toString()));
			this.writeStartTag(LINK, attributes, true);

			this.writeEndTag(ROUTE);
		}
	}

	private void writeDepartures(final Map<Id, DepartureImpl> departures) throws IOException {
		if (departures != null) {
			this.writeStartTag(DEPARTURES, null);

			// optimization: only create one List for multiple departures
			List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(2);

			for (DepartureImpl dep : departures.values()) {
				attributes.clear();
				attributes.add(this.createTuple(ID, dep.getId().toString()));
				attributes.add(this.createTimeTuple(DEPARTURE_TIME, dep.getDepartureTime()));
				this.writeStartTag(DEPARTURE, attributes, true);
			}

			this.writeEndTag(DEPARTURES);
		}
	}
}
