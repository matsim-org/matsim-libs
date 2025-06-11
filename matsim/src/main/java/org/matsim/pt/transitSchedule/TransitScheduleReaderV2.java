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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.ChainedDeparture;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

/**
 * Reads a transit schedule from a XML file in the format described by <code>transitSchedule_v1.dtd</code>.
 *
 * @author mrieser
 */
public class TransitScheduleReaderV2 extends MatsimXmlParser {
	private static final Logger log = LogManager.getLogger(TransitScheduleReaderV2.class);

	private final String externalInputCRS;
	private final String targetCRS;
	private final TransitSchedule schedule;
	private final RouteFactories routeFactory;

	private TransitLine currentTransitLine = null;
	private TempTransitRoute currentTransitRoute = null;
	private TempRoute currentRouteProfile = null;
	private List<ChainedDeparture> currentChainedDepartures = null;
	private Departure currentDeparture = null;

	private final AttributesXmlReaderDelegate attributesDelegate = new AttributesXmlReaderDelegate();
	private org.matsim.utils.objectattributes.attributable.Attributes currentAttributes = null;

	private CoordinateTransformation coordinateTransformation = new IdentityTransformation();
	private final StringCache cache = new StringCache();

	public TransitScheduleReaderV2(final TransitSchedule schedule, final RouteFactories routeFactory) {
		this(null, null, schedule, routeFactory);
	}

	public TransitScheduleReaderV2(
		final String externalInputCRS,
		final String targetCRS,
		final Scenario scenario) {
		this(externalInputCRS, targetCRS, scenario.getTransitSchedule(), scenario.getPopulation().getFactory().getRouteFactories());
	}

	private TransitScheduleReaderV2(
		String externalInputCRS,
		String targetCRS,
		TransitSchedule schedule,
		RouteFactories routeFactory) {
		super(ValidationType.DTD_ONLY);
		this.externalInputCRS = externalInputCRS;
		this.targetCRS = targetCRS;
		this.schedule = schedule;
		this.routeFactory = routeFactory;
		if (externalInputCRS != null && targetCRS != null) {
			this.coordinateTransformation = TransformationFactory.getCoordinateTransformation(externalInputCRS, targetCRS);
			ProjectionUtils.putCRS(this.schedule, targetCRS);
		}
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (Constants.STOP_FACILITY.equals(name)) {
			boolean isBlocking = Boolean.parseBoolean(atts.getValue(Constants.IS_BLOCKING));
			Coord coord = atts.getValue(Constants.Z) == null ?
				new Coord(Double.parseDouble(atts.getValue(Constants.X)), Double.parseDouble(atts.getValue(Constants.Y))) :
				new Coord(Double.parseDouble(atts.getValue(Constants.X)), Double.parseDouble(atts.getValue(Constants.Y)), Double.parseDouble(atts.getValue(Constants.Z)));
			TransitStopFacility stop =
				this.schedule.getFactory().createTransitStopFacility(
					Id.create(atts.getValue(Constants.ID), TransitStopFacility.class),
					this.coordinateTransformation.transform(coord),
					isBlocking);
			this.currentAttributes = stop.getAttributes();
			if (atts.getValue(Constants.LINK_REF_ID) != null) {
				Id<Link> linkId = Id.create(atts.getValue(Constants.LINK_REF_ID), Link.class);
				stop.setLinkId(linkId);
			}
			if (atts.getValue(Constants.NAME) != null) {
				stop.setName(this.cache.get(atts.getValue(Constants.NAME)));
			}
			if (atts.getValue(Constants.STOP_AREA_ID) != null) {
				stop.setStopAreaId(Id.create(atts.getValue(Constants.STOP_AREA_ID), TransitStopArea.class));
			}
			this.schedule.addStopFacility(stop);
		} else if (Constants.TRANSIT_LINE.equals(name)) {
			Id<TransitLine> id = Id.create(atts.getValue(Constants.ID), TransitLine.class);
			this.currentTransitLine = this.schedule.getFactory().createTransitLine(id);
			this.currentAttributes = this.currentTransitLine.getAttributes();
			if (atts.getValue(Constants.NAME) != null) {
				this.currentTransitLine.setName(atts.getValue(Constants.NAME));
			}
			this.schedule.addTransitLine(this.currentTransitLine);
		} else if (Constants.TRANSIT_ROUTE.equals(name)) {
			Id<TransitRoute> id = Id.create(atts.getValue(Constants.ID), TransitRoute.class);
			this.currentTransitRoute = new TempTransitRoute(id);
			this.currentAttributes = this.currentTransitRoute.attributes;
		} else if (Constants.DEPARTURE.equals(name)) {
			Id<Departure> id = Id.create(atts.getValue(Constants.ID), Departure.class);
			this.currentDeparture = new DepartureImpl(id, Time.parseTime(atts.getValue("departureTime")));
			this.currentAttributes = this.currentDeparture.getAttributes();
			String vehicleRefId = atts.getValue(Constants.VEHICLE_REF_ID);
			if (vehicleRefId != null) {
				this.currentDeparture.setVehicleId(Id.create(vehicleRefId, Vehicle.class));
			}
			this.currentTransitRoute.departures.put(id, this.currentDeparture);
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
			TransitRouteStopImpl.Builder stopBuilder = new TransitRouteStopImpl.Builder().stop(facility);
			String arrival = atts.getValue(Constants.ARRIVAL_OFFSET);
			String departure = atts.getValue(Constants.DEPARTURE_OFFSET);
			if (arrival != null) {
				Time.parseOptionalTime(arrival).ifDefined(stopBuilder::arrivalOffset);
			}
			if (departure != null) {
				Time.parseOptionalTime(departure).ifDefined(stopBuilder::departureOffset);
			}
			stopBuilder.allowBoarding(Boolean.parseBoolean(atts.getValue(Constants.ALLOW_BOARDING)));
			stopBuilder.allowAlighting(Boolean.parseBoolean(atts.getValue(Constants.ALLOW_ALIGHTING)));
			stopBuilder.awaitDepartureTime(Boolean.parseBoolean(atts.getValue(Constants.AWAIT_DEPARTURE)));
			this.currentTransitRoute.stopBuilders.add(stopBuilder);
		} else if (Constants.RELATION.equals(name)) {
			Id<TransitStopFacility> fromStop = Id.create(atts.getValue(Constants.FROM_STOP), TransitStopFacility.class);
			Id<TransitStopFacility> toStop = Id.create(atts.getValue(Constants.TO_STOP), TransitStopFacility.class);
			double transferTime = Time.parseTime(atts.getValue(Constants.TRANSFER_TIME));
			this.schedule.getMinimalTransferTimes().set(fromStop, toStop, transferTime);
		} else if (Constants.CHAINED_DEPARTURE.equals(name)) {

			Id<Departure> departureId = Id.create(atts.getValue(Constants.TO_DEPARTURE), Departure.class);
			Id<TransitLine> transitLineId = this.currentTransitLine.getId();
			Id<TransitRoute> transitRouteId = this.currentTransitRoute.id;

			if (atts.getValue(Constants.TO_TRANSIT_LINE) != null) {
				transitLineId = Id.create(atts.getValue(Constants.TO_TRANSIT_LINE), TransitLine.class);
			}

			if (atts.getValue(Constants.TO_TRANSIT_ROUTE) != null) {
				transitRouteId = Id.create(atts.getValue(Constants.TO_TRANSIT_ROUTE), TransitRoute.class);
			}

			ChainedDeparture chainedDeparture = new ChainedDepartureImpl(transitLineId, transitRouteId, departureId);

			// Only create new list of necessary
			if (this.currentChainedDepartures == null) {
				this.currentChainedDepartures = new ArrayList<>();
			}

			this.currentChainedDepartures.add(chainedDeparture);

		} else if (Constants.ATTRIBUTE.equals(name)) {
			this.attributesDelegate.startTag(name, atts, context, this.currentAttributes);
		} else if (Constants.ATTRIBUTES.equals(name)) {
			this.attributesDelegate.startTag(name, atts, context, this.currentAttributes);
		} else if (Constants.TRANSIT_SCHEDULE.equals(name)) {
			this.currentAttributes = this.schedule.getAttributes();
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (Constants.DEPARTURE.equals(name)) {
			if (this.currentChainedDepartures != null) {
				this.currentDeparture.setChainedDepartures(Collections.unmodifiableList(this.currentChainedDepartures));
				this.currentChainedDepartures = null;
			}
			this.currentDeparture = null;
		} else if (Constants.DESCRIPTION.equals(name) && Constants.TRANSIT_ROUTE.equals(context.peek())) {
			this.currentTransitRoute.description = content;
		} else if (Constants.TRANSPORT_MODE.equals(name)) {
			this.currentTransitRoute.mode = content.intern();
		} else if (Constants.TRANSIT_ROUTE.equals(name)) {
			List<TransitRouteStop> stops = new ArrayList<>(this.currentTransitRoute.stopBuilders.size());
			this.currentTransitRoute.stopBuilders.forEach(stopBuilder -> stops.add(stopBuilder.build()));
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
			AttributesUtils.copyTo(this.currentTransitRoute.attributes, transitRoute.getAttributes());
			this.currentTransitLine.addRoute(transitRoute);
			this.currentTransitRoute = null;
		} else if (Constants.TRANSIT_LINE.equals(name)) {
			this.currentTransitLine = null;
		} else if (Constants.ATTRIBUTE.equals(name)) {
			this.attributesDelegate.endTag(name, content, context);
		} else if (Constants.ATTRIBUTES.equals(name)) {
			if (context.peek().equals(Constants.TRANSIT_SCHEDULE)) {
				String inputCRS = (String) currentAttributes.getAttribute(ProjectionUtils.INPUT_CRS_ATT);

				if (inputCRS != null && targetCRS != null) {
					if (externalInputCRS != null) {
						// warn or crash?
						log.warn("coordinate transformation defined both in config and in input file: setting from input file will be used");
					}
					coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS);
					currentAttributes.putAttribute(ProjectionUtils.INPUT_CRS_ATT, targetCRS);
				}
			}
			this.currentAttributes = null;
		}
	}

	private static class TempTransitRoute {
		protected final Id<TransitRoute> id;
		protected final org.matsim.utils.objectattributes.attributable.Attributes attributes = new org.matsim.utils.objectattributes.attributable.AttributesImpl();
		protected String description = null;
		protected Map<Id<Departure>, Departure> departures = new LinkedHashMap<>();
		/*package*/ List<TransitRouteStopImpl.Builder> stopBuilders = new ArrayList<>();
		/*package*/ String mode = null;

		protected TempTransitRoute(final Id<TransitRoute> id) {
			this.id = id;
		}
	}

	private static class TempRoute {
		/*package*/ List<Id<Link>> linkIds = new ArrayList<>();
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

		private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(10000);

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
			String s = this.cache.putIfAbsent(string, string);
			if (s == null) {
				return string;
			}
			return s;
		}
	}

}
