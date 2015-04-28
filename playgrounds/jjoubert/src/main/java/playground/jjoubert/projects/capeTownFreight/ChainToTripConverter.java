/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.Header;

/**
 * Class to read in a population, and convert the activity chains (plans) to a 
 * series of individual trips.
 * 
 * @author jwjoubert
 */
public class ChainToTripConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ChainToTripConverter.class.toString(), args);
		
		String population = args[0];
		String output = args[1];
		
		convertSelectedPlansToTrips(population, output);
		
		Header.printFooter();
	}
	
	
	private static void convertSelectedPlansToTrips(String input, String output){
		/* Read the population. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader pr = new MatsimPopulationReader(sc);
		pr.readFile(input);
		
		/* Set up the output file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		try{
			bw.write("id,trip,ox,oy,start,dx,dy,end");
			bw.newLine();
			
			/* Process each selected plan. */
			int id = 0;
			for(Person person : sc.getPopulation().getPersons().values()){
				Plan plan = person.getSelectedPlan();
				int trip = 1;
				for (int i = 0; i < plan.getPlanElements().size(); i+=2){
					Activity o = (Activity)plan.getPlanElements().get(i);
					Coord oc = ct.transform(o.getCoord());
					String tripStartTime = Time.writeTime(o.getEndTime());
					
					Activity d = (Activity)plan.getPlanElements().get(i+2);
					String tripEndTime = Time.writeTime(d.getStartTime());
					Coord dc = ct.transform(d.getCoord());
					
					bw.write(String.format("%d,%d,%.4f,%.4f,%s,%.4f,%.4f,%s\n", id, trip, oc.getX(), oc.getY(), tripStartTime, dc.getX(), dc.getY(), tripEndTime));
					trip++;
				}
				id++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
	}
	
}
