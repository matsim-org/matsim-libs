/* *********************************************************************** *
 * project: org.matsim.*
 * MultimodalTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.router.util;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MultiModalTravelTimeFactory implements MatsimFactory {
	
	private static final Logger log = Logger.getLogger(MultiModalTravelTimeFactory.class);
	
	private final Map<String, Provider<TravelTime>> factories;
	private final Map<String, Provider<TravelTime>> additionalFactories;
	private final Map<Id<Link>, Double> linkSlopes;
	
	public MultiModalTravelTimeFactory(Config config) {
		this(config, null, null);
	}

	public MultiModalTravelTimeFactory(Config config, Map<Id<Link>, Double> linkSlopes) {
		this(config, linkSlopes, null);
	}
	
	public MultiModalTravelTimeFactory(Config config, Map<Id<Link>, Double> linkSlopes, Map<String, Provider<TravelTime>> additionalFactories) {
		this.linkSlopes = linkSlopes;
		this.factories = new LinkedHashMap<>();
		this.additionalFactories = additionalFactories;
		
		if (this.linkSlopes == null) {
			log.warn("No slope information for the links available - travel time will only take agents age and gender into account!");
		}
		
		this.initMultiModalTravelTimeFactories(config);
	}
	
	public Map<String, TravelTime> createTravelTimes() {
		Map<String, TravelTime> travelTimes = new HashMap<>();
		
		for (Entry<String, Provider<TravelTime>> entry : factories.entrySet()) {
			travelTimes.put(entry.getKey(), entry.getValue().get());
		}
		
		return travelTimes;
	}
	
	private void initMultiModalTravelTimeFactories(Config config) {
		
		PlansCalcRouteConfigGroup plansCalcRouteConfigGroup = config.plansCalcRoute();
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
        Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		
		for (String mode : simulatedModes) {		
			if (mode.equals(TransportMode.walk)) {
				Provider<TravelTime> factory = new WalkTravelTimeFactory(plansCalcRouteConfigGroup, linkSlopes);
				this.factories.put(mode, factory);
			} else if (mode.equals(TransportMode.transit_walk)) {
				Provider<TravelTime> factory = new TransitWalkTravelTimeFactory(plansCalcRouteConfigGroup, linkSlopes);
				this.factories.put(mode, factory);
			} else if (mode.equals(TransportMode.bike)) {
				Provider<TravelTime> factory = new BikeTravelTimeFactory(plansCalcRouteConfigGroup, linkSlopes);
				this.factories.put(mode, factory);
			} else {
				Provider<TravelTime> factory = getTravelTimeFactory(mode);
				
				if (factory == null) {
					log.warn("Mode " + mode + " is not supported! " + 
							"Use a constructor where you provide the travel time objects. " +
							"Using a UnknownTravelTime calculator based on constant speed." +
							"Agent specific attributes are not taken into account!");
					factory = new UnknownTravelTimeFactory(mode, plansCalcRouteConfigGroup);
					this.factories.put(mode, factory);
				} else {
					log.info("Found additional travel time factory from type " + factory.getClass().toString() +
							" for mode " + mode + ".");
					this.factories.put(mode, factory);
				}
			}
		}
	}
	
	private Provider<TravelTime> getTravelTimeFactory(String mode) {
		if (additionalFactories != null) return this.additionalFactories.get(mode);
		else return null;
	}
}