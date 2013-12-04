/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
package playground.southafrica.population.freight;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

/**
 * Class to take a population, and write each activity pair in the plan as a 
 * pair of coordinates so they can be visualized in R.
 * 
 * @author jwjoubert
 */
public class WriteLegCoordinatePairs {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(WriteLegCoordinatePairs.class.toString(), args);
		
		String populationFile = args[0];
		String outputFile = args[1];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(populationFile);
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("x1,y1,x2,y2");
			bw.newLine();
			
			for(Person person : sc.getPopulation().getPersons().values()){
				for(Plan plan : person.getPlans()){
					for(int i = 0; i < plan.getPlanElements().size()-1; i += 2){
						PlanElement from = plan.getPlanElements().get(i);
						PlanElement to = plan.getPlanElements().get(i+2);
						
						if(from instanceof Activity && to instanceof Activity){
							Activity fromActivity = (Activity) from;
							Activity toActivity = (Activity) to;
							bw.write(String.format("%.0f,%.0f,%.0f,%.0f\n", fromActivity.getCoord().getX(), fromActivity.getCoord().getY(),
																			 toActivity.getCoord().getX(), toActivity.getCoord().getY() ));
						} else{
							throw new RuntimeException("PlanElements should be of type Activity");
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close BufferedWriter for " + outputFile );
			}
		}
		
		
		Header.printFooter();
	}

}
