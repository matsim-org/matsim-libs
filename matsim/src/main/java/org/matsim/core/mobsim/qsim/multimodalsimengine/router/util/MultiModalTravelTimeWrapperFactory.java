/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalTravelTimeWrapperFactory.java
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.router.util.TravelTime;

/**
 * Factory for a MultiModalTravelTimeWrapper. Uses PersonalizableTravelTimeFactory
 * to create new PersonalizableTravelTime instances for each Wrapper.
 * 
 * @author cdobler
 */
public class MultiModalTravelTimeWrapperFactory implements MultiModalTravelTimeFactory {

	private static final Logger log = Logger.getLogger(MultiModalTravelTimeWrapperFactory.class);
	
	private final Map<String, TravelTime> travelTimeFactories;
	
	public MultiModalTravelTimeWrapperFactory() {
		this.travelTimeFactories = new HashMap<String, TravelTime>();
	}
	
	@Override
	public MultiModalTravelTimeWrapper createTravelTime() {
		MultiModalTravelTimeWrapper wrapper = new MultiModalTravelTimeWrapper();
		
		for (Entry<String, TravelTime> entry : travelTimeFactories.entrySet()) {
			wrapper.setTravelTime(entry.getKey(), entry.getValue());
		}
		
		return wrapper;
	}
	
	public void setPersonalizableTravelTimeFactory(String transportMode, TravelTime travelTime) {
		if (this.travelTimeFactories.containsKey(transportMode)) {
			log.warn("A PersonalizableTravelTimeFactory for transport mode " + transportMode + " already exists. Replacing it!");
		}
		this.travelTimeFactories.put(transportMode, travelTime);
	}

	public Map<String, TravelTime> getTravelTimeFactories() {
		return Collections.unmodifiableMap(travelTimeFactories);
	}

}
