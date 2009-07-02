/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityI.java
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

package org.matsim.core.facilities;

import java.util.Map;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.population.*;
import org.matsim.core.basic.v01.facilities.BasicActivityFacility;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;
import org.matsim.world.*;

public interface ActivityFacility extends BasicActivityFacility, Location, Facility {

	public ActivityOption createActivityOption(final String type);
	// TODO [MR] move to Builder

	public Map<String, ActivityOption> getActivityOptions();

	public ActivityOption getActivityOption(final String type);

	
	/**
	 * Moves a facility to a new {@link Coord coordinate}. It also takes care that
	 * the up- and down-mapping to the neighbor layers (up-layer: {@link ZoneLayer}
	 * and down-layer: {@link NetworkLayer}) will be updated, too (if the neighbors exist).
	 * 
	 * <p><b>Note:</b> Other data structures than the {@link World} and the {@link NetworkLayer} of MATSim
	 * will not be updated (i.e. the references to links and facilities in a {@link PlanImpl}
	 * of an agent of the {@link Population}).</p>
	 * 
	 * <p><b>Mapping rule (zone-facility):</b> The facility gets one zones assigned, in which 
	 * the facility is located in, or---if no such zone exists---the facility does not get a zone assigned.</p>
	 * 
	 * <p><b>Mapping rule (facility-link):</b> The facility gets the nearest right entry link assigned
	 * (see also {@link NetworkLayer#getNearestRightEntryLink(Coord)}).</p>
	 * 
	 * @param newCoord the now coordinate of the facility
	 */
	@Deprecated
	public void moveTo(Coord newCoord);

	@Deprecated
	public void setDesc(String desc);

	@Deprecated
	public String getDesc();

}
