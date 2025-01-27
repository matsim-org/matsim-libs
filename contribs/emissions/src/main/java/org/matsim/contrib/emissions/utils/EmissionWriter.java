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
package org.matsim.contrib.emissions.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author benjamin
 *
 */
public final class EmissionWriter {
	// is this useful as a publicly available class?  kai, jan'19

	private static final Logger logger = LogManager.getLogger(EmissionWriter.class);


	public EmissionWriter(){
	}

	public void writeHomeLocation2TotalEmissions(
			Population population,
			Map<Id<Person>, SortedMap<String, Double>> totalEmissions,
			Collection<String> pollutants,
			String outFile) {
		try{
			FileWriter fstream = new FileWriter(outFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("personId \t xHome \t yHome \t");
			for (String pollutant : pollutants){
				out.append(pollutant).append("[g] \t");
			}
			out.append("\n");

			for(Person person: population.getPersons().values()){
				Id<Person> personId = person.getId();
				Plan plan = person.getSelectedPlan();
				Activity homeAct = (Activity) plan.getPlanElements().get(0);
				Coord homeCoord = homeAct.getCoord();
				Double xHome = homeCoord.getX();
				Double yHome = homeCoord.getY();

				out.append(String.valueOf(personId)).append("\t").append(String.valueOf(xHome)).append("\t").append(String.valueOf(yHome)).append("\t");

				Map<String, Double> emissionType2Value = totalEmissions.get(personId);
				for(String pollutant : pollutants){
					if(emissionType2Value.get(pollutant) != null){
						out.append(String.valueOf(emissionType2Value.get(pollutant))).append("\t");
					} else{
						out.append("0.0" + "\t"); // TODO: do I still need this?
					}
				}
				out.append("\n");
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to {}", outFile);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}
