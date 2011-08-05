/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionPrinter.java
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
package playground.benjamin.scenarios.munich.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * @author benjamin
 *
 */
public class EmissionWriter {
	private static final Logger logger = Logger.getLogger(EmissionWriter.class);

	void writeHomeLocation2Emissions(
			Population population,
			SortedSet<String> listOfPollutants,
			Map<Id, Map<String, Double>> emissions,
			String outFile) {
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("personId \t xHome \t yHome \t");
			for (String pollutant : listOfPollutants){
				out.append(pollutant + "[g] \t");
			}
			out.append("\n");

			for(Person person: population.getPersons().values()){
				Id personId = person.getId();
				Plan plan = person.getSelectedPlan();
				Activity homeAct = (Activity) plan.getPlanElements().get(0);
				Coord homeCoord = homeAct.getCoord();
				Double xHome = homeCoord.getX();
				Double yHome = homeCoord.getY();

				out.append(personId + "\t" + xHome + "\t" + yHome + "\t");

				Map<String, Double> emissionType2Value = emissions.get(personId);
				for(String pollutant : listOfPollutants){
					if(emissionType2Value.get(pollutant) != null){
						out.append(emissionType2Value.get(pollutant) + "\t");
					} else{
						out.append("0.0" + "\t"); // TODO: do I still need this?
					}
				}
				out.append("\n");
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to " + outFile);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}
