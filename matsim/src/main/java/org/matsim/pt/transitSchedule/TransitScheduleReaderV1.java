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

package org.matsim.pt.transitSchedule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

/**
 * Reads a transit schedule from a XML file in the format described by <code>transitSchedule_v1.dtd</code>.
 *
 * @author mrieser
 */
public class TransitScheduleReaderV1 extends MatsimXmlParser implements MatsimSomeReader {

	private final TransitSchedule schedule;
	private final RouteFactoryImpl routeFactory;

	private TransitLine currentTransitLine = null;
	private TempTransitRoute currentTransitRoute = null;
	private TempRoute currentRouteProfile = null;

	private final CoordinateTransformation coordinateTransformation;
	private final StringCache cache = new StringCache(); 

	/**
	 * @param schedule
	 * @param network
	 * @deprecated use {@link #TransitScheduleReaderV1(TransitSchedule, RouteFactoryImpl)}
	 */
	@Deprecated
	public TransitScheduleReaderV1(final TransitSchedule schedule, final Network network) {
		this(schedule, new RouteFactoryImpl());
	}

	public TransitScheduleReaderV1(final TransitSchedule schedule, final RouteFactoryImpl routeFactory) {
		this( new IdentityTransformation() , schedule , routeFactory );
	}

	public TransitScheduleReaderV1(final Scenario scenario) {
		this( scenario.getTransitSchedule(),
				((PopulationFactoryImpl) (scenario.getPopulation().getFactory())).getRouteFactory() );
	}

	public TransitScheduleReaderV1(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario) {
		this( coordinateTransformation,
				scenario.getTransitSchedule(),
				((PopulationFactoryImpl) (scenario.getPopulation().getFactory())).getRouteFactory() );
	}

	public TransitScheduleReaderV1(
			CoordinateTransformation coordinateTransformation,
			TransitSchedule schedule,
			RouteFactoryImpl routeFactory) {
		this.coordinateTransformation = coordinateTransformation;
		this.schedule = schedule;
		this.routeFactory = routeFactory;
	}

	public void readFile(final String fileName) throws UncheckedIOException {
		this.parse(fileName);
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (Constants.STOP_FACILITY.equals(name)) {
			boolean isBlocking = Boolean.parseBoolean(atts.getValue(Constants.IS_BLOCKING));
			TransitStopFacility stop =
					schedule.getFactory().createTransitStopFacility(
							Id.create(
									atts.getValue(Constants.ID),
									TransitStopFacility.class),
							coordinateTransformation.transform(
									new Coord(
											Double.parseDouble(atts.getValue("x")),
											Double.parseDouble(atts.getValue("y")))),
							isBlocking);
			if (atts.getValue(Constants.LINK_REF_ID) != null) {
				Id<Link> linkId = Id.create(atts.getValue(Constants.LINK_REF_ID), Link.class);
				stop.setLinkId(linkId);
			}
			if (atts.getValue(Constants.NAME) != null) {
				stop.setName(this.cache.get(atts.getValue(Constants.NAME)));
			}
			this.schedule.addStopFacility(stop);
		} else if (Constants.TRANSIT_LINE.equals(name)) {
			Id<TransitLine> id = Id.create(atts.getValue(Constants.ID), TransitLine.class);
			this.currentTransitLine = schedule.getFactory().createTransitLine(id);
			if (atts.getValue(Constants.NAME) != null) {
				this.currentTransitLine.setName(atts.getValue(Constants.NAME));
			}
			this.schedule.addTransitLine(this.currentTransitLine);
		} else if (Constants.TRANSIT_ROUTE.equals(name)) {
			Id<TransitRoute> id = Id.create(atts.getValue(Constants.ID), TransitRoute.class);
			this.currentTransitRoute = new TempTransitRoute(id);
		} else if (Constants.DEPARTURE.equals(name)) {
			Id<Departure> id = Id.create(atts.getValue(Constants.ID), Departure.class);
			Departure departure = new DepartureImpl(id, Time.parseTime(atts.getValue("departureTime")));
			String vehicleRefId = atts.getValue(Constants.VEHICLE_REF_ID);
			if (vehicleRefId != null) {
				departure.setVehicleId(Id.create(vehicleRefId, Vehicle.class));
			}
			this.currentTransitRoute.departures.put(id, departure);
		} else if (Constants.ROUTE_PROFILE.equals(name)) {
			this.currentRouteProfile = new TempRoute();
		} else if (Constants.LINK.equals(name)) {
			String linkStr = atts.getValue(Constants.REF_ID);
			if (!linkStr.contains(" ")) {
				this.currentRouteProfile.addLink(Id.create(linkStr, Link.class));
			} else {
				String[] links = linkStr.split(" ");
				for (int i = 0; i < links.length; i++) {
					this.currentRouteProfile.addLink(Id.create(links[i], Link.class));
				}
			}
		} else if (Constants.STOP.equals(name)) {
			Id<TransitStopFacility> id = Id.create(atts.getValue(Constants.REF_ID), TransitStopFacility.class);
			TransitStopFacility facility = this.schedule.getFacilities().get(id);
			if (facility == null) {
				throw new RuntimeException("no stop/facility with id " + atts.getValue(Constants.REF_ID));
			}
			TempStop stop = new TempStop(facility);
			String arrival = atts.getValue(Constants.ARRIVAL_OFFSET);
			String departure = atts.getValue(Constants.DEPARTURE_OFFSET);
			if (arrival != null) {
				stop.arrival = Time.parseTime(arrival);
			}
			if (departure != null) {
				stop.departure = Time.parseTime(departure);
			}
			stop.awaitDeparture = Boolean.parseBoolean(atts.getValue(Constants.AWAIT_DEPARTURE));
			this.currentTransitRoute.stops.add(stop);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (Constants.DESCRIPTION.equals(name) && Constants.TRANSIT_ROUTE.equals(context.peek())) {
			this.currentTransitRoute.description = content;
		} else if (Constants.TRANSPORT_MODE.equals(name)) {
			this.currentTransitRoute.mode = content.intern();
		} else if (Constants.TRANSIT_ROUTE.equals(name)) {
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(this.currentTransitRoute.stops.size());
			for (TempStop tStop : this.currentTransitRoute.stops) {
				TransitRouteStopImpl routeStop = new TransitRouteStopImpl(tStop.stop, tStop.arrival, tStop.departure);
				stops.add(routeStop);
				routeStop.setAwaitDepartureTime(tStop.awaitDeparture);
			}
			NetworkRoute route = null;
			if (this.currentRouteProfile.firstLinkId != null) {
				if (this.currentRouteProfile.lastLinkId == null) {
					this.currentRouteProfile.lastLinkId = this.currentRouteProfile.firstLinkId;
				}
				route = this.routeFactory.createRoute(NetworkRoute.class, this.currentRouteProfile.firstLinkId, this.currentRouteProfile.lastLinkId);
				route.setLinkIds(this.currentRouteProfile.firstLinkId, this.currentRouteProfile.linkIds, this.currentRouteProfile.lastLinkId);
			}
			TransitRoute transitRoute = this.schedule.getFactory().createTransitRoute(this.currentTransitRoute.id, route, stops, this.currentTransitRoute.mode);
			transitRoute.setDescription(this.currentTransitRoute.description);
			for (Departure departure : this.currentTransitRoute.departures.values()) {
				transitRoute.addDeparture(departure);
			}
			this.currentTransitLine.addRoute(transitRoute);
		}
	}

	private static class TempTransitRoute {
		protected final Id<TransitRoute> id;
		protected String description = null;
		protected Map<Id<Departure>, Departure> departures = new LinkedHashMap<Id<Departure>, Departure>();
		/*package*/ List<TempStop> stops = new ArrayList<TempStop>();
		/*package*/ String mode = null;

		protected TempTransitRoute(final Id<TransitRoute> id) {
			this.id = id;
		}
	}

	private static class TempStop {
		protected final TransitStopFacility stop;
		protected double departure = Time.UNDEFINED_TIME;
		protected double arrival = Time.UNDEFINED_TIME;
		protected boolean awaitDeparture = false;

		protected TempStop(final TransitStopFacility stop) {
			this.stop = stop;
		}
	}

	private static class TempRoute {
		/*package*/ List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		/*package*/ Id<Link> firstLinkId = null;
		/*package*/ Id<Link> lastLinkId = null;

		protected TempRoute() {
			// public constructor for private inner class
		}

		protected void addLink(final Id<Link> linkId) {
			if (this.firstLinkId == null) {
				this.firstLinkId = linkId;
			} else if (this.lastLinkId == null) {
				this.lastLinkId = linkId;
			} else {
				this.linkIds.add(this.lastLinkId);
				this.lastLinkId = linkId;
			}
		}

	}

	private static class StringCache {

		private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<String, String>(10000);

		/**
		 * Returns the cached version of the given String. If the strings was
		 * not yet in the cache, it is added and returned as well.
		 *
		 * @param string
		 * @return cached version of string
		 */
		public String get(final String string) {
			if (string == null) {
				return null;
			}
			String s = cache.putIfAbsent(string, string);
			if (s == null) {
				return string;
			}
			return s;
		}
	}

}
