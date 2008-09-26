/* *********************************************************************** *
 * project: org.matsim.*
 * PopDensityGrid.java
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

/**
 * 
 */
package playground.johannes.socialnets;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.geometry.Coord;

/**
 * @author illenberger
 *
 */
public class PopDensityGrid {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioData data = new ScenarioData(config);
		/*
		 * Make grid...
		 */
		Population population = data.getPopulation();
				
		double maxX = 0;
		double maxY = 0;
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		for(Person person : population) {
			Coord homeLoc = person.getSelectedPlan().getFirstActivity().getCoord();
			maxX = Math.max(maxX, homeLoc.getX());
			maxY = Math.max(maxY, homeLoc.getY());
			minX = Math.min(minX, homeLoc.getX());
			minY = Math.min(minY, homeLoc.getY());
		}
		
		double resolution = Double.parseDouble(args[1]);
		SpatialGrid<Double> grid = new SpatialGrid<Double>(minX, minY, maxX, maxY, resolution);
		for(Person person : population) {
			Coord homeLoc = person.getSelectedPlan().getFirstActivity().getCoord();
			Double count = grid.getValue(homeLoc);
			if(count == null)
				count = 0.0;
			count++;
			grid.setValue(count, homeLoc);
		}
		grid.toFile(args[2], new DoubleStringSerializer());
		
	}

}
