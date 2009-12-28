/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnetworks.graph.spatial;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * @author illenberger
 *
 */
public class GridUtils {

	public static SpatialGrid<Double> createDensityGrid(Population population, double resolution) {
		double maxX = 0;
		double maxY = 0;
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		for(Person person : population.getPersons().values()) {
			Coord homeLoc = ((Activity)person.getPlans().get(0).getPlanElements().get(0)).getCoord();
			maxX = Math.max(maxX, homeLoc.getX());
			maxY = Math.max(maxY, homeLoc.getY());
			minX = Math.min(minX, homeLoc.getX());
			minY = Math.min(minY, homeLoc.getY());
		}
		
		SpatialGrid<Double> grid = new SpatialGrid<Double>(minX, minY, maxX, maxY, resolution);
		for(Person person : population.getPersons().values()) {
			Coord homeLoc = ((Activity)person.getPlans().get(0).getPlanElements().get(0)).getCoord();
			Double count = grid.getValue(homeLoc);
			if(count == null)
				count = 0.0;
			count++;
			grid.setValue(count, homeLoc);
		}
		
		return grid;
	}
}
