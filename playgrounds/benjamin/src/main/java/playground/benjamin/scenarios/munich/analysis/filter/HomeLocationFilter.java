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

import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author benjamin
 *
 */
public class HomeLocationFilter {

	public boolean isPersonsHomeInShape(Person person, Set<Feature> featuresInShape) {
		boolean isInShape = false;
		Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		Coord homeCoord = homeAct.getCoord();
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(homeCoord.getX(), homeCoord.getY()));
		for(Feature feature : featuresInShape){
			if(feature.getDefaultGeometry().contains(geo)){
				//logger.debug("found homeLocation of person " + person.getId() + " in feature " + feature.getID());
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
}
