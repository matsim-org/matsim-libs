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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;

public class TransitScheduleReader extends MatsimXmlParser {

	private static final String TRANSIT_LINE = "transitLine";
	private static final String TRANSIT_ROUTE = "transitRoute";
	private static final String DESCRIPTION = "description";
	private static final String DEPARTURE = "departure";
	private static final String ROUTE_PROFILE = "routeProfile";
	private static final String STOP = "stop";
	private static final String LINK = "link";

	private static final String ID = "id";

	private final TransitSchedule schedule;
	private final NetworkLayer network;
	private final Facilities facilities;

	private TransitLine currentTransitLine = null;
	private TransitRoute currentTransitRoute = null;
	private TempRouteProfile currentRouteProfile = null;

	public TransitScheduleReader(final TransitSchedule schedule, final NetworkLayer network, final Facilities facilities) {
		this.schedule = schedule;
		this.network = network;
		this.facilities = facilities;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (TRANSIT_LINE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			this.currentTransitLine = new TransitLine(id);
			this.schedule.addTransitLine(id, this.currentTransitLine);
		} else if (TRANSIT_ROUTE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			this.currentTransitRoute = new TransitRoute(id);
			this.currentTransitLine.addRoute(id, this.currentTransitRoute);
		} else if (DEPARTURE.equals(name)) {
			Id id = new IdImpl(atts.getValue(ID));
			Departure departure = new Departure(id, Time.parseTime(atts.getValue("departureTime")));
			this.currentTransitRoute.addDeparture(id, departure);
		} else if (ROUTE_PROFILE.equals(name)) {
			this.currentRouteProfile = new TempRouteProfile();
		} else if (STOP.equals(name)) {
			Facility facility = this.facilities.getFacility(new IdImpl(atts.getValue(ID)));
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
			Link link = this.network.getLink(atts.getValue("refId"));
			if (link == null) {
				throw new RuntimeException("no link with id " + atts.getValue("refId"));
			}
			this.currentRouteProfile.addLink(link);
		}

	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (DESCRIPTION.equals(name) && TRANSIT_ROUTE.equals(context.peek())) {
			this.currentTransitRoute.setDescription(content);
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

		private final List<TempStop> stops = new ArrayList<TempStop>();
		private final List<Link> links = new ArrayList<Link>();

		public TempRouteProfile() {
			// public constructor for private inner class
		}

		public void addStop(final TempStop stop) {
			this.stops.add(stop);
			if (this.links.get(this.links.size() - 1) != stop.stop.getLink()) {
				this.links.add(stop.stop.getLink());
			}
		}

		public void addLink(final Link link) {
			this.links.add(link);
		}

	}

}
