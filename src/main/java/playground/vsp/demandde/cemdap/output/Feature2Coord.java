/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package playground.vsp.demandde.cemdap.output;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.demandde.corineLandcover.CorineLandCoverData;
import playground.vsp.demandde.corineLandcover.LandCoverUtils;

/**
 * @author dziemke
 */
public class Feature2Coord {
	private final static Logger LOG = Logger.getLogger(Feature2Coord.class);
	
	public Feature2Coord() {
	}

	public final void assignCoords(Population population, int planNumber, ObjectAttributes personZoneAttributes, Map<String, SimpleFeature> zones,
			Map<Id<Person>, Coord> homeZones, boolean allowVariousWorkAndEducationLocations,  CorineLandCoverData corineLandCoverData ) {
		int counter = 0;
		LOG.info("Start assigning (non-home) coordinates. Plan number is " + planNumber +".");
		for (Person person : population.getPersons().values()) {
			counter++;
			if (counter % 1000000 == 0) {
				LOG.info(counter + " persons assigned with (non-home) coordinates so far.");
				Gbl.printMemoryUsage();
			}
			
			int activityIndex = 0;
			Coord workCoord = null;
			Coord educCoord = null;

			for (PlanElement planElement : person.getPlans().get(planNumber).getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					String zoneId = (String) personZoneAttributes.getAttribute(person.getId().toString(), CemdapStopsParser.ZONE + activityIndex);
					if (zoneId == null) {
						LOG.error("Person with ID " + person.getId() + ": Object attribute '" + CemdapStopsParser.ZONE + activityIndex + "' not found.");
					}
					SimpleFeature zone = zones.get(zoneId);
					if (zone == null) {
						throw new RuntimeException("Zone with id " + zoneId + " not found.");
					}
					if (allowVariousWorkAndEducationLocations) {
						if (activity.getType().equals(ActivityTypes.HOME)) {
							((Activity)activity).setCoord(homeZones.get(person.getId()));
						} else {
							Coord coord = getCoord(corineLandCoverData, zone, "other");
							((Activity)activity).setCoord(coord);
						}
					} else {
						if (activity.getType().equals(ActivityTypes.HOME)) {
							((Activity)activity).setCoord(homeZones.get(person.getId()));
						} else if (activity.getType().equals(ActivityTypes.WORK)) {
							if (workCoord == null) {
								workCoord = getCoord(corineLandCoverData, zone, "other");
							}
							((Activity)activity).setCoord(workCoord);
						} else if (activity.getType().equals(ActivityTypes.EDUCATION)) {
							if (educCoord == null) {
								educCoord = getCoord(corineLandCoverData, zone, "other");
							}
							((Activity)activity).setCoord(educCoord);
						} else {
							Coord coord = getCoord(corineLandCoverData, zone, "other");
							((Activity)activity).setCoord(coord);
						}
					}
					activityIndex++;
				}
			}
		}
		LOG.info("Finished assigning non-home coordinates.");
	}

	private Coord getCoord (CorineLandCoverData corineLandCoverData, SimpleFeature feature, String activityType) {
		Coord coord ;
		if (corineLandCoverData==null) {
			coord = Cemdap2MatsimUtils.getRandomCoordinate(feature);
		} else {
			coord = corineLandCoverData.getRandomCoord(feature,activityType);
		}
		return coord;
	}

	
	public final void assignHomeCoords(Population population, ObjectAttributes personZoneAttributes, Map<String, SimpleFeature> zones, Map<Id<Person>, Coord> homeZones, CorineLandCoverData corineLandCoverData) {
		int counter = 0;
		LOG.info("Start assigning home coordinates.");
		for (Person person : population.getPersons().values()) {
			counter++;
			if (counter % 1000000 == 0) {
				LOG.info(counter + " persons assigned with home coordinates so far.");
				Gbl.printMemoryUsage();
			}
			int activityIndex = 0;

			for (PlanElement planElement : person.getPlans().get(0).getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					String zoneId = (String) personZoneAttributes.getAttribute(person.getId().toString(), CemdapStopsParser.ZONE + activityIndex);
					Id<Person> personId = person.getId();
					if (zoneId == null) {
						LOG.error("Person with ID " + person.getId() + ": Object attribute '" + CemdapStopsParser.ZONE + activityIndex + "' not found.");
					}
					SimpleFeature zone = zones.get(zoneId);
					if (zone == null) {
						throw new RuntimeException("Zone with id " + zoneId + " not found.");
					}
					if (activity.getType().equals(ActivityTypes.HOME)) {
						Coord homeCoord = getCoord(corineLandCoverData, zone, "home");
						homeZones.put(personId, homeCoord);
					}
					activityIndex++;
				}
			}
		}
		LOG.info("Finished assigning home coordinates.");
	}
}