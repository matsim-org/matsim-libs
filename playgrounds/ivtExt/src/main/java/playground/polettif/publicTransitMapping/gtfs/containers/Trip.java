/* *********************************************************************** *
 * project: org.matsim.*
 * Trip.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.gtfs.containers;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;

/**
 * Container for a GTFS Trip
 */
public class Trip {
	
	//Attributes
	private final String tripId;
	private final Service service;
	private final Shape shape;
	private final String name;
	private final SortedMap<Integer,StopTime> stopTimes;
	private final List<Frequency> frequencies;
	private List<Link> links;

	//Methods
	public Trip(String tripId, Service service, Shape shape, String name) {
		super();
		this.tripId = tripId;
		this.service = service;
		this.shape = shape;
		this.name = name;
		stopTimes = new TreeMap<>();
		frequencies = new ArrayList<>();
		links = new ArrayList<>();
	}	

	/**
	 * @return the service
	 */
	public Service getService() {
		return service;
	}

	/**
	 * @return the shape
	 */
	public Shape getShape() {
		return shape;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the stopTimes
	 */
	public SortedMap<Integer, StopTime> getStopTimes() {
		return stopTimes;
	}

	/**
	 * @return the frequencies
	 */
	public List<Frequency> getFrequencies() {
		return frequencies;
	}

	/**
	 * @return the route
	 */
	public List<Link> getLinks() {
		return links;
	}

	/**
	 * Puts a new stopTime
	 * @param stopSequencePosition which stop number in the stopSequence this stopTime is referencing
	 * @param stopTime
	 */
	public void putStopTime(Integer stopSequencePosition, StopTime stopTime) {
		stopTimes.put(stopSequencePosition, stopTime);
	}

	/**
	 * Adds a new frequency
	 * @param frequency
	 */
	public void addFrequency(Frequency frequency) {
		frequencies.add(frequency);
	}

	/**
	 * @param route the route to set
	 */
	public void setRoute(List<Link> route) {
		this.links = route;
	}

	/**
	 * Adds a new link
	 * @param link
	 */
	public void addLink(Link link) {
		links.add(link);
	}

	/**
	 * @return the tripId
	 */
	public String getId() {
		return tripId;
	}
}
