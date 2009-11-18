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

package org.matsim.api.core.v01.population;

import java.io.Serializable;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author dgrether
 */
public interface PopulationFactory extends Serializable, MatsimFactory {

	/**
	 * Creates a new Route object
	 * @param currentRouteLinkIds List of Ids including the start and the end Link Id of the route's links
	 * @return a BasicRoute Object with the links set accordingly
	 * @deprecated needs to be verified
	 */
@Deprecated
//	public BasicRoute createRoute(Id startLinkId, Id endLinkId, final List<Id> currentRouteLinkIds); // TODO [MR] check this
	// disabled until everything else is figured out
	
	
	
	Person createPerson(Id id);
	/**
	 * @deprecated use createPlan() instead. the reference to the Person
	 * is set when calling Person.addPlan(Plan p).
	 */
	@Deprecated
	Plan createPlan(Person person);

	Plan createPlan();
	
	Activity createActivityFromCoord(String actType, Coord coord);
	
//	Activity createActivityFromFacilityId(String actType, Id facilityId);
	// disabled until everything else is figured out
	
	Activity createActivityFromLinkId(String actType, Id linkId);
	
	Leg createLeg(TransportMode legMode);

	/**
	 * Creates a new Route object
	 * @param currentRouteLinkIds List of Ids including the start and the end Link Id of the route's links
	 * @return a BasicRoute Object with the links set accordingly
	 * @deprecated needs to be verified // TODO [MR] verify
	 */
//	Route createRoute(Id startLinkId, Id endLinkId, final List<Id> currentRouteLinkIds);
	// disabled until everything else is figured out

}
