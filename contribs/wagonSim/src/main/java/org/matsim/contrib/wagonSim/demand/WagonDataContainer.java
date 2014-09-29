/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.demand;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author balmermi
 * @since 2013-08-13
 */
public class WagonDataContainer {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	final Map<Id<Wagon>, Wagon> wagons = new HashMap<>();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public WagonDataContainer() {
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////
	
	static class Wagon {
		final Id<Wagon> id;
		Double weight = null;
		Double length = null;
		Double weightLoad = null;
		String fromZoneId = null;
		String toZoneId = null;
		Double depTime = null;
		
		Wagon(String id) { this.id = Id.create(id, Wagon.class); }
	}
}
