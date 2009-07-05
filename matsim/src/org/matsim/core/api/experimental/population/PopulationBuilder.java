/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.api.experimental.population;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPopulationBuilder;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * @author dgrether
 */
public interface PopulationBuilder extends BasicPopulationBuilder {

	PersonImpl createPerson(Id id);

	PlanImpl createPlan(BasicPerson person);
	// yyyyyy needs to return "Plan"

	ActivityImpl createActivityFromCoord(String actType, Coord coord);
	// yyyyyy needs to return "Activity"
	
//	Activity createActivityFromFacilityId(String actType, Id facilityId);
	// disabled until everything else is figured out
	
	ActivityImpl createActivityFromLinkId(String actType, Id linkId);
	// yyyyyy needs to return "Activity"
	
	LegImpl createLeg(TransportMode legMode);
	// yyyyyy needs to return "Leg".

	/**
	 * Creates a new Route object
	 * @param currentRouteLinkIds List of Ids including the start and the end Link Id of the route's links
	 * @return a BasicRoute Object with the links set accordingly
	 * @deprecated needs to be verified // TODO [MR] verify
	 */
//	Route createRoute(Id startLinkId, Id endLinkId, final List<Id> currentRouteLinkIds);
	// disabled until everything else is figured out

}
