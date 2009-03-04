/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.interfaces.core.v01;

import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.world.Location;

public interface Facilities extends Iterable<Facility> {
	// TODO [MR] remove Iterable

	public Map<Id, ? extends Facility> getFacilities();

	public Facility createFacility(final Id id, final Coord center);
	// TODO move create to Builder
	
	//Added 27.03.08 JH for random secondary location changes
	public Map<Id, Facility> getFacilities(final String act_type);

	// all the rest ist deprecated... 
	
	@Deprecated
	public static final Id LAYER_TYPE = new IdImpl("facility");

	@Deprecated // to be clarified
	public String getName();
	
	@Deprecated // to be clarified
	public void setName(String name);

	@Deprecated // needs to be clarified, return Facility instead of Location
	public List<Location> getLocations(final Coord center);

	@Deprecated // needs to be clarified
	public List<Location> getNearestLocations(final Coord center);

	@Deprecated
	public void finishFacility(final Facility f);

	@Deprecated
	public void printFacilitiesCount();

}
