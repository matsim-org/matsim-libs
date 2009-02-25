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

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TransitScheduleReader extends MatsimXmlParser {

	private static final String TRANSIT_LINE = "transitLine";
	private static final String TRANSIT_ROUTE = "transitRoute";
	private static final String DESCRIPTION = "description";
	private static final String DEPARTURE = "departure";
	private static final String ROUTE_PROFILE = "routeProfile";
	private static final String STOP = "stop";
	private static final String LINK = "link";

	private static final String ID = "id";
	private static final String REF = "ref";

	private final TransitSchedule schedule;
	private final NetworkLayer network;
	private final Facilities facilities;

	private TransitLine currentTransitLine = null;
	private TempTransitRoute currentTransitRoute = null;
	private TempRouteProfile currentRouteProfile = null;

	public TransitScheduleReader(final TransitSchedule schedule, final NetworkLayer network, final Facilities facilities) {
		this.schedule = schedule;
		this.network = network;
		this.facilities = facilities;
	}

	public void readFile(final String fileName) throws SAXException, ParserConfigurationException, IOException {
		this.setValidating(false);
		this.parse(fileName);
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (TRANSIT_LINE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			this.currentTransitLine = new TransitLine(id);
			this.schedule.addTransitLine(id, this.currentTransitLine);
		} else if (TRANSIT_ROUTE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			this.currentTransitRoute = new TempTransitRoute(id);
		} else if (DEPARTURE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			Departure departure = new Departure(id, Time.parseTime(atts.getValue("departureTime")));
			this.currentTransitRoute.departures.put(id, departure);
		} else if (ROUTE_PROFILE.equals(name)) {
			this.currentRouteProfile = new TempRouteProfile();
		} else if (STOP.equals(name)) {
			Id id = new IdImpl(atts.getValue(REF));
			Facility facility = this.facilities.getFacility(id);
			if (facility == null) {
				throw new RuntimeException("no stop/facility with id " + atts.getValue(ID));
			}
			TempStop stop = new TempStop(facility);
			String arrival = atts.getValue("arrival");
			String departure = atts.getValue("departure");
			if (arrival != null) {
				stop.arrival = Time.parseTime(arrival);
			}
			if (departure != null) {
				stop.departure = Time.parseTime(departure);
			}
			this.currentRouteProfile.addStop(stop);
		} else if (LINK.equals(name)) {
			Link link = this.network.getLink(atts.getValue(REF));
			if (link == null) {
				throw new RuntimeException("no link with id " + atts.getValue(REF));
			}
			this.currentRouteProfile.addLink(link);
		}

	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (DESCRIPTION.equals(name) && TRANSIT_ROUTE.equals(context.peek())) {
			this.currentTransitRoute.description = content;
		} else if (TRANSIT_ROUTE.equals(name)) {
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(this.currentRouteProfile.stops.size());
			for (TempStop tStop : this.currentRouteProfile.stops) {
				stops.add(new TransitRouteStop(tStop.stop, tStop.departure, tStop.arrival));
			}
			CarRoute route = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car, this.currentRouteProfile.firstLink, this.currentRouteProfile.lastLink);
			route.setLinks(this.currentRouteProfile.firstLink, this.currentRouteProfile.links, this.currentRouteProfile.lastLink);
			TransitRoute transitRoute = new TransitRoute(this.currentTransitRoute.id, route, stops);
			transitRoute.setDescription(this.currentTransitRoute.description);
			for (Map.Entry<Id, Departure> entry : this.currentTransitRoute.departures.entrySet()) {
				transitRoute.addDeparture(entry.getKey(), entry.getValue());
			}
			this.currentTransitLine.addRoute(this.currentTransitRoute.id, transitRoute);
		}
	}

	private static class TempTransitRoute {
		public final Id id;
		public String description = null;
		public Map<Id, Departure> departures = new LinkedHashMap<Id, Departure>();

		public TempTransitRoute(final Id id) {
			this.id = id;
		}
	}

	private static class TempStop {
		public final Facility stop;
		public double departure = Time.UNDEFINED_TIME;
		public double arrival = Time.UNDEFINED_TIME;

		public TempStop(final Facility stop) {
			this.stop = stop;
		}
	}

	private static class TempRouteProfile {

		/*package*/ List<TempStop> stops = new ArrayList<TempStop>();
		/*package*/ List<Link> links = new ArrayList<Link>();
		/*package*/ Link firstLink = null;
		/*package*/ Link lastLink = null;
		private Link prevLink = null;

		public TempRouteProfile() {
			// public constructor for private inner class
		}

		public void addStop(final TempStop stop) {
			this.stops.add(stop);
			this.addLink(stop.stop.getLink());
		}

		public void addLink(final Link link) {
			if (this.prevLink != link) {
				if (this.firstLink == null) {
					this.firstLink = link;
				} else if (this.lastLink == null) {
					this.lastLink = link;
				} else {
					this.links.add(this.lastLink);
					this.lastLink = link;
				}
				this.prevLink = link;
			}
		}

	}

}
