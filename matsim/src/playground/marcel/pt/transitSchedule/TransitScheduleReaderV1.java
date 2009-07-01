/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReader.java
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitStopFacility;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.api.Departure;
import playground.marcel.pt.transitSchedule.api.TransitLine;
import playground.marcel.pt.transitSchedule.api.TransitRoute;
import playground.marcel.pt.transitSchedule.api.TransitRouteStop;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;

/**
 * Reads a transit schedule from a XML file in the format described by <code>transitSchedule_v1.dtd</code>.
 * 
 * @author mrieser
 */
public class TransitScheduleReaderV1 extends MatsimXmlParser {

	private static final String STOP_FACILITY = "stopFacility";
	private static final String LINK_REF_ID = "linkRefId";
	
	private static final String TRANSIT_LINE = "transitLine";
	private static final String TRANSIT_ROUTE = "transitRoute";
	private static final String DESCRIPTION = "description";
	private static final String DEPARTURE = "departure";
	private static final String ROUTE_PROFILE = "routeProfile";
	private static final String TRANSPORT_MODE = "transportMode";
	private static final String STOP = "stop";
	private static final String LINK = "link";

	private static final String ID = "id";
	private static final String REF_ID = "refId";
	private static final String ARRIVAL_OFFSET = "arrivalOffset";
	private static final String DEPARTURE_OFFSET = "departureOffset";

	private final TransitSchedule schedule;
	private final Network network;

	private TransitLine currentTransitLine = null;
	private TempTransitRoute currentTransitRoute = null;
	private TempRoute currentRouteProfile = null;

	public TransitScheduleReaderV1(final TransitSchedule schedule, final Network network) {
		this.schedule = schedule;
		this.network = network;
	}

	public void readFile(final String fileName) throws SAXException, ParserConfigurationException, IOException {
		this.parse(fileName);
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (STOP_FACILITY.equals(name)) {
			TransitStopFacility stop = new TransitStopFacility(
					new IdImpl(atts.getValue(ID)), new CoordImpl(atts.getValue("x"), atts.getValue("y")));
			if (atts.getValue(LINK_REF_ID) != null) {
				Link link = this.network.getLinks().get(new IdImpl(atts.getValue(LINK_REF_ID)));
				if (link == null) {
					throw new RuntimeException("no link with id " + atts.getValue(LINK_REF_ID));
				}
				stop.setLink(link);
			}
			this.schedule.addStopFacility(stop);
		} else if (TRANSIT_LINE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			this.currentTransitLine = new TransitLineImpl(id);
			this.schedule.addTransitLine(this.currentTransitLine);
		} else if (TRANSIT_ROUTE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			this.currentTransitRoute = new TempTransitRoute(id);
		} else if (DEPARTURE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			Departure departure = new DepartureImpl(id, Time.parseTime(atts.getValue("departureTime")));
			this.currentTransitRoute.departures.put(id, departure);
		} else if (ROUTE_PROFILE.equals(name)) {
			this.currentRouteProfile = new TempRoute();
		} else if (LINK.equals(name)) {
			Link link = this.network.getLinks().get(new IdImpl(atts.getValue(REF_ID)));
			if (link == null) {
				throw new RuntimeException("no link with id " + atts.getValue(REF_ID));
			}
			this.currentRouteProfile.addLink(link);
		} else if (STOP.equals(name)) {
			Id id = new IdImpl(atts.getValue(REF_ID));
			TransitStopFacility facility = this.schedule.getFacilities().get(id);
			if (facility == null) {
				throw new RuntimeException("no stop/facility with id " + atts.getValue(REF_ID));
			}
			TempStop stop = new TempStop(facility);
			String arrival = atts.getValue(ARRIVAL_OFFSET);
			String departure = atts.getValue(DEPARTURE_OFFSET);
			if (arrival != null) {
				stop.arrival = Time.parseTime(arrival);
			}
			if (departure != null) {
				stop.departure = Time.parseTime(departure);
			}
			this.currentTransitRoute.stops.add(stop);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (DESCRIPTION.equals(name) && TRANSIT_ROUTE.equals(context.peek())) {
			this.currentTransitRoute.description = content;
		} else if (TRANSPORT_MODE.equals(name)) {
			this.currentTransitRoute.mode = TransportMode.valueOf(content);
		} else if (TRANSIT_ROUTE.equals(name)) {
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(this.currentTransitRoute.stops.size());
			for (TempStop tStop : this.currentTransitRoute.stops) {
				stops.add(new TransitRouteStopImpl(tStop.stop, tStop.arrival, tStop.departure));
			}
			NetworkRoute route = null;
			if (this.currentRouteProfile.firstLink != null) {
				route = (NetworkRoute) this.network.getFactory().createRoute(TransportMode.car, this.currentRouteProfile.firstLink, this.currentRouteProfile.lastLink);
				route.setLinks(this.currentRouteProfile.firstLink, this.currentRouteProfile.links, this.currentRouteProfile.lastLink);
			}
			TransitRoute transitRoute = new TransitRouteImpl(this.currentTransitRoute.id, route, stops, this.currentTransitRoute.mode);
			transitRoute.setDescription(this.currentTransitRoute.description);
			for (Departure departure : this.currentTransitRoute.departures.values()) {
				transitRoute.addDeparture(departure);
			}
			this.currentTransitLine.addRoute(transitRoute);
		}
	}

	private static class TempTransitRoute {
		protected final Id id;
		protected String description = null;
		protected Map<Id, Departure> departures = new LinkedHashMap<Id, Departure>();
		/*package*/ List<TempStop> stops = new ArrayList<TempStop>();
		/*package*/ TransportMode mode = null;

		protected TempTransitRoute(final Id id) {
			this.id = id;
		}
	}

	private static class TempStop {
		protected final TransitStopFacility stop;
		protected double departure = Time.UNDEFINED_TIME;
		protected double arrival = Time.UNDEFINED_TIME;

		protected TempStop(final TransitStopFacility stop) {
			this.stop = stop;
		}
	}

	private static class TempRoute {
		/*package*/ List<Link> links = new ArrayList<Link>();
		/*package*/ Link firstLink = null;
		/*package*/ Link lastLink = null;

		protected TempRoute() {
			// public constructor for private inner class
		}

		protected void addLink(final Link link) {
			if (this.firstLink == null) {
				this.firstLink = link;
			} else if (this.lastLink == null) {
				this.lastLink = link;
			} else {
				this.links.add(this.lastLink);
				this.lastLink = link;
			}
		}

	}

}
