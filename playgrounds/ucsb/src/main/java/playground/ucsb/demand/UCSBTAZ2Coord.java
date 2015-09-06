/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBTAZ2Coord.java
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

package playground.ucsb.demand;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import playground.ucsb.UCSBUtils;

/**
 * @author balmermi
 *
 */
public class UCSBTAZ2Coord {

	public final void assignCoords(Scenario scenario, ObjectAttributes personObjectAttributes, Map<String, SimpleFeature> features) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			int actIndex = 0;
			Coord homeCoord = null;
			Coord workCoord = null;
			Coord educCoord = null;
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Activity activity = (Activity)pe;
					String zoneId = (String)personObjectAttributes.getAttribute(person.getId().toString(),UCSBStopsParser.ZONE+actIndex);
					if (zoneId == null) { throw new RuntimeException("pid="+person.getId()+": object attribute '"+UCSBStopsParser.ZONE+actIndex+"' not found."); }
					SimpleFeature zone = features.get(zoneId);
					if (zone == null) { throw new RuntimeException("zone with id="+zoneId+" not found."); }
					
					if (activity.getType().startsWith("home")) {
						if (homeCoord == null) { homeCoord = UCSBUtils.getRandomCoordinate(zone); }
						((ActivityImpl)activity).setCoord(homeCoord);
					}
					else if (activity.getType().startsWith("work")) {
						if (workCoord == null) { workCoord = UCSBUtils.getRandomCoordinate(zone); }
						((ActivityImpl)activity).setCoord(workCoord);
					}
					else if (activity.getType().startsWith("educ")) {
						if (educCoord == null) { educCoord = UCSBUtils.getRandomCoordinate(zone); }
						((ActivityImpl)activity).setCoord(educCoord);
					}
					else {
						Coord coord = UCSBUtils.getRandomCoordinate(zone);
						((ActivityImpl)activity).setCoord(coord);
					}
					actIndex++;
				}
			}
		}
	}
}
