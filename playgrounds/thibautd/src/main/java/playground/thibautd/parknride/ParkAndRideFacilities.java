/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacilities.java
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: meant to be a readable/writable object, defining links between
 * network nodes and PT stops.
 *
 * @author thibautd
 */
public class ParkAndRideFacilities implements MatsimToplevelContainer {
	public static final String ELEMENT_NAME = "pnrFacilities";
	private final String name;
	private final Map<Id, ParkAndRideFacility> facilities =
		new HashMap<Id, ParkAndRideFacility>();

	public ParkAndRideFacilities(final String name) {
		this.name = name;
	}

	public Map<Id, ParkAndRideFacility> getFacilities() {
		return facilities;
	}

	public void addFacility(final ParkAndRideFacility facility) {
		this.facilities.put( facility.getId() , facility );
	}

	@Override
	public MatsimFactory getFactory() {
		throw new UnsupportedOperationException( "TODO" );
	}

	public String getName() {
		return name;
	}
}

