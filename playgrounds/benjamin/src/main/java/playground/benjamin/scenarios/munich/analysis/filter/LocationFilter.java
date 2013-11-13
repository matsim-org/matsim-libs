/* *********************************************************************** *
 * project: org.matsim.*
 * HomeLocationFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.filter;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author benjamin
 *
 */
public class LocationFilter {

	public boolean isPersonsHomeInShape(Person person, Collection<SimpleFeature> featuresInShape) {
		boolean isInShape = false;
		Coord homeCoord = getHomeActivityCoord(person);
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(homeCoord.getX(), homeCoord.getY()));
		for(SimpleFeature feature : featuresInShape){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				//logger.debug("found homeLocation of person " + person.getId() + " in feature " + feature.getID());
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
	
	public Coord getHomeActivityCoord(Person person){
		Coord homeActCoord = null;
		Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		homeActCoord = homeAct.getCoord();
		return homeActCoord;
	}
	
	// alternative (slower but more reliable?) way of finding a person's home coord
//	private Coord findHomeCoord(Person person) {
//		Plan plan = person.getSelectedPlan();
//		Coord homeCoord = null;
//		for (PlanElement pe : plan.getPlanElements()) {
//			if(pe instanceof Activity){
//				Activity act =  ((Activity) pe);
//				if(act.getType().equals("home")){
//					homeCoord = act.getCoord();
//					break;
//				} else if(act.getType().equals("pvHome")){
//					homeCoord = act.getCoord();
//					break;
//				} else if(act.getType().equals("gvHome")){
//					homeCoord = act.getCoord();
//					break;
//				}
//			}
//		}
//		if(homeCoord == null){
//			throw new RuntimeException("Person " + person.getId() + " has no homeCoord. Aborting...");
//		} else {
//			return homeCoord;
//		}
//	}
}
