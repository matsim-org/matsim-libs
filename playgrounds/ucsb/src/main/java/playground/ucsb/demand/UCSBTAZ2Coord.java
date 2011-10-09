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

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.ucsb.UCSBUtils;

/**
 * @author balmermi
 *
 */
public class UCSBTAZ2Coord {

	public final void assignCoords(Scenario scenario, ObjectAttributes personObjectAttributes, Map<String,Feature> features) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			int actIndex = 0;
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Activity activity = (Activity)pe;
					String zoneId = (String)personObjectAttributes.getAttribute(person.getId().toString(),UCSBStopsParser.ZONE+actIndex);
					if (zoneId == null) { Gbl.errorMsg("pid="+person.getId()+": object attribute '"+UCSBStopsParser.ZONE+actIndex+"' not found."); }
					Feature zone = features.get(zoneId);
					if (zone == null) { Gbl.errorMsg("zone with id="+zoneId+" not found."); }
					Coord coord = UCSBUtils.getRandomCoordinate(zone);
					activity.getCoord().setXY(coord.getX(),coord.getY());
				}
			}
		}
	}
}
