/* *********************************************************************** *
 * project: org.matsim.*
 * HomeFacilityGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.coopsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import playground.johannes.coopsim.utils.MatsimCoordUtils;

/**
 * @author illenberger
 *
 */
public class FacilityValidator {

	public static final String HOME_PREFIX = "home";
	
	public static void generate(ActivityFacilities facilities, NetworkImpl network, SocialGraph graph) {
		/*
		 * set link ids
		 */
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			Coord coord = facility.getCoord();
			Link link = NetworkUtils.getNearestLink(network, coord);
			((ActivityFacilityImpl) facility).setLinkId(link.getId());
		}
		/*
		 * create home facilities
		 */
		for(SocialVertex v : graph.getVertices()) {
			Person person = v.getPerson().getPerson();
			
			Id<ActivityFacility> id = Id.create(HOME_PREFIX + person.getId().toString(), ActivityFacility.class);
			ActivityFacilityImpl homeFac = ((ActivityFacilitiesImpl) facilities).createAndAddFacility(id, MatsimCoordUtils.pointToCoord(v.getPoint()));
			homeFac.createAndAddActivityOption("visit");
			Link link = NetworkUtils.getNearestLink(network, homeFac.getCoord());
			homeFac.setLinkId(link.getId());
		}
	}
	
}
