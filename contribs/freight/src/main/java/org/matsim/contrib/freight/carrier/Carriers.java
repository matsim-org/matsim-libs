/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.carrier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.matsim.api.core.v01.Id;

/**
 * A container that maps carriers.
 * 
 * @author sschroeder
 *
 */
public class Carriers {

	private static final  Logger log = LogManager.getLogger(Carriers.class);
	private final Map<Id<Carrier>, Carrier> carriers = new HashMap<>();

	public Carriers(Collection<Carrier> carriers) {
		makeMap(carriers);
	}

	public Carriers() {
	}

	private void makeMap(Collection<Carrier> carriers) {
		for (Carrier carrier : carriers) {
			this.carriers.put(carrier.getId(), carrier);
		}
	}

	public Map<Id<Carrier>, Carrier> getCarriers() {
		return carriers;
	}

	public void addCarrier(Carrier carrier) {
		if(!carriers.containsKey(carrier.getId())){
			carriers.put(carrier.getId(), carrier);
		}
		else log.warn("carrier " + carrier.getId() + " already exists");
	}

}
