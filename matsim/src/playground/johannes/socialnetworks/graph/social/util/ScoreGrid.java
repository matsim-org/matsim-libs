/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreGrid.java
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
package playground.johannes.socialnetworks.graph.social.util;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGridKMLWriter;

/**
 * @author illenberger
 *
 */
public class ScoreGrid {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();
		ScenarioImpl scenario = loader.getScenario();
		PopulationImpl population = scenario.getPopulation();
//		CoordinateTransformation transform = new CH1903LV03toWGS84();
//		SocialNetwork<Person> socialnet = new SocialNetwork<Person>(population);

		SpatialGrid<Double> densityGrid2 = SpatialGrid.readFromFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.xml");
//		Coord min = new CoordImpl(densityGrid2.getXmin(), densityGrid2.getYmin());
//		Coord max = new CoordImpl(densityGrid2.getXmax(), densityGrid2.getYmax());
//		min = transform.transform(min);
//		max = transform.transform(max);

		SpatialGrid<Double> densityGrid = new SpatialGrid<Double>(densityGrid2.getXmin(), densityGrid2.getYmin(), densityGrid2.getXmax(), densityGrid2.getYmax(), densityGrid2.getResolution());
		SpatialGrid<Double> scoreSums = new SpatialGrid<Double>(densityGrid2.getXmin(), densityGrid2.getYmin(), densityGrid2.getXmax(), densityGrid2.getYmax(), densityGrid2.getResolution());
		
		for(Person person : population.getPersons().values()) {
			Coord homeLoc = ((Activity)person.getPlans().get(0).getPlanElements().get(0)).getCoord();
//			homeLoc = transform.transform(homeLoc);
			
			double sum = 0;
			for(Plan plan : person.getPlans()) {
				Double score = plan.getScore();
				if(score != null)
					sum += score; 	
			}
			double avr = sum/person.getPlans().size(); 
			
			Double val = scoreSums.getValue(homeLoc);
			if(val == null)
				val = 0.0;
			
			val += avr;
			
			scoreSums.setValue(val, homeLoc);
			
			Double count = densityGrid.getValue(homeLoc);
			if(count == null)
				count = 0.0;
			count++;
			densityGrid.setValue(count, homeLoc);
		}
		
		loader = null;
		scenario = null;
		population = null;
		config = null;
		System.gc();
		
		for(int row = 0; row < densityGrid.getNumRows(); row++) {
			for(int col = 0; col < densityGrid.getNumCols(row); col++) {
				Double sum = scoreSums.getValue(row, col);
				if(sum != null) {
					scoreSums.setValue(row, col, sum/densityGrid.getValue(row, col));
				}
			}
		}
		
		SpatialGridKMLWriter writer = new SpatialGridKMLWriter();
		writer.setCoordTransform(new CH1903LV03toWGS84());
		writer.write(scoreSums, "/Users/fearonni/vsp-work/runs-svn/run669/scoregrid.kmz");
	}

}
    