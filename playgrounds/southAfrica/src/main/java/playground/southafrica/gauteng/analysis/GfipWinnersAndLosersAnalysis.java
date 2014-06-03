/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.gauteng.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class GfipWinnersAndLosersAnalysis {

	/**
	 * Class to measure the gains (or losses) for every user. The intent is to 
	 * use two plans file, both which have the selected plan scored. The output 
	 * is written to CSV.
	 * 
	 * @param args Required arguments (in the following order):<br>
	 * <ol>
	 * 		<li> Base output plans file;
	 * 		<li> Comparing output plans file;
	 * 		<li> Output file;
	 * 		<li> Projected coordinate reference system (as used in plans file);
	 * 		<li> Unprojected coordinate reference system.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(GfipWinnersAndLosersAnalysis.class.toString(), args);
		String file1 = args[0];
		String file2 = args[1];
		String outputFilename = args[2];
		String crsProjected = args[3];
		
		/* Read the first (base) plan. */
		Scenario sc1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc1).parse(file1);

		/* Read the second plan. */
		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc2).parse(file2);
		
		/* Calculate the gains/losses for each person. */
//		Map<Id, Double> map = new TreeMap<Id, Double>();
//		for(Person person : sc1.getPopulation().getPersons().values()){
//			double score1 = ((PlanImpl)sc1.getPopulation().getPersons().get(person.getId()).getSelectedPlan()).getScore();
//			double score2 = ((PlanImpl)sc2.getPopulation().getPersons().get(person.getId()).getSelectedPlan()).getScore();
//			double change = score2 - score1;
//			map.put(person.getId(), change);
//		}
		
		/* Get the coordinate transformation. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crsProjected, "WGS84");
		
		/* Write the score changes to file, as well as the coordinate of the 
		 * 'home' location. 
		 * TODO Consider the hexagon IDs here. */
		
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFilename);
		try{
			bw.write("Id,Class,First,Second,Long,Lat,X,Y");
			bw.newLine();
			
			for(Id id : sc1.getPopulation().getPersons().keySet()){
				String sId = id.toString();
				String[] sa = sId.split("_");
				String classId = sa[0];
				
				Coord cp = ((Activity)sc1.getPopulation().getPersons().get(id).getSelectedPlan().getPlanElements().get(0)).getCoord();
				Coord cu = ct.transform(cp);
				
				double cLong = cu.getX();
				double cLat = cu.getY();
				double cX = cp.getX();
				double cY = cp.getY();
				
				bw.write(String.format("%s,%s,%.2f,%.2f,%.6f,%.6f,%.2f,%.2f\n", 
						id.toString(), 
						classId, 
						((PlanImpl)sc1.getPopulation().getPersons().get(id).getSelectedPlan()).getScore(), 
						((PlanImpl)sc2.getPopulation().getPersons().get(id).getSelectedPlan()).getScore(), 
						cLong, cLat, cX, cY));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFilename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFilename);
			}
		}
		
		
		Header.printFooter();
	}

}
