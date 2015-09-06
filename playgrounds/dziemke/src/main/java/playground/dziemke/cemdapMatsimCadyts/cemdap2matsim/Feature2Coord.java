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

package playground.dziemke.cemdapMatsimCadyts.cemdap2matsim;

import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 * based on balmermi
 *
 */
public class Feature2Coord {
	private final static Logger log = Logger.getLogger(Feature2Coord.class);
	
	public final static Random r = MatsimRandom.getRandom();
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	public final void assignCoords(Scenario scenario, int planNumber, ObjectAttributes personObjectAttributes, Map<String, SimpleFeature> features) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			int actIndex = 0;
			Coord homeCoord = null;
			Coord workCoord = null;
			Coord educCoord = null;
			// for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			for (PlanElement pe : person.getPlans().get(planNumber).getPlanElements()) {
				if (pe instanceof Activity) {
					Activity activity = (Activity)pe;
					String zoneId = (String)personObjectAttributes.getAttribute(person.getId().toString(),CemdapStopsParser.ZONE+actIndex);
					if (zoneId == null) {
						log.error("pid="+person.getId()+": object attribute '"+CemdapStopsParser.ZONE+actIndex+"' not found.");
						//Gbl.errorMsg("pid="+person.getId()+": object attribute '"+CemdapStopsParser.ZONE+actIndex+"' not found.");
					}
					SimpleFeature zone = features.get(zoneId);
					if (zone == null) {
						log.error("zone with id="+zoneId+" not found.");
						//Gbl.errorMsg("zone with id="+zoneId+" not found.");
					}
					
					if (activity.getType().startsWith("home")) {
						// if (homeCoord == null) { homeCoord = UCSBUtils.getRandomCoordinate(zone); }
						if (homeCoord == null) { homeCoord = getRandomCoordinate(zone); }
						((ActivityImpl)activity).setCoord(homeCoord);
					}
					else if (activity.getType().startsWith("work")) {
						// if (workCoord == null) { workCoord = UCSBUtils.getRandomCoordinate(zone); }
						if (workCoord == null) { workCoord = getRandomCoordinate(zone); }
						((ActivityImpl)activity).setCoord(workCoord);
					}
					else if (activity.getType().startsWith("educ")) {
						//if (educCoord == null) { educCoord = UCSBUtils.getRandomCoordinate(zone); }
						if (educCoord == null) { educCoord = getRandomCoordinate(zone); }
						((ActivityImpl)activity).setCoord(educCoord);
					}
					else {
						// Coord coord = UCSBUtils.getRandomCoordinate(zone);
						Coord coord = getRandomCoordinate(zone);
						((ActivityImpl)activity).setCoord(coord);
					}
					actIndex++;
				}
			}
		}
	}
	
	public static final Coord getRandomCoordinate(SimpleFeature feature) {
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();
		while (true) {
			Point point = getRandomCoordinate(envelope);
			if (point.within(geometry)) {
				return new Coord(point.getX(), point.getY());
			}
		}
	}
	
	public static final Point getRandomCoordinate(Envelope envelope) {
		double x = envelope.getMinX() + r.nextDouble() * envelope.getWidth();
		double y = envelope.getMinY() + r.nextDouble() * envelope.getHeight();
		return geometryFactory.createPoint(new Coordinate(x,y));
	}
}
