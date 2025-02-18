/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.drt.routing;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.utils.misc.OptionalTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;

/**
 * Assumptions:
 * <ul>
 * <li>{@code maxWaitTime} is the maximum wait time</li>
 * <li>{@code directRideTime} is the time of an unshared ride</li>
 * <li>{@code travelTime} is the maximum travel (wait+ride) time (excluding walk to/from the stop)</li>
 * </ul>
 *
 * @author michalm (Michal Maciejewski)
 */
public class DrtRoute extends AbstractRoute {
  	private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	public final static String ROUTE_TYPE = TransportMode.drt;
	private RouteDescription routeDescription = null;

	public DrtRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
		this.routeDescription = new RouteDescription();
	}

	public double getDirectRideTime() {
		return routeDescription.getDirectRideTime();
	}

	public double getMaxWaitTime() {
		return routeDescription.getMaxWaitTime();
	}

	public List<String> getUnsharedPath() {
		return routeDescription.getUnsharedPath();
	}

	public double getMaxTravelTime() {
		return getTravelTime().seconds(); // currently DrtRoute.travelTime is set to the max allowed travel time
	}

	public double getMaxRideTime() {
		return routeDescription.getMaxRideTime();
	}

	public void setDirectRideTime(double directRideTime) {
		this.routeDescription.setDirectRideTime(directRideTime);
	}

	public void setMaxWaitTime(double maxWaitTime) {
		this.routeDescription.setMaxWaitTime(maxWaitTime);
	}

	public void setUnsharedPath(VrpPathWithTravelData unsharedPath) {
		List<String> links = new ArrayList<String>();
		unsharedPath.iterator().forEachRemaining(l->{links.add(l.getId().toString());});
		this.routeDescription.setUnsharedPath(links);
	}

	public void setMaxRideTime(double maxRideTime) {
		this.routeDescription.setMaxRideTime(maxRideTime);
	}

	private DvrpLoad load;

	public DvrpLoad getLoad(DvrpLoadType loadType) {
		if (load == null) {
			load = loadType.deserialize(routeDescription.getLoad());
		}

		return load;
	}

	public void setLoad(DvrpLoad load, DvrpLoadType loadType) {
		this.load = load;
		routeDescription.setLoad(loadType.serialize(load));
	}

	@Override
	public String getRouteDescription() {
		try {
			return OBJECT_MAPPER.writeValueAsString(routeDescription);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setRouteDescription(String routeDescription) {

		Pattern pat = Pattern.compile("[0-9.]+ [0-9.]+");

		// Handle old routeDescription (non-json)
		if (pat.matcher(routeDescription).find()) {
			String[] values = routeDescription.split(" ");
			this.routeDescription.setMaxWaitTime(requiresZeroOrPositive(Double.parseDouble(values[0])));
			this.routeDescription.setDirectRideTime(requiresZeroOrPositive(Double.parseDouble(values[1])));

			// Handle new routeDescription (json)
		} else if (routeDescription.startsWith("{")) {
			try {
				this.routeDescription = OBJECT_MAPPER.readValue(routeDescription, RouteDescription.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException("Unsupported RouteDescription");
		}
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public DrtRoute clone() {
		return (DrtRoute)super.clone();
	}

	private double requiresZeroOrPositive(double value) {
		checkArgument(value >= 0 || value <= Double.MAX_VALUE, "Value: (%s) must be zero or positive", value);
		return value;
	}


	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("maxWaitTime", routeDescription.getMaxWaitTime())
				.add("directRideTime", routeDescription.getDirectRideTime())
				.add("load", routeDescription.getLoad())
				.add("super", super.toString())
				.toString();
	}


	public static class RouteDescription {
		private OptionalTime maxWaitTime = OptionalTime.undefined();
		private OptionalTime directRideTime = OptionalTime.undefined();
		private List<String> unsharedPath = new ArrayList<String>();
		private OptionalTime maxRideTime = OptionalTime.undefined();
		private String load = null;

		@JsonProperty("directRideTime")
		public double getDirectRideTime() {
			return directRideTime.isUndefined() ? OptionalTime.undefined().seconds() : directRideTime.seconds();
		}

		@JsonProperty("maxRideTime")
		public double getMaxRideTime() {
			return maxRideTime.seconds();
		}

		@JsonProperty("maxWaitTime")
		public double getMaxWaitTime() {
			return maxWaitTime.isUndefined() ? OptionalTime.undefined().seconds() : maxWaitTime.seconds();
		}

		@JsonProperty("unsharedPath")
		public List<String> getUnsharedPath() {
			return unsharedPath;
		}

		@JsonProperty("load")
		public String getLoad() {
			return load;
		}

		public void setDirectRideTime(double directRideTime) {
			this.directRideTime = OptionalTime.defined(directRideTime);
		}

		public void setMaxRideTime(double maxRideTime) {
			this.maxRideTime = OptionalTime.defined(maxRideTime);
		}

		public void setMaxWaitTime(double maxWaitTime) {
			this.maxWaitTime = OptionalTime.defined(maxWaitTime);
		}

		public void setUnsharedPath(List<String> unsharedPath) {
			this.unsharedPath = unsharedPath;
		}

		public void setLoad(String load) {
			this.load = load;
		}
	}
}
