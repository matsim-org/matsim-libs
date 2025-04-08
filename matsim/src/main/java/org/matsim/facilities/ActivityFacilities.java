/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Root class for activity facilities.
 *
 * Maintainer: mrieser / Senozon AG
 */
public interface ActivityFacilities extends MatsimToplevelContainer, Attributable {

	public String getName();

	public void setName(String name);

	@Override
	public ActivityFacilitiesFactory getFactory();

	public Map<Id<ActivityFacility>, ? extends ActivityFacility> getFacilities();

	public void addActivityFacility(ActivityFacility facility);

	/* not sure if this method should be in the interface, but too many users seem to use and like it,
	 * so there seems to be a need for it...   mrieser/jul13
	 */
	public TreeMap<Id<ActivityFacility>, ActivityFacility> getFacilitiesForActivityType(final String actType);

}
