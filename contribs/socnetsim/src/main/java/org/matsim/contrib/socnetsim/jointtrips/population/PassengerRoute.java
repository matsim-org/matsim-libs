/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointtrips.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * A route for passenger trips.
 * @author thibautd
 */
public class PassengerRoute implements Route {
	private double distance = Double.NaN;
	private double travelTime = Time.UNDEFINED_TIME;
	private Id<Link> startLink = null;
	private Id<Link> endLink = null;
	private Id<Person> driver = null;
	private final Attributes attributes = new Attributes();

	private PassengerRoute() {}

	public PassengerRoute(
			final Id<Link> startLink,
			final Id<Link> endLink) {
		this.startLink = startLink;
		this.endLink = endLink;
	}

	public Id<Person> getDriverId() {
		return driver;
	}

	public void setDriverId(final Id<Person> d) {
		driver = d;
	}

	@Override
	public double getDistance() {
		return distance;
	}

	@Override
	public void setDistance(final double distance) {
		this.distance = distance;
	}

	@Override
	public double getTravelTime() {
		return travelTime;
	}

	@Override
	public void setTravelTime(final double travelTime) {
		this.travelTime = travelTime;
	}

	@Override
	public Id<Link> getStartLinkId() {
		return startLink;
	}

	@Override
	public Id<Link> getEndLinkId() {
		return endLink;
	}

	@Override
	public void setStartLinkId(final Id<Link> linkId) {
		startLink = linkId;
	}

	@Override
	public void setEndLinkId(final Id<Link> linkId) {
		endLink = linkId;
	}

	@Override
	public void setRouteDescription(
			final String routeDescription
			) {
		driver = Id.create(routeDescription.trim(), Person.class);
	}

	@Override
	public String getRouteDescription() {
		return driver != null ? driver.toString() : "";
	}

	@Override
	public String getRouteType() {
		return "passenger";
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

	@Override
	public PassengerRoute clone() {
		PassengerRoute c = new PassengerRoute();
		c.distance = distance;
		c.travelTime= travelTime;
		c.startLink = startLink;
		c.endLink = endLink;
		c.driver = driver;
		return c;
	}

	@Override
	public String toString() {
		return "[PassengerRoute: "+
			"distance="+distance+"; "+
			"travelTime="+travelTime+"; "+
			"startLink="+startLink+"; "+
			"endLink="+endLink+"; "+
			"driver="+driver+"]";
	}
}

