/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacilities.java
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
package eu.eunoiaproject.bikesharing.scenario;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * @author thibautd
 */
public class BikeSharingFacilities implements MatsimToplevelContainer {
	private final Map<Id, BikeSharingFacility> facilities =
		new LinkedHashMap<Id, BikeSharingFacility>();
	private final Map<Id, BikeSharingFacility> unmodifiableFacilities =
		Collections.unmodifiableMap( facilities );

	public void addFacility( final BikeSharingFacility facility ) {
		facilities.put( facility.getId() , facility );
	}

	public Map<Id, BikeSharingFacility> getFacilities() {
		return unmodifiableFacilities;
	}

	@Override
	public BikeSharingFacilitiesFactory getFactory() {
		return new BikeSharingFacilitiesFactory() {
			@Override
			public BikeSharingFacility createBikeSharingFacility(
					final Id id,
					final Coord coord,
					final Id linkId,
					final int capacity,
					final int initialNumberOfBikes) {
				return new BikeSharingFacility(
							id,
							coord,
							linkId,
							capacity,
							initialNumberOfBikes);
			}
		};
	}
}

