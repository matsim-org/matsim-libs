/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalTravelTimeWrapper.java
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

package org.matsim.ptproject.qsim.multimodalsimengine.router.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelTime;

/**
 * This class can bundle several MultiModalTravelTime calculators.
 * For each call to getModalLinkTravelTime(...) it is checked whether
 * a travel time calculator for the given transport mode is available.
 * 
 * @author cdobler
 */
public class MultiModalTravelTimeWrapper implements MultiModalTravelTime {

	private static final Logger log = Logger.getLogger(MultiModalTravelTimeWrapper.class);
	
	private final Map<String, PersonalizableTravelTime> travelTimes;
	private TravelTime modeTravelTime;
	
	// use the factory
	/*package*/ MultiModalTravelTimeWrapper() {
		this.travelTimes = new HashMap<String, PersonalizableTravelTime>();
	}
	
	/*package*/ void setPersonalizableTravelTime(String transportMode, PersonalizableTravelTime travelTime) {
		if (this.travelTimes.containsKey(transportMode)) {
			log.warn("A PersonalizableTravelTime calculator for transport mode " + transportMode + " already exists. Replacing it!");
		}
		this.travelTimes.put(transportMode, travelTime);
	}
	
	@Override
	public void setPerson(Person person) {
		for (PersonalizableTravelTime travelTime : travelTimes.values()) travelTime.setPerson(person);
	}

	@Override
	public void setTransportMode(String transportMode) {
		modeTravelTime = travelTimes.get(transportMode);
		if (modeTravelTime == null) throw new RuntimeException("No PersonalizableTravelTime calculator set for transport mode " + transportMode);
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
		return modeTravelTime.getLinkTravelTime(link, time);
	}

	@Override
	public double getModalLinkTravelTime(Link link, double time, String transportMode) {
		TravelTime travelTime = travelTimes.get(transportMode);
		if (travelTime == null) throw new RuntimeException("No PersonalizableTravelTime calculator set for transport mode " + transportMode);
		return travelTime.getLinkTravelTime(link, time);
	}

}
