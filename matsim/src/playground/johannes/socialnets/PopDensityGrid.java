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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class PopDensityGrid {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
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
		
		BufferedWriter popWriter = IOUtils.getBufferedWriter("/Users/fearonni/vsp-work/socialnets/data-analysis/pop.txt");
		
		double resolution = Double.parseDouble(args[1]);
		minX = minX - 200;
		maxX = maxX - 200;
		SpatialGrid<Double> grid = new SpatialGrid<Double>(minX, minY, maxX, maxY, resolution);
		
		BufferedWriter gridWriter = IOUtils.getBufferedWriter("/Users/fearonni/vsp-work/socialnets/data-analysis/grid.txt");
		for(int x = (int) minX; x < maxX; x+=resolution) {
			for(int y = (int) minY; y < maxY; y+=resolution) {
				gridWriter.write(String.valueOf(x));
				gridWriter.write("\t");
				gridWriter.write(String.valueOf(y));
				gridWriter.newLine();
			}
		}
		gridWriter.close();
		
		for(Person person : population) {
			Coord homeLoc = person.getSelectedPlan().getFirstActivity().getCoord();
			
			popWriter.write(String.valueOf(homeLoc.getX()));
			popWriter.write("\t");
			popWriter.write(String.valueOf(homeLoc.getY()));
			popWriter.newLine();
			
			Double count = grid.getValue(homeLoc);
			if(count == null)
				count = 0.0;
			count++;
			grid.setValue(count, homeLoc);
		}
		popWriter.close();
		grid.toFile(args[2], new DoubleStringSerializer());
		
	}

}
