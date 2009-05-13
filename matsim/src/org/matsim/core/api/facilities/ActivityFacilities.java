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

package org.matsim.core.api.facilities;

import java.util.Map;

import org.matsim.api.basic.v01.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.facilities.BasicActivityFacilities;

public interface ActivityFacilities extends BasicActivityFacilities, Facilities {

	public Map<Id, ? extends ActivityFacility> getFacilities();
	
	public ActivityFacility createFacility(final Id id, final Coord center);
	// TODO move create to Builder
	
	// all the rest ist deprecated... 

	//Added 27.03.08 JH for random secondary location changes
	/** @deprecated filtering should be done outside of API */
	public Map<Id, ActivityFacility> getFacilitiesForActivityType(final String act_type);
	
	@Deprecated
	public static final Id LAYER_TYPE = new IdImpl("facility");

	@Deprecated // to be clarified
	public String getName();
	
	@Deprecated // to be clarified
	public void setName(String name);

	@Deprecated
	public void finishFacility(final ActivityFacility f);

}
