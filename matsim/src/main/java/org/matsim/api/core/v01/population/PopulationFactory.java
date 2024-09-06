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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.facilities.ActivityFacility;

/**
 * @author dgrether
 */
public interface PopulationFactory extends MatsimFactory {

	Person createPerson(Id<Person> id);

	Plan createPlan();

	/**
	 * Creates an Activity from a coordinate. Such activities are assigned to a link on running the Controler.
	 * @param actType - the type of the activity, which needs to correspond to some string in the config file
	 * @param coord - the coordinates of the activity
	 * @return the activity
	 * <p></p>
	 * It might in fact make sense to add a creational method that takes coord <i>and</i> link id.  kai, aug'10
	 */
	Activity createActivityFromCoord(String actType, Coord coord);
	
	Activity createInteractionActivityFromCoord(String actType, Coord coord);

	/**
	 * Creates an Activity from a link id. No coordinate will be associated directly with this activity. This does
	 * <i> not </i> add the activity into the plan.
	 * @param actType - the type of the activity, which needs to correspond to some string in the config file
	 * @return the activity
	 */
	Activity createActivityFromLinkId(String actType, Id<Link> linkId);
	
	Activity createInteractionActivityFromLinkId(String actType, Id<Link> linkId);

	Activity createActivityFromActivityFacilityId( String actType, Id<ActivityFacility> activityFacilityId ) ;
	
	Activity createInteractionActivityFromActivityFacilityId( String actType, Id<ActivityFacility> activityFacilityId ) ;

	Leg createLeg(String legMode);

	RouteFactories getRouteFactories();

}
