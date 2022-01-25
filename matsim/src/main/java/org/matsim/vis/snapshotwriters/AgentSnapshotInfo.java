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

package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public interface AgentSnapshotInfo {

	String marker = "marker";

	// !!! WARNING: The enum list can only be extended.  Making it shorter or changing the sequence of existing elements
	// will break the otfvis binary channel, meaning that *.mvi files generated until then will become weird. kai, jan'10
	enum AgentState { PERSON_AT_ACTIVITY, PERSON_DRIVING_CAR, PERSON_OTHER_MODE, TRANSIT_DRIVER, MARKER }
	// !!! WARNING: See comment above this enum.

	enum DrivingState { NOT_DRIVING, FREE_FLOW, CONGESTED }

	Id<Person> getId() ;

	Id<Vehicle> getVehicleId();

	Id<Link> getLinkId();

	DrivingState getDrivingState();

	double getEasting();

	double getNorthing();

	@Deprecated
	double getAzimuth();

	double getColorValueBetweenZeroAndOne();

	AgentState getAgentState();

	int getUserDefined() ;

}
