/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.ciarif.flexibletransports.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;



public class MembershipUtils {
	
	private final static Logger log = Logger.getLogger(MembershipUtils.class);
	
	public MembershipUtils () {
		
	}

	public static final QuadTree<Person> createPersonQuadTree(Scenario scenario) {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

	    for (ActivityFacility f : scenario.getActivityFacilities().getFacilities().values()) {
	      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
	      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
	      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
	      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
	    QuadTree<Person> personQuadTree = new QuadTree<Person>(minx, miny, maxx, maxy);
	    for (Person p : scenario.getPopulation().getPersons().values()) {
	      Coord c = ((ActivityFacility)scenario.getActivityFacilities().getFacilities().get(((PlanImpl)p.getSelectedPlan()).getFirstActivity().getFacilityId())).getCoord();
	      personQuadTree.put(c.getX(), c.getY(), p);
	    }
	    log.info("PersonQuadTree has been created");
	    return personQuadTree; }

	public static void getPersonsQuadTree() {
		// TODO Auto-generated method stub
		
	}

}
