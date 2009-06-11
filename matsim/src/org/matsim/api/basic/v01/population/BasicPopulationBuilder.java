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

package org.matsim.api.basic.v01.population;

import java.io.Serializable;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;

/**
 * @author dgrether
 */
public interface BasicPopulationBuilder extends Serializable{

	public BasicPerson createPerson(Id id);

	public BasicPlan createPlan(BasicPerson person);

	public BasicActivity createActivityFromCoord(String actType, Coord coord);

	public BasicActivity createActivityFromFacilityId(String actType, Id facilityId);

	public BasicActivity createActivityFromLinkId(String actType, Id linkId);
	
	public BasicLeg createLeg(TransportMode legMode);

	/**
	 * Creates a new Route object
	 * @param currentRouteLinkIds List of Ids including the start and the end Link Id of the route's links
	 * @return a BasicRoute Object with the links set accordingly
	 * @deprecated needs to be verified
	 */
	public BasicRoute createRoute(Id startLinkId, Id endLinkId, final List<Id> currentRouteLinkIds); // TODO [MR] check this

}
